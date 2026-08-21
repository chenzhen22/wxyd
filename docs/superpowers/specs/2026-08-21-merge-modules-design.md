# Design: Merge api / batch / tbpApply into one Spring Boot app

**Date:** 2026-08-21
**Status:** Approved
**Branch:** V2.0

## Goal

Merge the three Spring Boot services (`api`, `batch`, `tbpApply`) plus the shared `common` library into a **single Spring Boot application**. Remove Nacos (discovery + config), Feign, and all Oracle database support. Keep **only MySQL** as the sole database. The merged app runs mock-first: `MockAspect` serves mock JSON for every `tbphx.do` action, so the former `api → (.send file) → batch → (Feign HTTP) → tbpApply` chain is bypassed.

## Decisions (confirmed with user)

1. **Database:** Remove Oracle (all 8 institution datasources), keep only MySQL. This matches the `api` module's existing mock subsystem, which already has mock JSON for every `tbphx.do` action.
2. **Call mechanism:** Direct in-process method calls; delete the `.send/.rev` file queue and the batch polling tasks. No Feign.
3. **Oracle-dependent methods:** Enable mock mode to cover ALL `tbphx.do` actions. Oracle-exclusive service paths are deleted; MySQL-backed services are kept but dormant under mock.
4. **Merge strategy:** Lean merge (Approach A). One bootable module; delete Nacos/Feign/file-queue/Oracle; keep the live mock path + a single MySQL datasource.

## Current architecture (for reference)

- `api` (8090, context `/api`) — no DB. `ApiController.tbphx.do` → `ApiServiceImpl` → `MsgService.getResult` writes `./msg/{uuid}.send`, polls for `{uuid}.rev`. Has `MockAspect` (Order 1, around `CommController+`) that short-circuits every action to `classpath:mock/tbphx.do/action=*.json` when `wxyd.mock.enabled=true`.
- `batch` (2103, context `/batch`) — 1 MySQL datasource (`BatchMapper`: message/socketMessage/requestlog). `@EnableScheduling` + `@EnableFeignClients` + `@EnableDiscoveryClient`. Scheduled tasks: `refreshMessage` (MySQL `message` table → `SocketMessageAdapter`), `handleApiMsgLocal` (polls `./msg/*.send`, reflects over `TbpApplyFeign`, writes `.rev`), `handleApiMsgSftp(_Y)` (SFTP variant), `delete`/`shutdown` crons. `TbpApplyFeign` interface declares 30 methods against `tbpApply`.
- `tbpApply` (8094, context `/tbpApply`) — 9 datasources: 1 MySQL (`MysqlMapper`) + 8 Oracle (`first/second/three/four/sitcbs/sitats/sit2cbs/sit2ats`) wired by `DataSourceConfig` + 8 `mybatisDbNConfig` classes + `AtsMapperFactory`/`CbsMapperFactory`. `@EnableDiscoveryClient`. `TbpApplication.main` opens a `ServerSocket:26210` (`SystemConfig`) to receive runtime properties before boot.
- `common` — shared lib. Carries `spring-cloud-starter-bootstrap` + `spring-cloud-starter-alibaba-nacos-config` + `mybatis-spring-boot-starter` + `mysql-connector-java`. Holds `Result`/`Doc`/`Message` POJOs, `CommController` marker, `ControllerAspect`, SSH/SFTP utils.

## Target architecture

### 1. Module structure
- Collapse the parent-pom multi-module layout into **one bootable module** (`jar` packaging, single `pom.xml`). Parent `wxyd` becomes a plain Spring Boot app project (or the `api` module is repurposed as the merged app — either is acceptable; the plan will pick one and migrate sources).
- All Java sources from `common`, `api`, `batch`, `tbpApply` consolidate under one `src/main/java/com/chenzhen` tree. Resources consolidate into one `src/main/resources` (merging `mapper/`, `mock/`, `static/`, `logback.xml`).
- One main class: `com.chenzhen.WxydApplication` with `@SpringBootApplication` only (no `@EnableDiscoveryClient`, no `@EnableFeignClients`). `@EnableScheduling` removed (no surviving crons).
- Port **8090**, context-path `/api` (unchanged external entry).
- `dataDict` (standalone data-dictionary tool) and `IFPREDIS/ifpSession` (custom Redis session component) are **deleted** as part of the merge — already removed on branch V3.0. Because `api` referenced `ifpSession`, the api session-config classes are also removed (see deletion checklist); no Redis-session replacement (mock-first app needs none).

