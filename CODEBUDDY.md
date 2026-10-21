# CODEBUDDY.md

This file provides guidance to CodeBuddy Code when working with code in this repository.

## Project Overview

**wxyd** (小泽助手) — a lightweight Spring Boot 2.2.5 operations/management platform. It is a **single-module** Maven project (`app`); the build produces one executable fat jar.

- **Language:** Java 8 (must be a full JDK, not JRE — the Java8-API module uses `javax.tools.JavaCompiler`)
- **Framework:** Spring Boot 2.2.5.RELEASE
- **ORM:** MyBatis 3.0.0 (manual `SqlSessionFactoryBean`, `mapUnderscoreToCamelCase`)
- **DB:** MySQL 8.0.30 + HikariCP
- **Security:** 国密 SM4 (CBC + PKCS7Padding + random IV) via BouncyCastle
- **HTTP client:** OkHttp 4.9.3
- **Frontend:** jQuery 1.8.3 + dark glassmorphism theme, served from `static/`

> Note: `CLAUDE.md` in this repo describes an older multi-module Spring Cloud Alibaba layout (`common`/`api`/`batch`/`tbpApply`/`dataDict`). That layout is **no longer present** — the parent `pom.xml` declares only the `app` module. Treat `README.md` (this doc's source of truth) and the actual `app/` module as authoritative; do not rely on `CLAUDE.md` for module structure or build commands.

## Build & Run Commands

```bash
# Build the single app module (skips tests)
mvn -pl app -am clean package -DskipTests

# Run locally in mock mode (external tbphx.do gateway returns mock data;
# user/robot/message/javaapi endpoints still run real logic)
java -Dwxyd.mock.enabled=true -jar app/target/app-1.0.0.jar

# Production mode (no mock)
java -Dwxyd.mock.enabled=false -jar app/target/app-1.0.0.jar
```

App runs on port **8090** with context-path **/api** → UI at `http://localhost:8090/api/login.html`.

### Database init (run once)

```bash
mysql -u root -p < wxyd.sql                                    # 应用初始化脚本（已合并 V2~V18 最终表结构，全新库执行一次即可）
mysql -u root -p < app/src/main/resources/sql/V2__user_robot.sql  # （旧库升级用；全新安装无需执行）
```

### Configuration

- Copy the template and fill in real values: `cp app/src/main/resources/env.properties.example app/src/main/resources/env.properties` (`env.properties` is gitignored, never commit it).
- Required keys: `datasource.url/username/password`, `wxyd.sm4.key` (32 hex chars; generate via `com.cyz.util.SMUtil.generateSM4Key()`), `wxyd.admin.username/password` (seeds a super-admin on first boot when `users` is empty).
- Optional GitHub OAuth login: fill `wxyd.github.client-id` / `wxyd.github.client-secret` / `wxyd.github.redirect-uri` (all three) to enable; redirect-uri must equal the GitHub OAuth App's Authorization callback URL. Leave empty to disable. New GitHub users are auto-provisioned (status=0 免审批) keyed by GitHub login; password login remains available. Schema: run `app/src/main/resources/sql/V7__github_login.sql`. **注意**：换 token 端点位于 `github.com`，服务端必须能出网访问 `github.com:443`（国内环境常被拦截）；若无法直连，可配置 `wxyd.github.proxy-host`/`proxy-port` 走能访问 GitHub 的代理。RestTemplate 已设 10s 连接 / 15s 读超时。
- Optional Gitee OAuth login（与 GitHub 同构）: fill `wxyd.gitee.client-id` / `wxyd.gitee.client-secret` / `wxyd.gitee.redirect-uri`（三项都填才启用；在 https://gitee.com/oauth/applications 创建应用，回调地址须与 redirect-uri 完全一致，权限勾选 user_info）。Endpoints `/oauth/gitee` + `/oauth/gitee/callback`，scope=`user_info`，按 `gitee_login` 自动建号（status=0 免审批）。Schema: `app/src/main/resources/sql/V10__gitee_login.sql`（users.gitee_login 唯一列，`DataInitializer` 启动自检补列）。gitee.com 国内可直连，不走 GitHub 代理。
- Optional email registration (`register.html` 三步流程：滑块验证 → 邮箱 6 位验证码（5 分钟有效、60s 重发间隔）→ 设置密码/可选用户名)：fill `wxyd.smtp.host/port/username/password/from` to enable mail sending（端口 465 走 SSL，其余走 STARTTLS；QQ 邮箱等需填授权码）。未配置时发码接口报「SMTP 未配置」。mock 模式（`wxyd.mock.enabled=true`）只打印验证码日志不真发。滑块/验证码/registerToken 均存内存（单实例），重启即失效。Schema: `app/src/main/resources/sql/V9__email_register.sql`（users.email 唯一列，`DataInitializer` 启动自检补列）。注册成功免审批（status=0）并自动登录；旧 `/register`（用户名密码，status=1 待审批）保留。
- Optional AI 工具模型网关（V19，wb_gateway_key/wb_gateway_config，DataInitializer 自检建表）: 让 AI 工具（Claude Code 等）用 WorkBuddy 模型与积分，零外部 API Key。双协议入口 POST /api/anthropic/v1/messages（流式+非流式）、POST /api/openai/v1/chat/completions、GET /api/anthropic/v1/models、GET /api/openai/v1/models、POST /api/anthropic/v1/messages/count_tokens；鉴权用「模型网关」页面创建的用户 Key（x-api-key 或 Bearer，最多 5 把/人，SM4 加密存储+SM3 哈希查询）。**Key 仅当天有效**：创建/点「启用」写入 `active_date`（V23），次日 00:00 定时任务 + 列表查询时自动置 `enabled=0`；过期 Key 调用返回 401，提示用户登录小泽助手 →「模型网关」重新启用。所有对外应答经 `WbGatewayChatService.sanitize` 脱敏，不出现上游厂商字样。上游链路：OkHttp 直连 https://copilot.tencent.com/v2/chat/completions（OpenAI 风格 SSE）；凭据为纯 Java 多账号池（移植 dsh-xdpool：解密桌面 App auth 快照 + at-rest 密钥经 interop 批文件问 WorkBuddy.exe，wxyd.wbgateway.workbuddy-exe 可配），429 按模型/402 账号级/401 会话死三级冷却自动换号，令牌临期自动经 /v2/plugin/auth/token/refresh 刷新并持久化到 data/wb-pool/accounts.json。管理员「模型开关」（gateway/admin/switch）：开=用户自选模型生效；关=静默映射 WorkBuddy 免费模型（billing.free，响应 model 字段回写用户视角名，映射不可见）。管理端点 /gateway/**（登录保护，WebMvcConfig），前端 api.html gateway 视图 + Assets/js/gateway.js。协议细节照 dsh-workbuddy-connect 0.7.x 逆向对齐，WorkBuddy 更新可能需要调整。
- Optional 金融日报推送: fill `wxyd.dayinfo.sm2-private-key` / `wxyd.dayinfo.sm4-key`（两项都填才启用；密钥用 `SMUtil.generateSM2KeyPair()` / `SMUtil.generateSM4Key()` 生成，公钥与 SM4 密钥交给推送方）。调用方 `POST /api/common/dayinfo.do`（免登录，靠验签+解密鉴权）：`{timestamp, data(SM2 密文 hex C1C3C2), sign=SM4-ECB(SM3(data+timestamp))}`；明文 `{"type":"finance","data":{"date","title","md","brief"}}`（brief=简洁版，可选，订阅推送优先展示，空则后端截正文前 100 字），时间戳 ±5 分钟防重放，按 (type, info_date) 每天一条、同日覆盖（V17 补 brief 列），首次入库后由 `notifyJob` 定时任务（每 10 分钟）在时间窗口（`wxyd.dayinfo.notify-window-start/end`，默认 8-22 点，V18 补 notified 列）内按订阅走钉钉/邮件提醒，深夜入库顺延次日窗口。查询与订阅端点 `/dayinfo/**`（登录可见，WebMvcConfig 保护清单），订阅为多订阅（V16：一人可配多个钉钉机器人/邮箱，`subscribe/test` 可立即试推最新一期），推送消息附免登录查看链接（公开只读端点 `/wdayinfo/detail` + 静态分享页 `dayinfo-share.html`，链接由 `wxyd.dayinfo.base-url` 生成并带访问令牌 `t=SM3(sm4Key:dayinfo:id)前32位` 防 id 枚举，照 /welfare/** 公开模式，`dayinfo-share.html` 不匹配 `/dayinfo/**` 拦截），菜单「金融日报」由 MENU_SEED 播种。桌面列表支持关键词/月份筛选（`dayinfo/list` 的 keyword/month 参数）。接口调用文档见 `docs/dayinfo-api.md`。联调示例：`docs/dayinfo-push-demo.js`。Schema: `app/src/main/resources/sql/V15__day_info.sql`、`V16__dayinfo_subscribe_multi.sql`（`DataInitializer` 启动自检建表/升级）。
- `WxydApplication` loads `env.properties` via `@PropertySource`.

### Tests

There are no test sources tracked in the current `app` module.

```bash
# If/when tests exist, run the module's tests:
mvn -pl app test
# Run a single test class / method:
mvn -pl app test -Dtest=SomeTest
mvn -pl app test -Dtest=SomeTest#someMethod
```

## Code Architecture

All source lives under `app/src/main/java/com/cyz/`.

| Package | Responsibility |
|---------|----------------|
| `WxydApplication` | Spring Boot entry point |
| `config/` | WebMvc, MyBatis (`mybatisMysqlConfig`, `DataSourceConfig`), `AuthInterceptor`, `DataInitializer`, `Sm4KeyHolder` (holds the SM4 key from env), `ProperConfig` |
| `controller/` | HTTP endpoints: `AuthController` (login/register/logout + `register/email/*` 邮箱注册), `CaptchaController` (`captcha/slider`、`captcha/verify` 滑块验证码), `UserController`, `MessageController` (留言板), `RobotController` (钉钉机器人), `ApiController` (`tbphx.do` legacy gateway), `CommController` (marker interface) |
| `service/` | Business logic for auth/user/message/robot + `SliderCaptchaService`（Java2D 滑块图、ticket）、`EmailRegisterService`（邮箱验证码/registerToken/完成注册）、`MailService`（SMTP 发信，`wxyd.smtp.*`） |
| `aspect/` | `ControllerAspect` — around-advice on all `CommController` impls, sets `traceId`/`clientIp` MDC, logs in/out/elapsed |
| `pojo/` | `Result` (uniform response), `User`, `DingRobot`, `Message`, `WhiteUser`, ... |
| `constant/` | `ErrorEnum` (error codes used by `Result`) |
| `util/` | `SMUtil` (SM4), and other helpers |
| `mock/` | `MockAspect` — returns mock data for `tbphx.do` when `wxyd.mock.enabled=true` |
| `filter/` | `RepeatReadFilter` (wraps multipart bodies for re-reading) |
| `javaapi/` | Standalone Java 8 API learning platform: 384 API docs, in-browser compile+run via `javax.tools.JavaCompiler` + JUnit 4 (10s timeout, security blacklist) |
| `archive/`, `dubbo/`, `shellscript/`, `dispatcher/` | Additional feature areas (archive browsing, dubbo-style RPC endpoints, shell-script execution, message dispatch). Explore these directly before editing. |
| `mapper/` + `resources/mapper/MysqlMapper.xml` | MyBatis mappers (MySQL only) |
| `appender/`, `logback/` | Custom logback appender/config |

### Key conventions (still valid from the older CLAUDE.md)

- **Uniform response:** every controller returns `com.cyz.pojo.Result` with `errorCode`/`errorMsg`/`traceId`/`clientIp`/`body`; error codes come from `ErrorEnum`.
- **Controller marker interface:** business controllers implement `CommController` so `ControllerAspect` intercepts them uniformly.
- **Auth:** HTTP Session + `AuthInterceptor` (path-based whitelist). Login/register are public; everything else returns 401 when unauthenticated.
- **SM4:** passwords and DingTalk `access_token`/`secret` are SM4-encrypted at rest (non-deterministic, random IV). Generate/verify keys via `SMUtil`.
- **Legacy gateway:** `ApiController.tbphx.do` decodes base64 + URLdecode, splits on `||` to extract a timestamp and enforces a ~3s replay window.

### Java8-API sandbox notes

- `CodeExecutionService` extracts JUnit/Hamcrest jars from `BOOT-INF/lib/` to temp files when running from the fat jar (nested-jar classpath workaround).
- Code executed through this feature runs under a blacklist (blocks File/network/reflection/process/SQL) with a 10s timeout.

## Frontend

Static pages under `app/src/main/resources/static/`: `login.html`, `api.html`, and `Assets/css/admin.css`, `Assets/js/index.js` (user/robot/message), `Assets/js/javaapi.js`. Dark glassmorphism theme; accent `#a78bfa`.
