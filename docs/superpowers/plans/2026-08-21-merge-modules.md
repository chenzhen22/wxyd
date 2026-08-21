# Merge api/batch/tbpApply into one app — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Consolidate the `common`, `api`, `batch`, `tbpApply` Maven modules into a single bootable Spring Boot app that runs mock-first, with Nacos, Feign, the file-queue, and all Oracle support removed; MySQL is the sole database.

**Architecture:** One `app/` Maven module (jar), one `@SpringBootApplication` (`WxydApplication`), one HikariCP MySQL datasource + one `SqlSessionFactory` scanning `com.chenzhen.mapper`. `MockAspect` (mock on by default) serves every `tbphx.do` action from `classpath:mock/`. When mock is off, `MsgService.getResult` dispatches in-process via a new `ActionDispatcher` bean (no files, no Feign).

**Tech Stack:** Java 8, Spring Boot 2.2.5.RELEASE, MyBatis 3.0.0, MySQL 8.0.30, HikariCP, fastjson2 2.0.52 + fastjson-compat 2.0.48, POI/easyexcel, JSch, Hutool.

## Global Constraints

- **Branch:** `V3.0` (already created; `IFPREDIS/` and `dataDict/` already deleted there — leave deleted).
- **Java 8** source/target (`-parameters` on). Package is always `com.chenzhen` — no package renames needed when moving sources.
- **No spring-cloud / nacos / openfeign / loadbalancer / ojdbc** dependencies in the final `pom.xml`.
- **Sole DB:** MySQL via one datasource, prefix `spring.datasource`, mapper-locations `classpath:mapper/*.xml`.
- **Verification model:** this codebase has no unit-test harness for these modules (existing `src/test` files are ad-hoc scripts). The test cycle is therefore **compile → package → boot → smoke-test the mock endpoint**, not unit TDD. Each task ends with `mvn -q -pl app clean compile` passing unless noted.
- **Commit after every task.** End commit messages with `Co-Authored-By: Claude <noreply@anthropic.com>`.
- Run Maven from repo root `D:\GIT\wxyd`. Paths below are repo-relative.

## File Structure (target)

- `app/pom.xml` — single bootable module, all deps.
- `app/src/main/java/com/chenzhen/WxydApplication.java` — main class.
- `app/src/main/java/com/chenzhen/**` — all surviving sources from common/api/batch/tbpApply.
- `app/src/main/resources/application.yml` — sole config (no bootstrap.yml, no Nacos).
- `app/src/main/resources/mapper/*.xml` — `BatchMapper.xml` + `MysqlMapper.xml` (Oracle XML deleted).
- `app/src/main/resources/mock/**`, `static/**`, `logback.xml` — merged from api/batch/tbpApply.
- New: `app/src/main/java/com/chenzhen/dispatcher/ActionDispatcher.java` — in-process action→service dispatch.
- Deleted: `common/`, `api/`, `batch/`, `tbpApply/` module dirs after sources are moved (their `pom.xml` and old main classes go).

---

### Task 1: Scaffold the merged `app` module and move `common` sources in

**Files:**
- Create: `app/pom.xml`
- Create: `app/src/main/java/com/chenzhen/WxydApplication.java`
- Create: `app/src/main/resources/application.yml`
- Modify: `pom.xml` (root) — `<modules>` becomes `app` only; drop spring-cloud `dependencyManagement`.
- Move: `common/src/main/java/com/chenzhen/**` → `app/src/main/java/com/chenzhen/**`
- Move: `common/src/main/resources/**` → `app/src/main/resources/**`

**Interfaces:**
- Produces: a compiling `app` module containing all `common` classes; root pom lists only `app`.

