# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

**wxyd (小泽助手)** — 基于 Spring Boot 2.7.18 的轻量运维管理平台，前后端一体（桌面版 + H5），**单 jar 部署**。

- **语言:** Java 8（必须 JDK 而非 JRE — Java8-API 模块需要 `javax.tools.JavaCompiler`）
- **框架:** Spring Boot 2.7.18 + MyBatis (mybatis-spring-boot-starter 2.3.2, `mapUnderscoreToCamelCase`)
- **数据库:** MySQL 8 + HikariCP（单一数据源）
- **安全:** 国密 SM4（CBC + PKCS7Padding + 随机 IV）加密密码与 SECRET（BouncyCastle）；HTTP Session 鉴权
- **前端:** jQuery 3.7.1，静态页面在 `app/src/main/resources/static/`（index.html 桌面版 / h5.html 移动端）

> 历史提示：仓库早期是多模块 Spring Cloud 微服务（api/batch/tbpApply 等），现已收敛为单一 `app` 模块。旧的外部网关入口 `tbphx.do` 仍保留（`ApiController` → `ApiService`，`wxyd.mock.enabled=true` 时返回 mock 数据）。

## 构建与运行

```bash
# 构建（根 pom 的唯一模块是 app）
mvn -pl app -am clean package -DskipTests

# 本地运行（mock 模式下 tbphx.do 返回 mock 数据）
java -Dwxyd.mock.enabled=true -jar app/target/app-1.0.0.jar

# 入口: http://localhost:8090/api/login.html  （context-path=/api, 端口 ${PORT:8090}）
```

- 无测试目录（`app/src/test` 不存在），`-DskipTests` 实为习惯性防御。
- 配置：`app/src/main/resources/application.yml` + `env.properties`（经 `@PropertySource` 注入；**已 gitignore，含真实 DB 密钥/SM4 密钥/超管密码，绝不提交**）。模板见 `env.properties.example`。
- 数据库初始化：`wxyd.sql`（基础表）+ 按序号执行 `app/src/main/resources/sql/V*.sql` 迁移脚本。
- Docker：`app/Dockerfile`（eclipse-temurin:8-jre，`-Xmx384m` 适配小内存部署）。
- Lombok 版本在 `app/pom.xml` 显式覆盖为 1.18.30（Boot 2.2.5 管理的 1.18.12 无法在 JDK 16+ 构建机上运行）。

## 架构

唯一模块 `app`，入口 `com.cyz.WxydApplication`，根包 `com.cyz`：

| 包 | 说明 |
|------|------|
| `controller` | REST 控制器。业务控制器实现标记接口 `CommController`，由 `aspect/ControllerAspect` 环绕：设置 traceId/clientIp MDC、记录入参出参耗时 |
| `service` | 业务实现（Auth/User/Message/Group/Robot/Menu/SqlQuery/WbGatewayKey 等） |
| `mapper` + `resources/mapper/*.xml` | MyBatis Mapper（MysqlMapper、WbGatewayMapper、NoteIndexMapper、RadarMapper 等） |
| `config` | `AuthInterceptor`（Session 鉴权，检查 `session.userId`）、`InterceptorConfig`、`DataSourceConfig`、`DataInitializer`（首次启动播种超管）、`Sm4KeyHolder` |
| `filter` | `RepeatReadFilter`（multipart 免包装）、`DoSuffixFilter` |
| `wbgateway` | **AI 模型网关**（近期开发重点）：`/api/anthropic/**` 与 `/api/openai/**` 双协议入口。上游为 WorkBuddy 桌面 App 登录态（纯 Java 解密 auth 文件）与 Qoder；`pool/`（账号池、at-rest 密钥、积分自动化签到）、`provider/`（ProviderRegistry 适配器 + CompositeCatalog 同名模型合并 + FreePriority 免费模型优先）、`qoder/`（Qoder 客户端/每日签到 Job） |
| `pdf` | 证据材料整理：多格式合并为单一 A4 竖向 PDF（PDFBox 2.0.24） |
| `javaapi` | Java 8 API 学习平台（`javax.tools.JavaCompiler` 在线编译运行，10 秒超时 + 安全黑名单） |
| `note` | 用户级 Markdown 记忆笔记（md 存 `/apps/shareFile/{用户id}/`，frontmatter visibility 分公共/私有） |
| `radar` / `patrol` / `batch` | 雷达巡检、巡检日报、批次任务（各含 controller/model/service 子包；调度共用 pool-size=2 的调度器） |
| `push` | 钉钉机器人推送（文件→zip→base64，>20KB 分块 ≤18KB） |
| `dayinfo` 相关 (`controller/DayInfoController` 等) | 金融日报推送（SM2 加签 + SM4，见 `docs/dayinfo-*.md`） |
| `sqlquery` (`SqlQueryController/Service`) | 仅超管的在线 SQL 工具，仅允许 SELECT/SHOW/DESC/EXPLAIN/WITH 单语句 |

## 关键约定

- **统一响应**: Controller 返回 `com.cyz.pojo.Result`；错误码在 `ErrorEnum`。
- **鉴权**: HTTP Session（`AuthInterceptor` 检查 `userId` attribute）；管理员/超管分级；注册审批制；支持 GitHub/Gitee OAuth 与邮箱验证码注册（均需 `env.properties` 配置，留空即禁用）。
- **菜单显隐**: `menu_config` 四态配置（全部可见/隐藏/白名单/黑名单）控制桌面菜单与 H5 卡片/tab；管理员始终可见全部。
- **JSON**: fastjson2 + fastjson1-compat 混用（代码同时使用两套 API）。
- **群聊消息**: 服务器只做中转（拉取即删，`group_msg_transit`，7 天兜底清理），记录存浏览器 localStorage。
