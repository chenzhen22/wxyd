# 用户管理重做 + 钉钉机器人 — 设计 Spec

**日期：** 2026-08-24
**状态：** 已批准（待 spec 复核）
**前置：** 基于已完成的「6 功能移除」（commit 7ab27c1）后的合并 `app` 模块。

## 目标

1. **用户管理重做**：废弃 IP 白名单（`client` 表 + `addWhite`/`updateWhite`/`deleteWhite`），改为用户名/密码注册 + 超级管理员审批。默认只播种一个超级管理员。密码用国密 SM4 加密，密钥由 `SMUtil.generateSM4Key()` 生成、放 `env.properties`。
2. **钉钉机器人**：新功能页。用户可增删改自己的机器人（ACCESS_TOKEN + SECRET，SECRET SM4 加密入库），每个用户只能操作自己的机器人。发送消息支持纯文本与文件；文件先 zip 压缩再 base64（`ZipUtils`），base64 串 >20KB 时拆成 ≤18KB 的块分多条文本消息发送。

## 已锁定的决策

| # | 决策点 | 选择 |
|---|--------|------|
| 1 | 登录/会话机制 | HTTP Session（Spring Boot 内嵌 Tomcat 内存会话，JSESSIONID cookie） |
| 2 | 与免密登录的关系 | **取代免密登录**：删 `LoginController` + `LoginServiceImpl` + `mmLogin`/`ngLogin`；`client` 表 → `users` 表 |
| 3 | SM4 密钥位置 | `env.properties`（不进 git，与 DB 密钥同处） |
| 4 | 文件转 base64 | **先 zip 压缩再 base64**（`java.util.zip` 压缩 → `ZipUtils.zipToBase64(byte[])` 做 base64） |
| 5 | 机器人 secret 入库 | SM4 加密入库（与密码同密钥），发送时解密 |
| 6 | 超管初始密码 | 从 `env.properties` 的 `wxyd.admin.password` 读取（运营部署前设置） |

## Mock 交互（关键架构点）

`MockAspect` 切面 `@Around("execution(* com.chenzhen.controller.CommController+.*(..))")`：
- `mock=true`（dev）时，非 `tbphx.do` 路径若无对应 mock 文件 → `joinPoint.proceed()`（MockAspect.java:88）直通真实 Controller；
- `tbphx.do` 仍按 action 走 mock。

**结论**：新增的真实端点（`AuthController`/`UserController`/`RobotController`）实现 `CommController` 即可——dev 模式下 MockAspect 找不到 mock 文件直通真实逻辑，同时 `ControllerAspect` 请求日志照常生效。**无需改 MockAspect**。**不为新端点创建 mock 文件**（它们是真实 DB 操作，不可 mock）。

## 数据模型

### `users` 表（替换 `client` 表）

```sql
CREATE TABLE `users` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `username` VARCHAR(64) NOT NULL,
  `password` VARCHAR(256) NOT NULL COMMENT 'SM4 密文 hex（IV(16)+密文）',
  `display_name` VARCHAR(64) DEFAULT NULL,
  `role` TINYINT NOT NULL DEFAULT 1 COMMENT '0=超级管理员,1=普通',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '0=已通过,1=待审批,2=已拒绝',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

### `ding_robot` 表

```sql
CREATE TABLE `ding_robot` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT NOT NULL COMMENT '归属用户',
  `name` VARCHAR(128) NOT NULL,
  `access_token` VARCHAR(256) NOT NULL,
  `secret` VARCHAR(512) NOT NULL COMMENT 'SM4 加密入库',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

DDL 脚本：`app/src/main/resources/sql/V2__user_robot.sql`（运营在 dev/prod MySQL 各执行一次；含 `DROP TABLE IF EXISTS client` 迁移说明，可选保留旧表备份）。

## SM4 密钥与配置

### `env.properties`（新增，不进 git）

```
wxyd.sm4.key=<SMUtil.generateSM4Key() 生成的 32 位 hex>
wxyd.admin.username=admin
wxyd.admin.password=<运营部署前设置的初始密码>
```

生成密钥：一次性跑 `SMUtil.generateSM4Key()`（已有 main 可调），把输出写入 env.properties。

### `Sm4KeyHolder`（新 `@Component`）

- `@Value("${wxyd.sm4.key}")` 注入密钥 hex；
- `encrypt(String plain)` → `SMUtil.sm4Encrypt(key, plain)`；
- `decrypt(String cipherHex)` → `SMUtil.sm4Decrypt(key, cipherHex)`；
- 供 `AuthServiceImpl`（密码）、`RobotServiceImpl`（secret）共用。

## 认证流程（HTTP Session）