- [ ] **Step 1: Create `app/pom.xml`**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <parent>
        <artifactId>wxyd</artifactId>
        <groupId>com.chenzhen</groupId>
        <version>1.0.0</version>
    </parent>
    <modelVersion>4.0.0</modelVersion>

    <artifactId>app</artifactId>
    <packaging>jar</packaging>

    <dependencies>
        <!-- Spring Boot -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-aop</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-jdbc</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-actuator</artifactId>
        </dependency>

        <!-- MyBatis + MySQL -->
        <dependency>
            <groupId>org.mybatis.spring.boot</groupId>
            <artifactId>mybatis-spring-boot-starter</artifactId>
            <version>3.0.0</version>
        </dependency>
        <dependency>
            <groupId>mysql</groupId>
            <artifactId>mysql-connector-java</artifactId>
            <version>8.0.30</version>
        </dependency>

        <!-- JSON: fastjson2 + fastjson1-compat (code uses both APIs) -->
        <dependency>
            <groupId>com.alibaba.fastjson2</groupId>
            <artifactId>fastjson2</artifactId>
            <version>2.0.52</version>
        </dependency>
        <dependency>
            <groupId>com.alibaba</groupId>
            <artifactId>fastjson</artifactId>
            <version>2.0.48</version>
        </dependency>

        <!-- Excel / XML / utils -->
        <dependency>
            <groupId>com.alibaba</groupId>
            <artifactId>easyexcel</artifactId>
            <version>3.3.4</version>
        </dependency>
        <dependency>
            <groupId>com.google.code.gson</groupId>
            <artifactId>gson</artifactId>
        </dependency>
        <dependency>
            <groupId>org.dom4j</groupId>
            <artifactId>dom4j</artifactId>
            <version>2.1.4</version>
        </dependency>
        <dependency>
            <groupId>org.jsoup</groupId>
            <artifactId>jsoup</artifactId>
            <version>1.17.2</version>
        </dependency>
        <dependency>
            <groupId>com.jcraft</groupId>
            <artifactId>jsch</artifactId>
            <version>0.1.55</version>
        </dependency>
        <dependency>
            <groupId>cn.hutool</groupId>
            <artifactId>hutool-all</artifactId>
            <version>5.8.34</version>
        </dependency>
        <dependency>
            <groupId>net.sf.json-lib</groupId>
            <artifactId>json-lib</artifactId>
            <version>2.4</version>
            <classifier>jdk15</classifier>
        </dependency>
        <dependency>
            <groupId>xom</groupId>
            <artifactId>xom</artifactId>
            <version>1.2.10</version>
        </dependency>
        <dependency>
            <groupId>javax.mail</groupId>
            <artifactId>mail</artifactId>
            <version>1.6.1</version>
        </dependency>
        <dependency>
            <groupId>com.itextpdf</groupId>
            <artifactId>html2pdf</artifactId>
            <version>4.0.3</version>
        </dependency>
        <dependency>
            <groupId>org.apache.xmlgraphics</groupId>
            <artifactId>batik-transcoder</artifactId>
            <version>1.19</version>
        </dependency>
        <dependency>
            <groupId>org.apache.xmlgraphics</groupId>
            <artifactId>batik-swing</artifactId>
            <version>1.19</version>
        </dependency>
        <dependency>
            <groupId>org.apache.xmlgraphics</groupId>
            <artifactId>batik-codec</artifactId>
            <version>1.19</version>
        </dependency>

        <!-- CFCA toolkit (referenced by HttpRequestCPR09003; dormant under mock) -->
        <dependency>
            <groupId>cfca</groupId>
            <artifactId>CommonVO</artifactId>
            <version>0.0.1</version>
        </dependency>
        <dependency>
            <groupId>cfca</groupId>
            <artifactId>TPCFEPToolkit</artifactId>
            <version>0.0.1</version>
        </dependency>

        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
        </dependency>

        <!-- Test -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-test</artifactId>
            <scope>test</scope>
            <exclusions>
                <exclusion>
                    <groupId>com.vaadin.external.google</groupId>
                    <artifactId>android-json</artifactId>
                </exclusion>
            </exclusions>
        </dependency>
        <dependency>
            <groupId>org.springframework</groupId>
            <artifactId>spring-test</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>junit</groupId>
            <artifactId>junit</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>com.h2database</groupId>
            <artifactId>h2</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>
        </plugins>
        <resources>
            <resource>
                <directory>src/main/java</directory>
                <includes>
                    <include>**/*.xml</include>
                    <include>**/*.properties</include>
                </includes>
            </resource>
            <resource>
                <directory>src/main/resources</directory>
                <includes>
                    <include>**/*</include>
                </includes>
            </resource>
        </resources>
    </build>
</project>
```

- [ ] **Step 2: Create `app/src/main/java/com/chenzhen/WxydApplication.java`**

```java
package com.chenzhen;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class WxydApplication {
    public static void main(String[] args) {
        SpringApplication.run(WxydApplication.class, args);
        System.out.println("wxyd启动成功");
    }
}
```

- [ ] **Step 3: Create `app/src/main/resources/application.yml`** (minimal placeholder; finalized in Task 7)

```yaml
server:
  port: 8090
  servlet:
    context-path: /api

spring:
  application:
    name: wxyd
  datasource:
    type: com.zaxxer.hikari.HikariDataSource
    driver-class-name: com.mysql.cj.jdbc.Driver
    jdbc-url: ${datasource.url:jdbc:mysql://localhost:3306/wxyd?useSSL=false&characterEncoding=utf8&serverTimezone=Asia/Shanghai}
    username: ${datasource.username:root}
    password: ${datasource.password:root}
    hikari:
      connection-timeout: 30000
      minimum-idle: 5
      maximum-pool-size: 50
      auto-commit: true
      idle-timeout: 600000
      pool-name: WxydHikariCP
      max-lifetime: 1800000
  servlet:
    multipart:
      max-file-size: 2000MB
      max-request-size: 2000MB
      enabled: true

mybatis:
  mapperLocations: classpath:mapper/*.xml

wxyd:
  mock:
    enabled: true
```

- [ ] **Step 4: Update root `pom.xml`**

Replace the `<modules>` block so it lists only `app`, and delete the entire `<dependencyManagement>` block (spring-cloud + spring-cloud-alibaba imports). Final root pom `<modules>`:

```xml
    <modules>
        <module>app</module>
    </modules>
```

Keep the spring-boot-starter-parent parent, the `<build>` plugin/resources blocks, and the `1.8` compiler settings unchanged.

- [ ] **Step 5: Move `common` sources into `app`**

```bash
mkdir -p app/src/main/java app/src/main/resources
git mv common/src/main/java/com app/src/main/java/
git mv common/src/main/resources/* app/src/main/resources/
```

(`common/src/main/resources` currently has no files; if `git mv common/src/main/resources/*` errors "no match", skip it.)

- [ ] **Step 6: Delete the `common` module dir**

```bash
rm -rf common
```

- [ ] **Step 7: Verify compile**

```bash
mvn -q -pl app -am clean compile -DskipTests
```
Expected: BUILD SUCCESS. (If `WxydApplication` fails to find beans, that's fine — it compiles standalone.)

- [ ] **Step 8: Commit**

```bash
git add -A
git commit -m "refactor: scaffold merged app module, move common sources in

Co-Authored-By: Claude <noreply@anthropic.com>"
```

---

### Task 2: Move `api` sources in; remove ifpSession references

**Files:**
- Move: `api/src/main/java/com/chenzhen/**` → `app/src/main/java/com/chenzhen/**`
- Move: `api/src/main/resources/**` → `app/src/main/resources/**`
- Delete: `api/src/main/java/com/chenzhen/ApiApplication.java`
- Delete: `api/src/main/java/com/chenzhen/annotation/EnableIfpRedissonHttpSession.java`
- Delete: `api/src/main/java/com/chenzhen/config/IfpRedissonSessionConfiguration.java`
- Delete: `api/src/main/java/com/chenzhen/config/RedissonSessionConfiguration.java`
- Modify: `app/src/main/resources/application.yml` — remove any `ifp.session.*` block (none added yet; ensure not introduced when merging api's yml in Task 7).
- Inspect: `api/src/main/java/com/chenzhen/config/InterceptorConfig.java` for ifp wiring; remove ifp references, keep the rest.

**Interfaces:**
- Produces: api controllers (`ApiController`, `DocController`, `IndexController`), services (`ApiService`/`ApiServiceImpl`, `DocService`, `MsgService`), and the mock layer (`MockAspect`, `MockDataService`) living in `app`, with no ifpSession references.

- [ ] **Step 1: Move api Java sources**

```bash
git mv api/src/main/java/com/chenzhen/* app/src/main/java/com/chenzhen/
```
If `git mv` complains a target exists (collision), handle per the collision list in Task 4; for api there are no FQCN collisions with common, so this should succeed.

- [ ] **Step 2: Move api resources**

```bash
git mv api/src/main/resources/mock app/src/main/resources/
git mv api/src/main/resources/static app/src/main/resources/
git mv api/src/main/resources/logback.xml app/src/main/resources/logback.xml
# application.yml from api is superseded by app's; do not move it
rm -f api/src/main/resources/application.yml
```
If any target already exists, skip (common had none of these).

- [ ] **Step 3: Delete the api main class and ifp session classes**

```bash
git rm app/src/main/java/com/chenzhen/ApiApplication.java
git rm app/src/main/java/com/chenzhen/annotation/EnableIfpRedissonHttpSession.java
git rm app/src/main/java/com/chenzhen/config/IfpRedissonSessionConfiguration.java
git rm app/src/main/java/com/chenzhen/config/RedissonSessionConfiguration.java
# remove the now-empty annotation dir if git leaves it
rmdir app/src/main/java/com/chenzhen/annotation 2>/dev/null || true
```

- [ ] **Step 4: Inspect & clean `InterceptorConfig.java`**

Open `app/src/main/java/com/chenzhen/config/InterceptorConfig.java`. Remove any import of `com.ghbank.ifp.*`, `EnableIfpRedissonHttpSession`, or Redisson session beans. Keep non-ifp interceptors. If the file only existed for ifp session wiring, delete it instead:

```bash
git rm app/src/main/java/com/chenzhen/config/InterceptorConfig.java   # only if entirely ifp-related
```

- [ ] **Step 5: Verify compile**

```bash
mvn -q -pl app -am clean compile -DskipTests
```
Expected: BUILD SUCCESS with no `com.ghbank.ifp` or `EnableIfpRedissonHttpSession` symbol errors. If errors remain about missing ifp symbols, grep and remove them: `grep -rn "com.ghbank.ifp\|EnableIfpRedissonHttpSession\|RedissonSession" app/src` and delete the offending lines.

- [ ] **Step 6: Commit**

```bash
git add -A
git commit -m "refactor: move api sources into app, drop ifpSession

Co-Authored-By: Claude <noreply@anthropic.com>"
```

---

### Task 3: Move `batch` sources in; delete batch-only Feign/scheduling/socket code

**Files:**
- Move: `batch/src/main/java/com/chenzhen/{adapter,factory,mapper,pojo,feign,service}/**` → `app/src/main/java/com/chenzhen/**` (selectively — see steps)
- Move: `batch/src/main/resources/mapper/BatchMapper.xml` → `app/src/main/resources/mapper/BatchMapper.xml`
- Delete (do NOT move): `batch/src/main/java/com/chenzhen/BatchApplication.java`
- Delete: `batch/src/main/java/com/chenzhen/service/ScheduledService.java`
- Delete: `batch/src/main/java/com/chenzhen/adapter/**` (SocketMessageAdapter, MMLoginAdapter, NGLoginAdapter, StartAdapter, UnBindAdapter, CfcaInfoQryAdapter)
- Delete: `batch/src/main/java/com/chenzhen/factory/SocketMessageFactory.java`
- Delete: `batch/src/main/java/com/chenzhen/feign/TbpApplyFeign.java` (and the `feign` package)
- Delete: `batch/src/main/java/com/chenzhen/config/SystemConfig.java` (socket broadcaster)
- Delete: `batch/src/main/java/com/chenzhen/config/MsgConfig.java`
- Delete: `batch/src/main/java/com/chenzhen/config/ScheduleConfig.java`
- Keep & move (collides with tbpApply in Task 4 — keep batch's): `batch/src/main/java/com/chenzhen/config/DataSourceConfig.java`, `batch/src/main/java/com/chenzhen/config/mybatisMysqlConfig.java`, `batch/src/main/java/com/chenzhen/config/CFCAConfig.java`
- Keep & move: `batch/src/main/java/com/chenzhen/mapper/BatchMapper.java`
- Keep & move: `batch/src/main/java/com/chenzhen/pojo/Message.java` (note FQCN `com.chenzhen.pojo.Message` — check collision with tbpApply pojos; tbpApply mappers use `com.chenzhen.pojo.Messages`/`Message`? see Step 5)

**Interfaces:**
- Produces: `BatchMapper` + `BatchMapper.xml` in `app`; `CFCAConfig`; batch's `DataSourceConfig` (prefix `spring.datasource`) and `mybatisMysqlConfig` (scans `com.chenzhen.mapper`) become the canonical merged datasource config.

- [ ] **Step 1: Move surviving batch sources**

```bash
git mv batch/src/main/java/com/chenzhen/mapper/BatchMapper.java app/src/main/java/com/chenzhen/mapper/BatchMapper.java
git mv batch/src/main/java/com/chenzhen/pojo/Message.java app/src/main/java/com/chenzhen/pojo/Message.java
git mv batch/src/main/java/com/chenzhen/config/DataSourceConfig.java app/src/main/java/com/chenzhen/config/DataSourceConfig.java
git mv batch/src/main/java/com/chenzhen/config/mybatisMysqlConfig.java app/src/main/java/com/chenzhen/config/mybatisMysqlConfig.java
git mv batch/src/main/java/com/chenzhen/config/CFCAConfig.java app/src/main/java/com/chenzhen/config/CFCAConfig.java
git mv batch/src/main/resources/mapper/BatchMapper.xml app/src/main/resources/mapper/BatchMapper.xml
```

- [ ] **Step 2: Delete the dead batch classes** (do not move them)

```bash
git rm batch/src/main/java/com/chenzhen/BatchApplication.java
git rm batch/src/main/java/com/chenzhen/service/ScheduledService.java
git rm batch/src/main/java/com/chenzhen/adapter/SocketMessageAdapter.java
git rm batch/src/main/java/com/chenzhen/adapter/CfcaInfoQryAdapter.java
git rm batch/src/main/java/com/chenzhen/adapter/MMLoginAdapter.java
git rm batch/src/main/java/com/chenzhen/adapter/NGLoginAdapter.java
git rm batch/src/main/java/com/chenzhen/adapter/StartAdapter.java
git rm batch/src/main/java/com/chenzhen/adapter/UnBindAdapter.java
git rm batch/src/main/java/com/chenzhen/factory/SocketMessageFactory.java
git rm batch/src/main/java/com/chenzhen/feign/TbpApplyFeign.java
git rm batch/src/main/java/com/chenzhen/config/SystemConfig.java
git rm batch/src/main/java/com/chenzhen/config/MsgConfig.java
git rm batch/src/main/java/com/chenzhen/config/ScheduleConfig.java
```

- [ ] **Step 3: Delete the entire `batch` module dir**

```bash
rm -rf batch
```

- [ ] **Step 4: Confirm `mybatisMysqlConfig` scans the merged mapper package**

Open `app/src/main/java/com/chenzhen/config/mybatisMysqlConfig.java`. It must read `${mybatis.mapperLocations}` (already does), `@MapperScan(basePackages = {"com.chenzhen.mapper"}, ...)` (already does), and inject `@Qualifier("mysqlDataSource")`. No change needed — this is the canonical config. Verify by reading; do not edit.

- [ ] **Step 5: Resolve `Message` pojo collision check**

`MysqlMapper` references `com.chenzhen.pojo.Messages` (note plural) and `SocketMessage`, `OperInfo`, `LogPojo`, `WhiteUser`, `Doc`, `DocumentFile`. batch's `Message.java` is a different pojo (the socket-message-row). Confirm both are needed: `grep -rn "com.chenzhen.pojo.Message\b" app/src` — if batch's `Message` is only used by the deleted `ScheduledService`/`BatchMapper`, and `BatchMapper.queryMessageList` returns it, keep both `Message` and `Messages`. They have distinct names, so no collision. Do nothing.

- [ ] **Step 6: Verify compile**

```bash
mvn -q -pl app -am clean compile -DskipTests
```
Expected: BUILD SUCCESS. There will be unresolved references to `TbpApplyFeign`/`SocketMessageFactory`/`MsgConfig` from any surviving code — but those callers (`ScheduledService`) were deleted, so none should remain. If compile errors reference `com.chenzhen.feign.TbpApplyFeign` from `MsgService` (api), defer to Task 5 (ActionDispatcher) — comment out the offending line temporarily with a `// TODO Task 5` marker ONLY if it blocks compile, then fix in Task 5.

- [ ] **Step 7: Commit**

```bash
git add -A
git commit -m "refactor: move batch MySQL mapper/config into app, drop feign+scheduler+socket

Co-Authored-By: Claude <noreply@anthropic.com>"
```

---

### Task 4: Move `tbpApply` sources in; delete Oracle datasources/mappers/factories

**Files:**
- Move (surviving tbpApply): `controller/**`, `service/**` (except Oracle-excised in Task 6), `pojo/**`, `factory` NO (delete), `mapper/mysqlMapper/**` + `mapper/AtsMapper.java`/`CbsMapper.java` NO (delete), `config/{InterceptorConfig,MyWebConfig,ClientInfo...}.java` (resolve collisions).
- Delete (do NOT move):
  - `tbpApply/src/main/java/com/chenzhen/TbpApplication.java`
  - `tbpApply/src/main/java/com/chenzhen/config/SystemConfig.java` (socket receiver)
  - `tbpApply/src/main/java/com/chenzhen/config/DataSourceConfig.java` (duplicate FQCN — batch's kept)
  - `tbpApply/src/main/java/com/chenzhen/config/mybatisDb1Config.java` .. `mybatisDb8Config.java`
  - `tbpApply/src/main/java/com/chenzhen/mapper/AtsMapper.java`, `CbsMapper.java`
  - `tbpApply/src/main/java/com/chenzhen/mapper/firstMapper/**`, `secondMapper/**`, `threeMapper/**`, `fourMapper/**`, `sitcbsMapper/**`, `sitatsMapper/**`, `sit2cbsMapper/**`, `sit2atsMapper/**`
  - `tbpApply/src/main/java/com/chenzhen/factory/AtsMapperFactory.java`, `CbsMapperFactory.java`
  - `tbpApply/src/main/resources/mapper/firstMapper/**` .. `sit2atsMapper/**` (8 XML dirs)
  - `tbpApply/src/main/resources/bootstrap.yml`
- Move: `tbpApply/src/main/resources/mapper/mysqlMapper/MysqlMapper.xml` → `app/src/main/resources/mapper/MysqlMapper.xml` (flatten into one mapper dir)
- Delete: `tbpApply` module dir after moves.

**Interfaces:**
- Produces: tbpApply controllers + services (Oracle references excised in Task 6) + `MysqlMapper`/`MysqlMapper.xml` in `app`, all under `com.chenzhen.mapper.mysqlMapper` (scanned by `com.chenzhen.mapper`).

- [ ] **Step 1: Move surviving tbpApply Java sources (controllers, services, pojos)**

```bash
git mv tbpApply/src/main/java/com/chenzhen/controller app/src/main/java/com/chenzhen/
git mv tbpApply/src/main/java/com/chenzhen/service app/src/main/java/com/chenzhen/
git mv tbpApply/src/main/java/com/chenzhen/pojo app/src/main/java/com/chenzhen/
```
If `pojo` or `service` already exists in `app` (from common/batch), move file-by-file instead: `git mv tbpApply/src/main/java/com/chenzhen/pojo/*.java app/src/main/java/com/chenzhen/pojo/` and resolve any same-name collisions manually (expected: none — common pojos are `Result/Doc/Message/...`, tbpApply adds `WhiteUser/Messages/OperInfo/...`).

- [ ] **Step 2: Move the MySQL mapper + XML**

```bash
git mv tbpApply/src/main/java/com/chenzhen/mapper/mysqlMapper app/src/main/java/com/chenzhen/mapper/mysqlMapper
mkdir -p app/src/main/resources/mapper
git mv tbpApply/src/main/resources/mapper/mysqlMapper/MysqlMapper.xml app/src/main/resources/mapper/MysqlMapper.xml
```

- [ ] **Step 3: Move surviving tbpApply config classes (collision-aware)**

tbpApply's `config/` has: `DataSourceConfig` (DELETE — batch's kept), `mybatisMysqlConfig` (DELETE — batch's kept), `SystemConfig` (DELETE — socket), `InterceptorConfig`, `MyWebConfig`, plus any others. Move the non-colliding ones:

```bash
# move only classes that do NOT collide with what's already in app
git mv tbpApply/src/main/java/com/chenzhen/config/MyWebConfig.java app/src/main/java/com/chenzhen/config/MyWebConfig.java
# InterceptorConfig: tbpApply has one and api had one (Task 2). Keep ONE.
```
For `InterceptorConfig`: compare `tbpApply/.../config/InterceptorConfig.java` with whatever remains in `app/.../config/InterceptorConfig.java` (from api, possibly deleted in Task 2 Step 4). Keep the tbpApply version (it has the request-logging/whitelist interceptor) and delete the api one if it still exists:

```bash
# if api's InterceptorConfig still exists in app, remove it and use tbpApply's
git rm -f app/src/main/java/com/chenzhen/config/InterceptorConfig.java 2>/dev/null || true
git mv tbpApply/src/main/java/com/chenzhen/config/InterceptorConfig.java app/src/main/java/com/chenzhen/config/InterceptorConfig.java
```
Move any other tbpApply config classes that aren't `DataSourceConfig`/`mybatisMysqlConfig`/`SystemConfig`/`mybatisDbNConfig`:
```bash
ls tbpApply/src/main/java/com/chenzhen/config/
# for each remaining file NOT in the delete list, git mv it into app/src/main/java/com/chenzhen/config/
```

- [ ] **Step 4: Delete the Oracle + duplicate config classes**

```bash
git rm tbpApply/src/main/java/com/chenzhen/TbpApplication.java
git rm tbpApply/src/main/java/com/chenzhen/config/SystemConfig.java
git rm tbpApply/src/main/java/com/chenzhen/config/DataSourceConfig.java
git rm tbpApply/src/main/java/com/chenzhen/config/mybatisMysqlConfig.java
git rm tbpApply/src/main/java/com/chenzhen/config/mybatisDb1Config.java
git rm tbpApply/src/main/java/com/chenzhen/config/mybatisDb2Config.java
git rm tbpApply/src/main/java/com/chenzhen/config/mybatisDb3Config.java
git rm tbpApply/src/main/java/com/chenzhen/config/mybatisDb4Config.java
git rm tbpApply/src/main/java/com/chenzhen/config/mybatisDb5Config.java
git rm tbpApply/src/main/java/com/chenzhen/config/mybatisDb6Config.java
git rm tbpApply/src/main/java/com/chenzhen/config/mybatisDb7Config.java
git rm tbpApply/src/main/java/com/chenzhen/config/mybatisDb8Config.java
```

- [ ] **Step 5: Delete the Oracle mappers, base interfaces, factories, XML, bootstrap**

```bash
git rm tbpApply/src/main/java/com/chenzhen/mapper/AtsMapper.java
git rm tbpApply/src/main/java/com/chenzhen/mapper/CbsMapper.java
rm -rf tbpApply/src/main/java/com/chenzhen/mapper/firstMapper
rm -rf tbpApply/src/main/java/com/chenzhen/mapper/secondMapper
rm -rf tbpApply/src/main/java/com/chenzhen/mapper/threeMapper
rm -rf tbpApply/src/main/java/com/chenzhen/mapper/fourMapper
rm -rf tbpApply/src/main/java/com/chenzhen/mapper/sitcbsMapper
rm -rf tbpApply/src/main/java/com/chenzhen/mapper/sitatsMapper
rm -rf tbpApply/src/main/java/com/chenzhen/mapper/sit2cbsMapper
rm -rf tbpApply/src/main/java/com/chenzhen/mapper/sit2atsMapper
git rm tbpApply/src/main/java/com/chenzhen/factory/AtsMapperFactory.java
git rm tbpApply/src/main/java/com/chenzhen/factory/CbsMapperFactory.java
rm -rf tbpApply/src/main/resources/mapper/firstMapper
rm -rf tbpApply/src/main/resources/mapper/secondMapper
rm -rf tbpApply/src/main/resources/mapper/threeMapper
rm -rf tbpApply/src/main/resources/mapper/fourMapper
rm -rf tbpApply/src/main/resources/mapper/sitcbsMapper
rm -rf tbpApply/src/main/resources/mapper/sitatsMapper
rm -rf tbpApply/src/main/resources/mapper/sit2cbsMapper
rm -rf tbpApply/src/main/resources/mapper/sit2atsMapper
git rm tbpApply/src/main/resources/bootstrap.yml
```
Stage the `rm -rf` deletions: `git add -A` later in this task's commit.

- [ ] **Step 6: Delete the `tbpApply` module dir**

```bash
rm -rf tbpApply
```

- [ ] **Step 7: Verify compile (expect Oracle-reference errors in 2 service impls — defer to Task 6)**

```bash
mvn -q -pl app -am clean compile -DskipTests 2>&1 | grep -E "ERROR|cannot find symbol" | head -40
```
Expected: errors ONLY in `UkeyServiceImpl.java` and `MessageServiceImpl.java` referencing `AtsMapperFactory`/`CbsMapperFactory`/Oracle mappers. All other classes compile. If other Oracle-reference errors appear, add those files to the Task 6 excision list.

- [ ] **Step 8: Commit (compile will fail — that's expected; commit the structural state)**

```bash
git add -A
git commit -m "refactor: move tbpApply controllers/services/mmysql mapper into app, delete Oracle datasources/mappers/factories

Co-Authored-By: Claude <noreply@anthropic.com>"
```
Note: this commit intentionally leaves the build red; Task 6 fixes it. Do not run `-DskipTests` package yet.

---

### Task 5: Introduce `ActionDispatcher`; refactor `MsgService` to dispatch in-process

**Files:**
- Create: `app/src/main/java/com/chenzhen/dispatcher/ActionDispatcher.java`
- Modify: `app/src/main/java/com/chenzhen/service/MsgService.java` (replace file-queue body with `ActionDispatcher` delegate)

**Interfaces:**
- Consumes: tbpApply services (`MessageService`, `UkeyService`, `LoginService`, `TbpService`, `UserService`, `ManifestService`) — autowired.
- Produces: `ActionDispatcher.dispatch(Result) → Result` used by `MsgService.getResult`.

- [ ] **Step 1: Create `ActionDispatcher.java`**

This bean replaces the reflection-over-`TbpApplyFeign` dispatch. It maps the `action` field inside `result.getBody()` to a tbpApply service call. Under mock mode it is never reached (MockAspect short-circuits), so only the methods needed by non-mock paths must be wired; wire all that compile cleanly and leave Oracle-derived ones to throw a clear "mock-only" error.

```java
package com.chenzhen.dispatcher;

import com.alibaba.fastjson.JSONObject;
import com.chenzhen.pojo.Result;
import com.chenzhen.service.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * In-process replacement for the former TbpApplyFeign + ./msg/*.send file queue.
 * Reads action from result.getBody() and calls the corresponding tbpApply service directly.
 * Only reached when wxyd.mock.enabled=false; mock mode short-circuits at MockAspect.
 */