### 2. Live request path (mock-first)
- `tbphx.do` → `MockAspect` (Order 1, around `CommController+.*`). With `wxyd.mock.enabled=true` (default), it base64/URL-decodes `transData`, splits the `||` timestamp (3s replay check), parses `action`, and returns `classpath:mock/tbphx.do/action={action}.json` **before** `joinPoint.proceed()`.
- `ApiServiceImpl.tbphx` / `MsgService.getResult` are reached only when mock is **off**; in that case they dispatch in-process (§4), never over files or HTTP.
- `DocController`/`IndexController` are likewise intercepted by `MockAspect`; mock files exist for `doc/list`, `doc/upload`, etc.

### 3. MySQL datasource (single, sole DB)
- One HikariCP MySQL datasource (driver `com.mysql.cj.jdbc.Driver`, url/user/password from `application.yml`/`env.properties`).
- One `SqlSessionFactory` + one `@MapperScan` (or the equivalent of `mybatisMysqlConfig`) covering surviving mappers: `MysqlMapper` (tbpApply) and `BatchMapper` (batch) — the latter **only if** a retained service still consumes it; otherwise `BatchMapper` + `BatchMapper.xml` are deleted with the batch scheduled tasks.
- `wxyd.sql` remains the schema-init script (MySQL-only DDL: note/operinfo/message/client/action/pwmanagement/socketMessage/requestlog/doc/docFile/documentFile).

### 4. Replacing Feign + the file queue
- **Delete** `TbpApplyFeign`, the `spring-cloud-starter-openfeign` dependency, and `spring-cloud-starter-loadbalancer`.
- **Delete** the `.send/.rev` file I/O inside `MsgService.getResult` and batch's `handleApiMsgLocal` / `handleApiMsgSftp` / `handleApiMsgSftp_Y` scheduled tasks.
- **Introduce** an `ActionDispatcher` bean (replaces reflection-over-`TbpApplyFeign`). It maps `action` string → tbpApply service method via direct `@Autowired` calls (no HTTP, no reflection, no files). `MsgService.getResult` becomes a thin delegate to `ActionDispatcher`. This path is dead while mock is on, but provides the real in-process path when mock is off for MySQL-backed actions.
- tbpApply controllers have no Feign caller in the merged app; they are kept only if they compile without Oracle. Oracle-exclusive services (`queryUdOper`, `orderCreate`, `queryUdInfo`, `cfcaInfoQry`, `queryCprUser`, `requestData`/`responseData`/`start` as applicable) are **deleted**. MySQL-backed services (message/doc/white/user/ukey/manifest) are **kept, dormant under mock**.

### 5. Config (no Nacos)
- Single `application.yml` (no `bootstrap.yml`, no Nacos config block). Holds: server port/context-path, the MySQL datasource, `wxyd.mock.enabled: true`, CFCA placeholders, mybatis mapper-locations.
- Secrets (DB credentials) via `env.properties` or direct values — the plan will choose; either way no Nacos.
- **Remove** `SystemConfig` socket-injector (tbpApply `ServerSocket:26210` property injection) and batch's `SystemConfig.setProperties` socket-broadcast. Runtime properties now resolve from `application.yml`/`env.properties` through Spring's normal mechanism.
- Remove `@EnableDiscoveryClient` / `@EnableFeignClients` from the main class.

### 6. batch scheduled tasks
- `refreshMessage` (socket-message table dispatch via `SocketMessageAdapter` + `SocketMessageFactory` + adapters `MMLoginAdapter`/`NGLoginAdapter`/`StartAdapter`/`UnBindAdapter`/`CfcaInfoQryAdapter`) — these adapters call `TbpApplyFeign`; with Feign gone and mock covering all actions, **delete** the whole socket-message dispatch chain.
- `handleApiMsgLocal` / `handleApiMsgSftp(_Y)` — file-queue tasks, **deleted**.
- `delete` / `shutdown` crons — **deleted** for leanness.
- If `BatchMapper` retains no consumer, delete it + `BatchMapper.xml`; else retain. Disposition finalized in the plan.

## Deletion checklist (comprehensive)

