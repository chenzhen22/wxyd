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
mysql -u root -p < wxyd.sql                                    # base tables (message, action, requestlog, doc, ...)
mysql -u root -p < app/src/main/resources/sql/V2__user_robot.sql  # users + ding_robot
```

### Configuration

- Copy the template and fill in real values: `cp app/src/main/resources/env.properties.example app/src/main/resources/env.properties` (`env.properties` is gitignored, never commit it).
- Required keys: `datasource.url/username/password`, `wxyd.sm4.key` (32 hex chars; generate via `com.cyz.util.SMUtil.generateSM4Key()`), `wxyd.admin.username/password` (seeds a super-admin on first boot when `users` is empty).
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
| `controller/` | HTTP endpoints: `AuthController` (login/register/logout), `UserController`, `MessageController` (留言板), `RobotController` (钉钉机器人), `ApiController` (`tbphx.do` legacy gateway), `CommController` (marker interface) |
| `service/` | Business logic for auth/user/message/robot |
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
