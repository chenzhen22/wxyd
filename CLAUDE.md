# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

**wxyd** (微银行系统) — 基于 Spring Cloud Alibaba 的银行/企业后台收单业务系统。

- **语言:** Java 8
- **框架:** Spring Boot 2.2.5.RELEASE, Spring Cloud Hoxton.SR3, Spring Cloud Alibaba 2.2.1.RELEASE
- **注册/配置中心:** Nacos
- **ORM:** MyBatis (多数据源)
- **数据库:** MySQL + Oracle + 多套机构分库

## 模块架构

| 模块 | 端口 | 说明 |
|------|------|------|
| `common` | — | 共享库: 工具类、POJO (`Result`, `Doc`, `Message`)、AOP 日志切面、常量枚举、SSH/SFTP 工具 |
| `api` | 8090 | API 网关: 外部 HTTP 入口(`/spbt/tbphx.do`)、文档上传查询、文件管理 |
| `batch` | 2103 | 批量处理: 定时任务(200ms高频轮询)、Socket消息处理、Feign 调用 tbpApply、SFTP 文件交换 |
| `tbpApply` | 8094 | 核心业务: 交易处理、用户登录(MM/NG)、UKey管理、白名单管理、Manifest 处理 |
| `dataDict` | — | 数据字典工具: 读取数据库元数据生成 Excel 数据字典 |
| `IFPREDIS/ifpSession` | — | 定制 Redis Session 管理组件 (com.ghbank.ifp) |

### 服务调用关系

```
外部系统 → api (8090) → [tbphx.do → base64/url解码] → MsgService → 数据库
                                      ↓ (文件消息)
batch (2103) → [定时轮询 → SFTP/本地文件] → Feign → tbpApply (8094)
                                                        ↓
                                          多机构 DB (first/second/three/four + sitcbs/sitats)
```

## 关键代码模式

### 统一响应格式
所有 Controller 返回 `com.chenzhen.pojo.Result`，包含 `errorCode`、`errorMsg`、`traceId`、`clientIp`、`body` 字段。错误码定义在 `ErrorEnum` 枚举中。

### Controller 约定
- 业务 Controller 实现标记接口 `CommController`（在 `common` 模块中），用于 AOP 切面统一拦截
- `ControllerAspect` 对所有 `CommController+` 实现类进行环绕通知：设置 traceId/clientIp MDC、记录入参出参耗时
- 请求体统一为 `Result` 对象（JSON），body 中携带业务参数

### 数据传输编码
`api` 模块收到的请求经 `BtoAAtoB.atob()` base64 解码 + URLDecoder 解编码 → 拆分 `||` 时间戳防重放验证(3秒) → `MsgService` 处理。

### ScheduledService 高频任务
`batch` 模块 3 个 200ms 定时任务（各持有独立锁）：
1. `refreshMessage` — 查询 `message` 表，通过 `SocketMessageAdapter` 策略模式处理
2. `handleApiMsgLocal` — 本地 `./msg/` 目录 `.send` 文件轮询，反射调用 `TbpApplyFeign` 方法
3. `handleApiMsgSftp` / `handleApiMsgSftp_Y` — SFTP 远程目录文件轮询

### 多数据源
`tbpApply` 配置多套数据源：`mysql`、`first`、`second`、`three`、`four`、`sitcbs`、`sitats`、`sit2cbs`、`sit2ats`。每套有独立 MyBatis Mapper 配置和 XML。

## Maven 构建命令

```bash
# 全量构建（跳过测试）
mvn clean install -DskipTests

# 仅编译 common（其他模块依赖它）
mvn clean install -pl common -DskipTests

# 构建单个模块
mvn clean package -pl batch -am -DskipTests

# 运行测试
mvn test -pl common

# 打可执行 jar
mvn clean package -pl batch -am -DskipTests
mvn clean package -pl api -am -DskipTests
mvn clean package -pl tbpApply -am -DskipTests

# 通过 Spring Boot Maven 插件运行
mvn spring-boot:run -pl api
mvn spring-boot:run -pl batch
mvn spring-boot:run -pl tbpApply
```

## 配置说明

- **Nacos**: `tbpApply` 和 `batch` 通过 Nacos 获取配置(properties 格式)
- **环境变量占位符**: `${datasource.url}`, `${cfca.socketServerIP}` 等由 `env.properties` + Nacos 注入
- **batch 模块**: `SystemConfig.setProperties()` 读取 `env.properties` 设置系统属性，并通过 Socket 广播给其他服务
- **日志**: 各模块使用 logback.xml，`common` 提供自定义 appender

## 本地开发

1. 需要 Nacos 服务（配置 `discovery.server-addr`）
2. 需要 MySQL 数据库，执行 `wxyd.sql` 初始化
3. 需要 Oracle 数据库（tbpApply 连接机构数据）
4. `env.properties` 中配置连接信息（不提交到 git）
5. api 模块无数据库依赖（通过 `MsgService` 操作数据）