### 端点（`AuthController`，实现 `CommController`）

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/register` | `{username, password, displayName?}` → SM4 加密 → 插入 `users(status=1 待审批)` → 提示等待审批 |
| POST | `/login` | `{username, password}` → SM4 加密输入对比 DB → status=0 通过 → 写 session(userId,username,role) → 返回用户信息；status=1 提示待审批；status=2 提示已拒绝 |
| POST | `/logout` | invalidate session |

### 会话拦截器 `AuthInterceptor`（`HandlerInterceptor`）

- `addPathPatterns("/**")`；
- `excludePathPatterns("/login","/register","/static/**","/","/actuator/**")`；
- 未登录访问受保护资源 → 302 重定向 `/static/login.html`（或返回 401 JSON 给 ajax）。

### 超管播种 `DataInitializer`（`ApplicationRunner`）

- 启动时 `SELECT count(*) FROM users`；为 0 → 读 `wxyd.admin.username`(默认 admin) + `wxyd.admin.password` → `Sm4KeyHolder.encrypt` → 插入 `role=0,status=0`；
- 日志 INFO 打印一次「已初始化超级管理员: <username>，请及时修改密码」。

### 用户管理页改造（`UserController`，实现 `CommController`）

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/user/list` | 超管看全部；普通用户只看自己。返回含 status |
| POST | `/approveUser` | `{id}` → status=0。仅超管（session.role==0） |
| POST | `/rejectUser` | `{id}` → status=2。仅超管 |
| POST | `/deleteUser` | `{id}` → 删除。仅超管 |

删除旧 `queryWhiteInfo`/`addWhite`/`updateWhite`/`deleteWhite`（IP 白名单逻辑全删）。

## 钉钉机器人

### Pojo `DingRobot`

`id, userId, name, accessToken, secret, createTime`（secret 在传输/展示时返回脱敏或不下发明文）。

### 端点（`RobotController`，实现 `CommController`）