### Maven
- Parent `pom.xml`: drop `spring-cloud-dependencies` + `spring-cloud-alibaba-dependencies` from `dependencyManagement`.
- `common/pom.xml`: drop `spring-cloud-starter-bootstrap`, `spring-cloud-starter-alibaba-nacos-config`.
- `batch/pom.xml`: drop `spring-cloud-starter-alibaba-nacos-discovery`, `nacos-client`, `spring-cloud-starter-loadbalancer`, `spring-cloud-starter-openfeign`.
- `tbpApply/pom.xml`: drop `ojdbc6`, `spring-cloud-starter-alibaba-nacos-discovery`, `nacos-client`, `spring-cloud-starter-loadbalancer`.
- Keep: `mysql-connector-java`, `mybatis-spring-boot-starter`, `spring-boot-starter-web/aop/data-jdbc/actuator`, fastjson, easyexcel, poi, jsch, hutool, dom4j, etc.

### Java / config
- Main class: drop `@EnableDiscoveryClient`, `@EnableFeignClients`, `@EnableScheduling`; drop `SystemConfig.setProperties()` call from `main()`.
- Delete: `TbpApplyFeign`, batch `ScheduledService`, batch `SocketMessageAdapter`/`SocketMessageFactory` + adapters, batch `MsgConfig`, batch `ScheduleConfig`, batch `SystemConfig` (socket variant), tbpApply `SystemConfig` (socket injector).
- tbpApply `DataSourceConfig`: delete 8 Oracle `HikariDataSource` beans; keep `mysqlDataSource`.
- Delete `mybatisDb1Config`..`mybatisDb8Config`; keep `mybatisMysqlConfig`.
- Delete mapper packages `firstMapper`/`secondMapper`/`threeMapper`/`fourMapper`/`sitcbsMapper`/`sitatsMapper`/`sit2cbsMapper`/`sit2atsMapper` + their interfaces + XML; keep `mysqlMapper`/`MysqlMapper` + XML.
- Delete `AtsMapper`, `CbsMapper` (base interfaces), `AtsMapperFactory`, `CbsMapperFactory`.
- Delete tbpApply services/controllers that exclusively depend on the deleted Oracle mappers (finalized per-service in the plan).
- `MsgService.getResult`: replace file-queue body with a delegate to the new `ActionDispatcher`; remove `./msg/` file I/O.
- `logback.xml`: remove the `com.alibaba.nacos.client` logger entry.
- **api ifpSession cleanup (IFPREDIS deleted):** delete `api/src/main/java/com/chenzhen/annotation/EnableIfpRedissonHttpSession.java`, `config/IfpRedissonSessionConfiguration.java`, `config/RedissonSessionConfiguration.java`, and the `config/InterceptorConfig.java` ifp-related wiring; remove the `ifp.session.*` block from `application.yml`; remove any `@EnableIfpRedissonHttpSession` usage on the main class; remove redisson/`ifp` dependencies that came via IFPREDIS. No Redis-session replacement.

### Resources
- Delete `batch/src/main/resources/bootstrap.yml`, `env.properties` (Nacos keys) — superseded by the merged `application.yml`.
- Delete `tbpApply/src/main/resources/bootstrap.yml` (Nacos + 8 Oracle datasource blocks + `feign` block + 8 `mybatis.*Mapper` entries).
- Merge surviving `mapper/*.xml` into one `src/main/resources/mapper/` tree.

## Out of scope
- SFTP file-exchange for institution data — deleted with the batch SFTP tasks.
- CFCA real-time socket client — dormant under mock; config placeholders kept, client code disposition finalized in the plan.

## In scope (added after V3.0 review)
- `dataDict` module — **deleted** (already removed on V3.0).
- `IFPREDIS/ifpSession` module — **deleted** (already removed on V3.0); api's ifpSession/Redisson session references cleaned up with it, no replacement.

## Open items for the implementation plan
- Per-service audit of tbpApply's 6 services + 6 controllers to decide keep/stub/delete based on whether each compiles and runs without the Oracle mappers.
- Decide whether `BatchMapper` survives (does any retained service write the `message`/`socketMessage`/`requestlog` tables?).
- Choose final merged-module name and whether to repurpose `api/` or create a new module dir.
- Confirm `env.properties` vs inline values for DB credentials.