@Component
@Slf4j
public class ActionDispatcher {

    @Autowired private MessageService messageService;
    @Autowired private UkeyService ukeyService;
    @Autowired private LoginService loginService;
    @Autowired private TbpService tbpService;
    @Autowired private UserService userService;
    @Autowired private ManifestService manifestService;

    public Result dispatch(Result result) {
        Object body = result.getBody();
        JSONObject json = body instanceof JSONObject ? (JSONObject) body
                : (JSONObject) JSONObject.toJSON(body);
        String action = json.getString("action");
        json.remove("action");
        result.setBody(json);
        try {
            switch (action) {
                case "addMessage":      return messageService.addMessage(result);
                case "delMessage":      return messageService.delMessage(result);
                case "queryMessage":   return messageService.queryMessage(result);
                case "getParamValue":  return messageService.getParamValue(result);
                default:
                    log.warn("[Dispatcher] action={} not wired for non-mock path; mock-only", action);
                    Result r = Result.getInstance();
                    r.setTraceId(result.getTraceId());
                    r.setClientIp(result.getClientIp());
                    return r;
            }
        } catch (Exception e) {
            log.error("[Dispatcher] action={} failed", action, e);
            return Result.getInstance();
        }
    }
}
```

> **Note for implementer:** the exact service method names/signatures must match `MessageService`/etc. Open each service interface (`app/src/main/java/com/chenzhen/service/MessageService.java`, etc.) and align the `case` bodies to the real method names. Oracle-derived actions (`queryUdOper`, `orderCreate`, `queryUdInfo`, `cfcaInfoQry`, `queryCprUser`, `requestData`, `responseData`, `start`, `udOper`, `mmLogin`, `ngLogin`, `unBindUkey`, white ops, doc ops) are mock-only under this design — they fall to `default` and return an empty `Result` when mock is off. That is accepted per the spec.

- [ ] **Step 2: Refactor `MsgService.getResult` to delegate in-process**

Replace the entire `getResult` method body (the `./msg/*.send`/`.rev` file I/O) with a call to `ActionDispatcher`. Because `MsgService` is currently a static utility, convert it to a Spring bean so it can inject `ActionDispatcher`. New file `app/src/main/java/com/chenzhen/service/MsgService.java`:

```java
package com.chenzhen.service;

import com.alibaba.fastjson.JSONObject;
import com.chenzhen.dispatcher.ActionDispatcher;
import com.chenzhen.pojo.Result;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class MsgService {

    @Autowired
    private ActionDispatcher actionDispatcher;

    public Result getResult(JSONObject json) {
        json.put("traceId", MDC.get("traceId"));
        json.put("clientIp", MDC.get("clientIp"));
        Result result = Result.getInstance();
        result.setBody(json);
        return actionDispatcher.dispatch(result);
    }

    public static String getParamValue(String key) {
        // mock-off path no longer reads ./msg files; return null when not mocked.
        // Under mock mode this static helper is not used (MockAspect short-circuits).
        return null;
    }
}
```

> **Note:** `ApiServiceImpl.uploadDoc`/`createDoc` call `MsgService.getParamValue("uploadDocIP")` and `MsgService.getResult(...)` as static. These call sites must be updated to inject `MsgService` as a bean (remove `static`). In `ApiServiceImpl`, add `@Autowired private MsgService msgService;` and replace `MsgService.getParamValue(...)`/`MsgService.getResult(...)` with `msgService.getParamValue(...)`/`msgService.getResult(...)`. Make `getParamValue` non-static (drop `static` keyword in the signature above if you prefer — but `ApiServiceImpl` calls it statically today, so either keep it static returning null OR convert call sites). Recommended: convert call sites to instance calls and make `getParamValue` non-static.

- [ ] **Step 3: Update `ApiServiceImpl` call sites**

In `app/src/main/java/com/chenzhen/service/ApiServiceImpl.java`:
- Add field: `@Autowired private MsgService msgService;`
- Replace `MsgService.getResult(` → `msgService.getResult(` (all occurrences in this file).
- Replace `MsgService.getParamValue(` → `msgService.getParamValue(`.
- Remove the `import com.chenzhen.service.MsgService;` only if unused after change (it won't be).

- [ ] **Step 4: Update `ApiController` static call**

`ApiController.uploadDoc` calls `MsgService.getParamValue("uploadDocIP")` statically. Inject the bean instead: add `@Autowired private MsgService msgService;` to `ApiController` and replace the static call with `msgService.getParamValue("uploadDocIP")`.

- [ ] **Step 5: Verify compile**

```bash
mvn -q -pl app -am clean compile -DskipTests 2>&1 | grep -E "ERROR" | head -40
```
Expected: remaining errors are ONLY the Oracle references in `UkeyServiceImpl`/`MessageServiceImpl` (Task 6). No `MsgService`/`ActionDispatcher`/`TbpApplyFeign` errors.

- [ ] **Step 6: Commit**

```bash
git add -A
git commit -m "refactor: replace .send/.rev file queue + Feign with in-process ActionDispatcher

Co-Authored-By: Claude <noreply@anthropic.com>"
```

---

### Task 6: Excise Oracle references from `UkeyServiceImpl` and `MessageServiceImpl`

**Files:**
- Modify: `app/src/main/java/com/chenzhen/service/UkeyServiceImpl.java`
- Modify: `app/src/main/java/com/chenzhen/service/MessageServiceImpl.java`
- Possibly modify: other files flagged by Task 4 Step 7's compile error scan.

**Interfaces:**
- Consumes: the Oracle-free `MysqlMapper` for any MySQL-backed persistence these services still need.
- Produces: a green compile (`mvn clean compile` BUILD SUCCESS).

- [ ] **Step 1: Locate every Oracle reference**

```bash
grep -n "AtsMapperFactory\|CbsMapperFactory\|FirstMapper\|SecondMapper\|ThreeMapper\|FourMapper\|SitcbsMapper\|SitatsMapper\|Sit2cbsMapper\|Sit2atsMapper\|AtsMapper\|CbsMapper" \
  app/src/main/java/com/chenzhen/service/UkeyServiceImpl.java \
  app/src/main/java/com/chenzhen/service/MessageServiceImpl.java
```

- [ ] **Step 2: Excise Oracle calls in `UkeyServiceImpl.java`**

For each match: remove the `@Autowired`/`@Resource` field of the factory/mapper and every method body that calls it. Since mock mode covers all `tbphx.do` actions and the Oracle-derived methods are mock-only, replace the affected method bodies with a no-op that returns an empty/default `Result` (or the method's declared return type's default). Concretely, for a method like:

```java
public Result queryUdInfo(Result result) {
    AtsMapper m = atsMapperFactory.get(...);
    return ...;
}
```
becomes:

```java
public Result queryUdInfo(Result result) {
    return Result.getInstance();
}
```
Remove now-unused imports (`com.chenzhen.factory.*`, Oracle mapper imports). Keep methods that only use `MysqlMapper` unchanged.

- [ ] **Step 3: Excise Oracle calls in `MessageServiceImpl.java`** — same approach as Step 2.

- [ ] **Step 4: Verify green compile**

```bash
mvn -q -pl app -am clean compile -DskipTests
```
Expected: BUILD SUCCESS. If new Oracle-reference errors surface in other files (e.g. `TbpServiceImpl`), apply the same excision and re-run.

- [ ] **Step 5: Commit**

```bash
git add -A
git commit -m "refactor: remove Oracle mapper/factory references from Ukey/Message services

Co-Authored-By: Claude <noreply@anthropic.com>"
```

---

### Task 7: Finalize `application.yml`, merge resources, clean logback

**Files:**
- Modify: `app/src/main/resources/application.yml` (already created in Task 1; finalize).
- Modify: `app/src/main/resources/logback.xml` — remove the `com.alibaba.nacos.client` logger entry if it merged in from tbpApply's logback.
- Ensure present: `app/src/main/resources/mock/**`, `static/**`, `mapper/BatchMapper.xml`, `mapper/MysqlMapper.xml`.
- Create: `app/src/main/resources/env.properties` (optional secrets; see Step 3).

- [ ] **Step 1: Confirm `application.yml` matches the Task 1 version**

It must contain: port 8090, context `/api`, the single `spring.datasource` (HikariCP, `${datasource.*}` placeholders), `mybatis.mapperLocations: classpath:mapper/*.xml`, `wxyd.mock.enabled: true`, multipart config. No `spring.cloud.nacos`, no `feign`, no `ifp.session`. If tbpApply's logback got moved, verify no `bootstrap.yml` exists:

```bash
ls app/src/main/resources/ | grep -i bootstrap || echo "no bootstrap.yml (good)"
```

- [ ] **Step 2: Add `env.properties` for secrets (optional)**

```bash
cat > app/src/main/resources/env.properties <<'EOF'
datasource.url=jdbc:mysql://localhost:3306/wxyd?useSSL=false&characterEncoding=utf8&serverTimezone=Asia/Shanghai
datasource.username=root
datasource.password=root
cfca.socketServerIP=127.0.0.1
EOF
```
This file is git-ignored secrets in real deployments; committing a placeholder is fine for the merge. (Adjust `.gitignore` if it ignores `env.properties` — keep the placeholder tracked for now.)

- [ ] **Step 3: Clean `logback.xml`**

```bash
grep -n "nacos" app/src/main/resources/logback.xml || echo "no nacos logger (good)"
```
If a `<logger name="com.alibaba.nacos.client" .../>` line exists, delete that line.

- [ ] **Step 4: Verify mock resources present**

```bash
ls app/src/main/resources/mock/tbphx.do/ | head
ls app/src/main/resources/mapper/
```
Expected: `action=*.json` files under `mock/tbphx.do/`, and `BatchMapper.xml` + `MysqlMapper.xml` under `mapper/`.

- [ ] **Step 5: Package (skip tests — no harness)**

```bash
mvn -q -pl app -am clean package -DskipTests
```
Expected: BUILD SUCCESS, produces `app/target/app-1.0.0.jar`.

- [ ] **Step 6: Commit**

```bash
git add -A
git commit -m "refactor: finalize single application.yml + merged resources, drop nacos logger

Co-Authored-By: Claude <noreply@anthropic.com>"
```

---

### Task 8: Boot + smoke-test the mock endpoint

**Files:** none (verification only).

- [ ] **Step 1: Start the app**

```bash
mvn -q -pl app spring-boot:run 2>&1 | tee /tmp/wxyd-boot.log &
```
Wait for `wxyd启动成功` and no startup errors. (MySQL must be reachable per `env.properties`; if no MySQL is available, the datasource bean still initializes lazily — Hikari won't connect until first query. The mock path never touches the DB, so boot should succeed without a live MySQL. If boot fails on datasource, set `spring.datasource.driver-class-name` and url as above and retry.)

- [ ] **Step 2: Build a tbphx.do transData and POST it**

The `tbphx.do` flow: `transData` = `btoa(URLEncode(json + "||" + timestamp))`. Construct one for `action=queryMsgCode`:

```bash
JSON='{"action":"queryMsgCode"}'
TS=$(date +%s%3N)
RAW="${JSON}||${TS}"
ENCODED=$(printf '%s' "$RAW" | jq -sRr '@uri' | tr -d '\n')
# base64-encode (btoa uses a standard base64 of the URL-encoded string)
TRANSDATA=$(printf '%s' "$ENCODED" | base64 | tr -d '\n')
curl -s "http://localhost:8090/api/tbphx.do?transData=${TRANSDATA}"
echo
```
Expected: JSON matching `app/src/main/resources/mock/tbphx.do/action=queryMsgCode.json` (the mock response), printed to stdout. The log should show `[MOCK] tbphx.do action=queryMsgCode → ...`.

> If `jq`/`base64` differ on the platform, replicate `BtoAAtoB.atob/btoa` (standard base64) + URL-encoding in a small Java one-liner or use the existing `mock/tbphx.do/action=queryMsgCode.json` content directly as the assertion oracle.

- [ ] **Step 3: Assert the response equals the mock file**

```bash
EXPECTED=$(cat app/src/main/resources/mock/tbphx.do/action=queryMsgCode.json)
RESPONSE=$(curl -s "http://localhost:8090/api/tbphx.do?transData=${TRANSDATA}")
[ "$RESPONSE" = "$EXPECTED" ] && echo "SMOKE PASS" || echo "SMOKE FAIL"
```
Expected: `SMOKE PASS`.

- [ ] **Step 4: Stop the app**

```bash
# find and kill the spring-boot process
pkill -f "spring-boot:run" || true
```

- [ ] **Step 5: Final commit (if any cleanup)**

```bash
git add -A
git commit -m "test: smoke-test tbphx.do mock endpoint green

Co-Authored-By: Claude <noreply@anthropic.com>" 2>/dev/null || echo "nothing to commit"
```

---

## Self-Review notes (for the implementer)

- **Spec coverage:** Module merge (T1–T4), no Nacos (T1 pom + T7 yml), no Feign (T3 + T5 ActionDispatcher), Oracle removed (T4 + T6), single MySQL (T1/T3 config), file-queue removed (T5), mock-first (T7 + T8), ifpSession cleaned (T2) — all spec sections covered.
- **Collisions resolved:** `DataSourceConfig`/`mybatisMysqlConfig` keep batch's; both `SystemConfig` deleted; `InterceptorConfig` keeps tbpApply's; three old main classes replaced by `WxydApplication`.
- **Deferred decisions handled:** `BatchMapper` retained (Task 3) since it's the only MySQL mapper for batch tables and survives in `com.chenzhen.mapper`; module name `app`; DB creds via `env.properties` placeholders.