所有操作从 session 取 `userId`，只能操作 `ding_robot.user_id == session.userId` 的记录。

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/robot/list` | 当前用户的机器人列表（secret 脱敏：只返回 `hasSecret:true` + 末 4 位） |
| POST | `/robot/add` | `{name, accessToken, secret}` → secret `Sm4KeyHolder.encrypt` 入库 |
| POST | `/robot/update` | `{id, name, accessToken, secret}` → 校验归属；secret 非空才更新（空=不改） |
| POST | `/robot/delete` | `{id}` → 校验归属后删 |
| POST | `/robot/send` | multipart：`robotId, type=text\|file, text?, file?` |

### 发送逻辑（`RobotServiceImpl.send`）

1. 按 `robotId` 查机器人 + 校验 `user_id` 归属 → `Sm4KeyHolder.decrypt(secret)` → 构造 `DingTalkRobotUtil.RobotConfig(accessToken, decryptedSecret)`。
2. **纯文本**（type=text）：`DingTalkRobotUtil.sendTextMsg(config, text, null, false)`，返回钉钉响应。
3. **文件**（type=file）：
   - 读 `MultipartFile` 字节；
   - `java.util.zip.ZipOutputStream` 压成单条目 zip（条目名=原文件名）→ 得 zip 字节；
   - `ZipUtils.zipToBase64(zipBytes)` → base64 串；
   - **拆分**：若 base64 串长度 > 20480（20KB），按 ≤18432（18KB）字符切块；
   - 每块发一条文本消息，消息体格式：`[FILE|<filename>|<chunkIdx>|<totalChunks>|<base64chunk>]`（chunkIdx 从 0 起）；
   - 收集每块钉钉响应，返回 `{totalChunks, results:[...]}`。

> ZipUtils 可新增便捷方法 `compressToBase64(Path)`（内部 zip+base64），集中压缩逻辑；或 `RobotServiceImpl` 内联实现。spec 不强求，plan 决定。

## 前端

### 新页面

- `app/src/main/resources/static/login.html`：用户名/密码表单 → POST `/login` → 成功跳 `api.html`，失败提示。
- `app/src/main/resources/static/register.html`：用户名/密码/显示名表单 → POST `/register` → 提示「等待管理员审批」。

### `index.html`（重定向桩）

改为跳 `/static/login.html`（未登录入口）。

### `api.html` + `index.js` 改造

- 新增菜单项「钉钉机器人」+ 模块区：
  - 机器人列表表格（名称、token 末 4 位、操作：改/删）；
  - 新增/编辑表单（name、accessToken、secret）；
  - 发消息区：类型切换（文本/文件）→ 文本框 or 文件选择 → 发送按钮 → 结果展示（含分块数、每块响应）。
- 用户管理区改造：
  - 超管：待审批列表（用户名、显示名、注册时间、[通过]/[拒绝] 按钮）+ 已通过用户列表（[删除]）；
  - 普通用户：只看自己的信息。
- `index.js`：新增 robot CRUD/send 处理；用户管理改为 approve/reject/deleteUser；所有 ajax 带 cookie（同源默认带）。

## 免密登录基础设施清理（grep 门控）

取代免密登录后，以下变为孤儿，**逐个 grep 门控确认无其他引用后删除**（沿用 6 功能移除的 grep-gate 模式）：

- `controller/LoginController.java` + `service/LoginService.java` + `service/LoginServiceImpl.java`；
- `service/HttpRequestCPR09003.java`（grep 确认仅 LoginServiceImpl 用 → 删）；
- `pojo/SocketMessage.java`（grep 确认仅 HttpRequestCPR09003 + LoginServiceImpl 用 → 删）；
- `pojo/ESBPojo.java` / `OSBData.java` / `ReqSvcHeader.java` / `SvcBody.java` / `MsgBean.java` / `UUS1007T.java` / `UUS1009T.java` / `listen/OSBDataListener.java`（grep 确认仅 LoginServiceImpl/OSBDataListener 互引 → 删）；
- `MysqlMapper` + XML 的 `addSocketMessage`/`querySocketRevice`/`updateSocketMsgStatus`（grep 确认仅 LoginServiceImpl 用 → 删）；
- `common.properties` 的免密登录相关键（如 `mailIp`、登录相关）——保留 `localIp`（控制台 appender 用）、`passworkKey`（dec 用）等共用键。

## 文件结构图

### 新建
- `pojo/User.java`、`pojo/DingRobot.java`
- `controller/AuthController.java`、`controller/RobotController.java`
- `service/AuthService.java`+`AuthServiceImpl.java`、`service/RobotService.java`+`RobotServiceImpl.java`
- `config/Sm4KeyHolder.java`、`config/AuthInterceptor.java`（+ WebMvc 注册）、`config/DataInitializer.java`
- `static/login.html`、`static/register.html`
- `resources/sql/V2__user_robot.sql`

### 修改
- `controller/UserController.java`、`service/UserService.java`+`UserSericeImpl.java`（改 users 表）
- `mapper/MysqlMapper.java`+`MysqlMapper.xml`（加 users/robot 语句；grep 门控删 socket 语句）
- `static/api.html`、`static/Assets/js/index.js`（新菜单 + robot + 用户管理改造）
- `static/index.html`（跳 login.html）
- `app/src/main/resources/env.properties`（本地加 3 键，**不进 git**）

### 删除（grep 门控）
- `LoginController`、`LoginService`+`Impl`、`HttpRequestCPR09003`、`SocketMessage`、ESBPojo 家族、`OSBDataListener`

## 验证方式（无单元测试）

沿用「6 功能移除」的验证模型：**compile → boot → smoke**。

1. `mvn -pl app -am clean compile`（JDK17 编译 Java8 源）→ BUILD SUCCESS；
2. `mvn -pl app -am clean package -DskipTests` → 产出 `app-1.0.0.jar`；
3. 运营在 dev MySQL 执行 `V2__user_robot.sql`；
4. env.properties 配好 sm4.key / admin.username / admin.password；
5. `java -jar app/target/app-1.0.0.jar`（Java8 运行，mock=true 或 false 均可，新端点直通真实 DB）→ `Started WxydApplication` + 日志「已初始化超级管理员」；
6. smoke：
   - `POST /register` 注册一个普通用户 → DB status=1；
   - 用 admin 登录 → `POST /login` → 返回用户信息 + Set-Cookie；
   - 带 cookie `POST /approveUser` 通过该用户 → status=0；
   - 该用户登录 → 成功；
   - `POST /robot/add` 加机器人 → DB secret 为密文；
   - `POST /robot/send` type=text 发文本 → 钉钉响应 errmsg=ok；
   - `POST /robot/send` type=file 发 >20KB 文件 → 返回 totalChunks>1、每块 errmsg=ok；
   - 未登录访问 `/robot/list` → 401/重定向；
   - 用户 A 不能操作用户 B 的机器人（update/delete 返回权限错误）。

## 范围外（YAGNI）

- 密码修改/找回（用户未要求）；
- 机器人 @人 / @所有人（默认 null/false）；
- 文件接收/重组（仅做发送，接收端属另一系统）；
- 机器人 secret 展示明文（list 只返回脱敏）；
- 多超管提升 UI（role 仅播种时 =0，UI 不做提升）。

## 自检

- 无 TBD/占位符；
- 决策表与各节一致（SM4 env.properties / 先压缩 / secret 加密 / 超管密码 env）；
- Mock 直通点已确认（MockAspect:88）；
- 免密登录清理为 grep 门控，不假设引用；
- 范围聚焦单一 plan（用户管理 + 机器人），清理为同一 plan 的一个阶段。
