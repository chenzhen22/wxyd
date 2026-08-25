# 用户管理重做 + 钉钉机器人 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 把 IP 白名单用户管理重做为国密 SM4 密码 + 注册/审批/HTTP Session 登录，并新增每用户钉钉机器人（CRUD + 文本/文件发送，文件 zip+base64、>20KB 拆 ≤18KB 块）。

**Architecture:** 新 `users` + `ding_robot` 两表；`AuthController`/`UserController`/`RobotController` 实现 `CommController`（dev 模式 MockAspect 无 mock 文件即直通真实逻辑，MockAspect.java:88 已确认）；`AuthInterceptor` 守会话；`DataInitializer` 播超管；`Sm4KeyHolder` 封装 SM4（密钥在 env.properties）。前置一个全局包名重命名 `com.chenzhen`→`com.cyz`。

**Tech Stack:** Java 8, Spring Boot 2.2.5.RELEASE, MyBatis 3.0.0, MySQL 8, fastjson2, BouncyCastle（SM4 已有 SMUtil）, OkHttp（DingTalkRobotUtil 已有）, Lombok, Hutool（可选）。

**Spec:** `docs/superpowers/specs/2026-08-24-user-auth-robot-design.md`

## Global Constraints

- **无单元测试框架。** 验证 = `mvn -pl app -am clean compile`（JDK17 编 Java8 源）→ package → boot → smoke。步骤从 TDD 改为「compile 门控 + 末尾 boot/smoke」。
- **永不 `git add` env.properties**（含真实 DB 密钥 + SM4 密钥 + 超管密码）。Task 4/9 只本地编辑，不提交。
- 包名前缀 **`com.cyz`**（Task 1 重命名后所有新代码用此前缀）。
- 分支 `V3.0`，每个 Task 末尾 commit，消息带 `Co-Authored-By: Claude <noreply@anthropic.com>`。
- 现有 util 已就绪：`SMUtil.generateSM4Key()`/`sm4Encrypt`/`sm4Decrypt`、`DingTalkRobotUtil.sendTextMsg(RobotConfig, content, atMobiles, isAtAll)`、`ZipUtils.zipToBase64(byte[])`。
- `CommUtils` 已有：`getClientIp(HttpServletRequest)`、`getClientIpByMDC()`、`getParamValue(String)`、`handleDaoResult(int)`。
- `Result` pojo 已有：`Result.getInstance()`、`setBody`、`setErrorCode`、`setErrorMsg`、`getBody`。

---

## Task 1: 全局包名重命名 com.chenzhen → com.cyz

**Files:**
- Move: `app/src/main/java/com/chenzhen/**` → `app/src/main/java/com/cyz/**`（63 个 .java）
- Modify: 所有 .java 的 `package`/`import`、`app/src/main/resources/mapper/*.xml` 的 namespace/type、`app/src/main/resources/application.yml`、`app/src/main/resources/logback.xml`

**Interfaces:** 无（纯重命名，对外行为不变）。

- [ ] **Step 1: 移动整个包目录**

Run:
```powershell
cd D:/GIT/wxyd
git mv app/src/main/java/com/chenzhen app/src/main/java/com/cyz
```
Expected: 目录移动成功，63 个文件随之迁移。

- [ ] **Step 2: 全局替换 chenzhen → cyz（所有 .java + .xml + .yml）**

Run:
```powershell
cd D:/GIT/wxyd
$files = Get-ChildItem -Recurse app/src -Include *.java,*.xml,*.yml -File -ErrorAction SilentlyContinue
foreach ($f in $files) {
    $c = Get-Content $f.FullName -Raw
    if ($c -match 'chenzhen') {
        $c = $c -replace 'chenzhen','cyz'
        Set-Content $f.FullName -Value $c -NoNewline
    }
}
```
Expected: 无报错。`MockAspect` 的 pointcut 串 `execution(* com.chenzhen.controller.CommController+.*(..))` 变 `com.cyz.controller.CommController+`；MyBatis XML namespace `com.chenzhen.mapper...` 变 `com.cyz.mapper...`。

- [ ] **Step 3: 确认无残留 chenzhen**

Run:
```powershell
cd D:/GIT/wxyd
Get-ChildItem -Recurse app/src -Include *.java,*.xml,*.yml -File | Select-String 'chenzhen' | Measure-Object | Select-Object -ExpandProperty Count
```
Expected: `0`

- [ ] **Step 4: Compile（JDK17）→ 必须 GREEN**

Run:
```powershell
$env:JAVA_HOME = "D:\cz\project\jdk-17.0.19+10"
cd D:/GIT/wxyd; mvn -pl app -am clean compile -DskipTests 2>&1 | Select-String "BUILD SUCCESS|BUILD FAILURE|ERROR" | Select-Object -First 5
```
Expected: `BUILD SUCCESS`。若失败，多半是某个 import 漏改，按报错定位修。

- [ ] **Step 5: Commit**

```powershell
cd D:/GIT/wxyd; git add -A; git commit -m "refactor: rename package com.chenzhen -> com.cyz (63 files)

Co-Authored-By: Claude <noreply@anthropic.com>"
```

---

## Task 2: 清理免密登录基础设施（grep 门控）

**Files:**
- Delete: `controller/LoginController.java`、`service/LoginService.java`+`LoginServiceImpl.java`、`service/HttpRequestCPR09003.java`、`pojo/SocketMessage.java`、`pojo/ESBPojo.java`+`OSBData.java`+`ReqSvcHeader.java`+`SvcBody.java`+`MsgBean.java`+`UUS1007T.java`+`UUS1009T.java`+`listen/OSBDataListener.java`
- Modify: `mapper/MysqlMapper.java`+`MysqlMapper.xml`（删 socket 三方法）、`dispatcher/ActionDispatcher.java`（删 LoginService 注入）

**Interfaces:** 无（删孤儿）。

- [ ] **Step 1: grep 门控 — 确认登录设施仅互相引用**

Run:
```powershell
cd D:/GIT/wxyd
"--- HttpRequestCPR09003 callers (除自身) ---"
Get-ChildItem -Recurse app/src/main/java -Filter *.java | Select-String 'HttpRequestCPR09003' | Where-Object { $_.Filename -ne 'HttpRequestCPR09003.java' }
"--- SocketMessage callers (除自身) ---"
Get-ChildItem -Recurse app/src/main/java -Filter *.java | Select-String 'SocketMessage' | Where-Object { $_.Filename -ne 'SocketMessage.java' }
"--- ESBPojo family callers (除 pojo 自身) ---"
Get-ChildItem -Recurse app/src/main/java -Filter *.java | Select-String 'ESBPojo|OSBData|ReqSvcHeader|SvcBody|MsgBean|UUS1007|UUS1009' | Where-Object { $_.Path -notmatch '\\pojo\\' }
```
Expected: 命中只出现在 `LoginController`/`LoginServiceImpl`/`HttpRequestCPR09003`/`OSBDataListener` 之间（这些文件本 Task 全删）。若出现在其他保留文件，**停止**，先清该引用。

- [ ] **Step 2: 删登录相关文件**

```powershell
cd D:/GIT/wxyd
git rm app/src/main/java/com/cyz/controller/LoginController.java
git rm app/src/main/java/com/cyz/service/LoginService.java
git rm app/src/main/java/com/cyz/service/LoginServiceImpl.java
git rm app/src/main/java/com/cyz/service/HttpRequestCPR09003.java
git rm app/src/main/java/com/cyz/pojo/SocketMessage.java
git rm app/src/main/java/com/cyz/pojo/ESBPojo.java
git rm app/src/main/java/com/cyz/pojo/OSBData.java
git rm app/src/main/java/com/cyz/pojo/ReqSvcHeader.java
git rm app/src/main/java/com/cyz/pojo/SvcBody.java
git rm app/src/main/java/com/cyz/pojo/MsgBean.java
git rm app/src/main/java/com/cyz/pojo/UUS1007T.java
git rm app/src/main/java/com/cyz/pojo/UUS1009T.java
git rm app/src/main/java/com/cyz/listen/OSBDataListener.java
```

- [ ] **Step 3: MysqlMapper 删 socket 三方法**

Edit `app/src/main/java/com/cyz/mapper/mysqlMapper/MysqlMapper.java`：删除这三个声明：
```java
	int addSocketMessage(SocketMessage SocketMessage);
	String querySocketRevice(String date);
	int updateSocketMsgStatus(String date, String status);
```
保留 message/white/client/log 方法。

- [ ] **Step 4: MysqlMapper.xml 删对应三条语句**

Edit `app/src/main/resources/mapper/MysqlMapper.xml`：删除 `<update id="addSocketMessage">`、`<select id="querySocketRevice">`、`<update id="updateSocketMsgStatus">` 三段。

- [ ] **Step 5: ActionDispatcher 删 LoginService 注入**

Edit `app/src/main/java/com/cyz/dispatcher/ActionDispatcher.java`：删 `import com.cyz.service.LoginService;` 与 `@Autowired private LoginService loginService;`。switch 不变（login 未接）。

- [ ] **Step 6: BatchMapper.xml 也清 socket 引用（若有）**

Run: `Get-ChildItem app/src/main/resources/mapper -Filter *.xml | Select-String 'SocketMessage|addSocketMessage|querySocketRevice|updateSocketMsgStatus'`
若有命中，对应删除（BatchMapper 的 `updateSocketMessage` 用的是 `Message` pojo，不是 `SocketMessage`——确认后只删真正引用已删 pojo 的语句）。

- [ ] **Step 7: Compile → GREEN**

同 Task 1 Step 4。预期 BUILD SUCCESS。

- [ ] **Step 8: Commit**

```powershell
cd D:/GIT/wxyd; git add -A; git commit -m "refactor: remove 免密登录 infra (LoginController/ServiceImpl, HttpRequestCPR09003, SocketMessage, ESBPojo family, OSBDataListener, socket mapper methods)

Co-Authored-By: Claude <noreply@anthropic.com>"
```

---

## Task 3: DDL + Pojos + Mapper 新增

**Files:**
- Create: `app/src/main/resources/sql/V2__user_robot.sql`
- Create: `app/src/main/java/com/cyz/pojo/User.java`、`app/src/main/java/com/cyz/pojo/DingRobot.java`
- Modify: `app/src/main/java/com/cyz/mapper/mysqlMapper/MysqlMapper.java`、`app/src/main/resources/mapper/MysqlMapper.xml`

**Interfaces:**
- Produces: `User`、`DingRobot` pojo；`MysqlMapper` 的 users/robot 方法（供 Task 4-7 用）。

- [ ] **Step 1: 写 DDL 脚本**

Create `app/src/main/resources/sql/V2__user_robot.sql`：
```sql
-- 用户管理重做 + 钉钉机器人 表结构（运营在 dev/prod MySQL 各执行一次）
CREATE TABLE IF NOT EXISTS `users` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `username` VARCHAR(64) NOT NULL,
  `password` VARCHAR(256) NOT NULL COMMENT 'SM4 密文 hex',
  `display_name` VARCHAR(64) DEFAULT NULL,
  `role` TINYINT NOT NULL DEFAULT 1 COMMENT '0=超级管理员,1=普通',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '0=已通过,1=待审批,2=已拒绝',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `ding_robot` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT NOT NULL,
  `name` VARCHAR(128) NOT NULL,
  `access_token` VARCHAR(256) NOT NULL,
  `secret` VARCHAR(512) NOT NULL COMMENT 'SM4 加密入库',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 可选：备份旧 client 表后删除
-- RENAME TABLE `client` TO `client_bak_20260824`;
```

- [ ] **Step 2: User pojo**

Create `app/src/main/java/com/cyz/pojo/User.java`：
```java
package com.cyz.pojo;

import lombok.Data;

@Data
public class User {
    private Long id;
    private String username;
    private String password;
    private String displayName;
    private Integer role;
    private Integer status;
    private String createTime;
}
```

- [ ] **Step 3: DingRobot pojo**

Create `app/src/main/java/com/cyz/pojo/DingRobot.java`：
```java
package com.cyz.pojo;

import lombok.Data;

@Data
public class DingRobot {
    private Long id;
    private Long userId;
    private String name;
    private String accessToken;
    private String secret;
    private String createTime;
}
```

- [ ] **Step 4: MysqlMapper 新增 users 方法**

Edit `app/src/main/java/com/cyz/mapper/mysqlMapper/MysqlMapper.java`，在 `addLog` 后追加：
```java
	// ===== users =====
	int countUsers();

	int insertUser(User user);

	User queryUserByUsername(String username);

	User queryUserById(Long id);

	List<User> listUsers();

	int updateUserStatus(@Param("id") Long id, @Param("status") Integer status);

	int deleteUser(Long id);

	// ===== ding_robot =====
	int insertRobot(DingRobot robot);

	DingRobot queryRobotById(Long id);

	List<DingRobot> listRobotsByUserId(Long userId);

	int updateRobot(DingRobot robot);

	int deleteRobot(@Param("id") Long id, @Param("userId") Long userId);
```
加 import `import org.apache.ibatis.annotations.Param;`（若未导入）。

- [ ] **Step 5: MysqlMapper.xml 新增对应语句**

Edit `app/src/main/resources/mapper/MysqlMapper.xml`，在 `</mapper>` 前追加：
```xml
	<!-- ===== users ===== -->
	<select id="countUsers" resultType="Integer">
		select count(1) from `users`
	</select>

	<insert id="insertUser" parameterType="com.cyz.pojo.User" useGeneratedKeys="true" keyProperty="id">
		insert into `users` (`username`,`password`,`display_name`,`role`,`status`) values (#{username},#{password},#{displayName},#{role},#{status})
	</insert>

	<select id="queryUserByUsername" parameterType="String" resultType="com.cyz.pojo.User">
		select `id`,`username`,`password`,`display_name`,`role`,`status`,date_format(`create_time`,'%Y-%m-%d %H:%i:%s') as createTime from `users` where `username`=#{username}
	</select>

	<select id="queryUserById" parameterType="Long" resultType="com.cyz.pojo.User">
		select `id`,`username`,`display_name`,`role`,`status`,date_format(`create_time`,'%Y-%m-%d %H:%i:%s') as createTime from `users` where `id`=#{id}
	</select>

	<select id="listUsers" resultType="com.cyz.pojo.User">
		select `id`,`username`,`display_name`,`role`,`status`,date_format(`create_time`,'%Y-%m-%d %H:%i:%s') as createTime from `users` order by `status`,`id`
	</select>

	<update id="updateUserStatus">
		update `users` set `status`=#{status} where `id`=#{id}
	</update>

	<delete id="deleteUser" parameterType="Long">
		delete from `users` where `id`=#{id}
	</delete>

	<!-- ===== ding_robot ===== -->
	<insert id="insertRobot" parameterType="com.cyz.pojo.DingRobot" useGeneratedKeys="true" keyProperty="id">
		insert into `ding_robot` (`user_id`,`name`,`access_token`,`secret`) values (#{userId},#{name},#{accessToken},#{secret})
	</insert>

	<select id="queryRobotById" parameterType="Long" resultType="com.cyz.pojo.DingRobot">
		select `id`,`user_id`,`name`,`access_token`,`secret`,date_format(`create_time`,'%Y-%m-%d %H:%i:%s') as createTime from `ding_robot` where `id`=#{id}
	</select>

	<select id="listRobotsByUserId" parameterType="Long" resultType="com.cyz.pojo.DingRobot">
		select `id`,`user_id`,`name`,`access_token`,date_format(`create_time`,'%Y-%m-%d %H:%i:%s') as createTime from `ding_robot` where `user_id`=#{userId} order by `id`
	</select>

	<update id="updateRobot" parameterType="com.cyz.pojo.DingRobot">
		update `ding_robot` set `name`=#{name},`access_token`=#{accessToken}
		<if test="secret != null and secret != ''">
			,`secret`=#{secret}
		</if>
		where `id`=#{id} and `user_id`=#{userId}
	</update>

	<delete id="deleteRobot">
		delete from `ding_robot` where `id`=#{id} and `user_id`=#{userId}
	</delete>
```

- [ ] **Step 6: Compile → GREEN**

预期 BUILD SUCCESS。

- [ ] **Step 7: Commit**

```powershell
cd D:/GIT/wxyd; git add -A; git commit -m "feat: add users/ding_robot DDL + pojos + mapper methods

Co-Authored-By: Claude <noreply@anthropic.com>"
```

---

## Task 4: Sm4KeyHolder + DataInitializer

**Files:**
- Create: `app/src/main/java/com/cyz/config/Sm4KeyHolder.java`
- Create: `app/src/main/java/com/cyz/config/DataInitializer.java`
- Modify (local, 不提交): `app/src/main/resources/env.properties`

**Interfaces:**
- Produces: `Sm4KeyHolder.encrypt(String):String`、`Sm4KeyHolder.decrypt(String):String`（供 Task 5/7 用）。

- [ ] **Step 1: Sm4KeyHolder**

Create `app/src/main/java/com/cyz/config/Sm4KeyHolder.java`：
```java
package com.cyz.config;

import com.cyz.util.SMUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class Sm4KeyHolder {

    @Value("${wxyd.sm4.key}")
    private String sm4Key;

    public String encrypt(String plain) {
        return SMUtil.sm4Encrypt(sm4Key, plain);
    }

    public String decrypt(String cipherHex) {
        return SMUtil.sm4Decrypt(sm4Key, cipherHex);
    }
}
```

- [ ] **Step 2: DataInitializer（播超管）**

Create `app/src/main/java/com/cyz/config/DataInitializer.java`：
```java
package com.cyz.config;

import com.cyz.mapper.mysqlMapper.MysqlMapper;
import com.cyz.pojo.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class DataInitializer implements ApplicationRunner {

    @Autowired
    private MysqlMapper mysqlMapper;

    @Autowired
    private Sm4KeyHolder sm4KeyHolder;

    @Value("${wxyd.admin.username:admin}")
    private String adminUsername;

    @Value("${wxyd.admin.password:}")
    private String adminPassword;

    @Override
    public void run(ApplicationArguments args) {
        int count = mysqlMapper.countUsers();
        if (count > 0) {
            log.info("[INIT] users 表已有 {} 个用户，跳过超管播种", count);
            return;
        }
        if (adminPassword == null || adminPassword.isEmpty()) {
            log.warn("[INIT] wxyd.admin.password 未配置，跳过超管播种（请在 env.properties 设置后重启）");
            return;
        }
        User u = new User();
        u.setUsername(adminUsername);
        u.setPassword(sm4KeyHolder.encrypt(adminPassword));
        u.setDisplayName("超级管理员");
        u.setRole(0);
        u.setStatus(0);
        mysqlMapper.insertUser(u);
        log.info("[INIT] 已初始化超级管理员: {}，请及时修改密码", adminUsername);
    }
}
```

- [ ] **Step 3: env.properties 加 3 键（本地，不 git add）**

Edit `app/src/main/resources/env.properties`，追加：
```
wxyd.sm4.key=<用 SMUtil.generateSM4Key() 跑一次得到的 32 位 hex>
wxyd.admin.username=admin
wxyd.admin.password=<设一个初始密码>
```
生成密钥：临时改 `SMUtil.main` 调 `System.out.println(SMUtil.generateSM4Key())` 跑一次，或写个临时类。
**不要 `git add env.properties`。**

- [ ] **Step 4: Compile → GREEN**

- [ ] **Step 5: Commit（不含 env.properties）**

```powershell
cd D:/GIT/wxyd; git add app/src/main/java/com/cyz/config/; git commit -m "feat: Sm4KeyHolder + DataInitializer (super admin seeding)

Co-Authored-By: Claude <noreply@anthropic.com>"
```
验证 env.properties 未进：
```powershell
git show --stat HEAD | Select-String 'env.properties'
```
预期：无输出。

---

## Task 5: 认证（AuthController + AuthService + AuthInterceptor）

**Files:**
- Create: `app/src/main/java/com/cyz/service/AuthService.java`、`app/src/main/java/com/cyz/service/AuthServiceImpl.java`
- Create: `app/src/main/java/com/cyz/controller/AuthController.java`
- Create: `app/src/main/java/com/cyz/config/AuthInterceptor.java`、`app/src/main/java/com/cyz/config/WebMvcConfig.java`

**Interfaces:**
- Consumes: `Sm4KeyHolder`、`MysqlMapper.queryUserByUsername/insertUser`、HTTP Session。
- Produces: `POST /register`、`POST /login`、`POST /logout`；session 键 `userId`/`username`/`role`；`AuthInterceptor` 守会话。

- [ ] **Step 1: AuthService 接口**

Create `app/src/main/java/com/cyz/service/AuthService.java`：
```java
package com.cyz.service;

import com.cyz.pojo.User;

public interface AuthService {

    User register(String username, String password, String displayName);

    /** 登录校验，返回 user（密码字段置空）；失败返回 errorCode 的 Result 由 controller 包 */
    User login(String username, String password);
}
```

- [ ] **Step 2: AuthServiceImpl**

Create `app/src/main/java/com/cyz/service/AuthServiceImpl.java`：
```java
package com.cyz.service;

import com.cyz.config.Sm4KeyHolder;
import com.cyz.mapper.mysqlMapper.MysqlMapper;
import com.cyz.pojo.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class AuthServiceImpl implements AuthService {

    @Autowired
    private MysqlMapper mysqlMapper;

    @Autowired
    private Sm4KeyHolder sm4KeyHolder;

    @Override
    public User register(String username, String password, String displayName) {
        if (mysqlMapper.queryUserByUsername(username) != null) {
            throw new IllegalArgumentException("用户名已存在");
        }
        User u = new User();
        u.setUsername(username);
        u.setPassword(sm4KeyHolder.encrypt(password));
        u.setDisplayName(displayName);
        u.setRole(1);
        u.setStatus(1); // 待审批
        mysqlMapper.insertUser(u);
        u.setPassword(null);
        return u;
    }

    @Override
    public User login(String username, String password) {
        User u = mysqlMapper.queryUserByUsername(username);
        if (u == null) {
            throw new IllegalArgumentException("用户名或密码错误");
        }
        String inputCipher = sm4KeyHolder.encrypt(password);
        if (!inputCipher.equals(u.getPassword())) {
            throw new IllegalArgumentException("用户名或密码错误");
        }
        u.setPassword(null);
        return u;
    }
}
```

- [ ] **Step 3: AuthController**

Create `app/src/main/java/com/cyz/controller/AuthController.java`：
```java
package com.cyz.controller;

import com.cyz.pojo.Result;
import com.cyz.pojo.User;
import com.cyz.service.AuthService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpSession;
import java.util.Map;

@RestController
@Slf4j
public class AuthController implements CommController {

    @Autowired
    private AuthService authService;

    @ResponseBody
    @RequestMapping("register")
    public Result register(@RequestBody Result result) {
        Map<String, Object> map = (Map<String, Object>) result.getBody();
        String username = (String) map.get("username");
        String password = (String) map.get("password");
        String displayName = (String) map.get("displayName");
        result = Result.getInstance();
        try {
            User u = authService.register(username, password, displayName);
            result.setBody(u);
        } catch (IllegalArgumentException e) {
            result.setErrorCode("000003");
            result.setErrorMsg(e.getMessage());
        }
        return result;
    }

    @ResponseBody
    @RequestMapping("login")
    public Result login(@RequestBody Result result, HttpSession session) {
        Map<String, Object> map = (Map<String, Object>) result.getBody();
        String username = (String) map.get("username");
        String password = (String) map.get("password");
        result = Result.getInstance();
        try {
            User u = authService.login(username, password);
            if (u.getStatus() != null && u.getStatus() == 1) {
                result.setErrorCode("000003");
                result.setErrorMsg("账号待审批，请联系管理员");
                return result;
            }
            if (u.getStatus() != null && u.getStatus() == 2) {
                result.setErrorCode("000003");
                result.setErrorMsg("账号已被拒绝");
                return result;
            }
            session.setAttribute("userId", u.getId());
            session.setAttribute("username", u.getUsername());
            session.setAttribute("role", u.getRole());
            result.setBody(u);
        } catch (IllegalArgumentException e) {
            result.setErrorCode("000003");
            result.setErrorMsg(e.getMessage());
        }
        return result;
    }

    @ResponseBody
    @RequestMapping("logout")
    public Result logout(HttpSession session) {
        session.invalidate();
        return Result.getInstance();
    }
}
```

- [ ] **Step 4: AuthInterceptor**

Create `app/src/main/java/com/cyz/config/AuthInterceptor.java`：
```java
package com.cyz.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

@Component
@Slf4j
public class AuthInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest req, HttpServletResponse resp, Object handler) throws Exception {
        HttpSession session = req.getSession(false);
        if (session != null && session.getAttribute("userId") != null) {
            return true;
        }
        // ajax 请求返回 401；页面请求重定向 login.html
        String accept = req.getHeader("Accept");
        String xReq = req.getHeader("X-Requested-With");
        if ((accept != null && accept.contains("application/json")) || "XMLHttpRequest".equals(xReq)) {
            resp.setStatus(401);
            resp.setContentType("application/json;charset=UTF-8");
            resp.getWriter().write("{\"errorCode\":\"000401\",\"errorMsg\":\"未登录或会话过期\"}");
        } else {
            String ctx = req.getContextPath();
            resp.sendRedirect(ctx + "/login.html");
        }
        return false;
    }
}
```

- [ ] **Step 5: WebMvcConfig（注册拦截器 + 静态资源）**

Create `app/src/main/java/com/cyz/config/WebMvcConfig.java`：
```java
package com.cyz.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Autowired
    private AuthInterceptor authInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns(
                        "/login", "/register", "/login.html", "/register.html",
                        "/**/*.html", "/**/*.js", "/**/*.css", "/**/*.png",
                        "/**/*.jpg", "/**/*.gif", "/**/*.svg", "/**/*.ico",
                        "/**/*.woff", "/**/*.woff2", "/**/*.ttf",
                        "/", "/actuator/**", "/error"
                );
    }
}
```

- [ ] **Step 6: Compile → GREEN**

- [ ] **Step 7: Commit**

```powershell
cd D:/GIT/wxyd; git add -A; git commit -m "feat: auth (register/login/logout) + AuthInterceptor + WebMvcConfig

Co-Authored-By: Claude <noreply@anthropic.com>"
```

---

## Task 6: 用户管理重做（UserController + UserService）

**Files:**
- Modify (重写): `app/src/main/java/com/cyz/controller/UserController.java`、`app/src/main/java/com/cyz/service/UserService.java`、`app/src/main/java/com/cyz/service/UserSericeImpl.java`
- Modify: `app/src/main/resources/static/api.html`、`app/src/main/resources/static/Assets/js/index.js`（用户管理区改造，与 Task 8 一起；本 Task 只做 Java）

**Interfaces:**
- Consumes: session（取当前用户、判超管）、`MysqlMapper.listUsers/updateUserStatus/deleteUser`。
- Produces: `GET /user/list`、`POST /approveUser`、`POST /rejectUser`、`POST /deleteUser`。

- [ ] **Step 1: UserService 接口重写**

Overwrite `app/src/main/java/com/cyz/service/UserService.java`：
```java
package com.cyz.service;

import com.cyz.pojo.User;

import java.util.List;

public interface UserService {

    List<User> listUsers();

    int approveUser(Long id);

    int rejectUser(Long id);

    int deleteUser(Long id);

    String getUserName(String clientIp);
}
```
（保留 `getUserName` 给 MessageServiceImpl 用——但其签名从 client 表查。**注意**：client 表将被 users 表取代，`getUserName(ip)` 不再适用。MessageServiceImpl 用它取留言用户名。改为按当前登录用户取，见 Step 4 说明。）

- [ ] **Step 2: UserSericeImpl 重写**

Overwrite `app/src/main/java/com/cyz/service/UserSericeImpl.java`：
```java
package com.cyz.service;

import com.cyz.mapper.mysqlMapper.MysqlMapper;
import com.cyz.pojo.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserSericeImpl implements UserService {

    @Autowired
    private MysqlMapper mysqlMapper;

    @Override
    public List<User> listUsers() {
        return mysqlMapper.listUsers();
    }

    @Override
    public int approveUser(Long id) {
        return mysqlMapper.updateUserStatus(id, 0);
    }

    @Override
    public int rejectUser(Long id) {
        return mysqlMapper.updateUserStatus(id, 2);
    }

    @Override
    public int deleteUser(Long id) {
        return mysqlMapper.deleteUser(id);
    }

    @Override
    public String getUserName(String clientIp) {
        // 旧 client 表已废弃；留言板改用登录用户名（见 MessageController 改造）
        return null;
    }
}
```

- [ ] **Step 3: UserController 重写**

Overwrite `app/src/main/java/com/cyz/controller/UserController.java`：
```java
package com.cyz.controller;

import com.cyz.pojo.Result;
import com.cyz.pojo.User;
import com.cyz.service.UserService;
import com.cyz.util.CommUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpSession;
import java.util.List;
import java.util.Map;

@RestController
@Slf4j
public class UserController implements CommController {

    @Autowired
    private UserService userService;

    @ResponseBody
    @RequestMapping("user/list")
    public Result listUsers(HttpSession session) {
        result = Result.getInstance();
        Integer role = (Integer) session.getAttribute("role");
        Long uid = (Long) session.getAttribute("userId");
        if (role != null && role == 0) {
            List<User> all = userService.listUsers();
            result.setBody(all);
        } else {
            // 普通用户只看自己
            result.setBody(userService.listUsers().stream().filter(u -> uid.equals(u.getId())).collect(java.util.stream.Collectors.toList()));
        }
        return result;
    }

    @ResponseBody
    @RequestMapping("approveUser")
    public Result approveUser(@RequestBody Result result, HttpSession session) {
        Integer role = (Integer) session.getAttribute("role");
        result = Result.getInstance();
        if (role == null || role != 0) {
            result.setErrorCode("000003");
            result.setErrorMsg("权限不足，仅超级管理员可审批");
            return result;
        }
        Map<String, Object> map = (Map<String, Object>) result.getBody();
        // 注意：上面 result 已重置，body 丢失；改用独立变量
        return result;
    }
}
```

> ⚠️ **修正**：`approveUser`/`rejectUser`/`deleteUser` 需在重置 result 前先取 body。修正后最终代码：

Overwrite `app/src/main/java/com/cyz/controller/UserController.java`（最终版）：
```java
package com.cyz.controller;

import com.cyz.pojo.Result;
import com.cyz.pojo.User;
import com.cyz.service.UserService;
import com.cyz.util.CommUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpSession;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@Slf4j
public class UserController implements CommController {

    @Autowired
    private UserService userService;

    @ResponseBody
    @RequestMapping("user/list")
    public Result listUsers(HttpSession session) {
        Result result = Result.getInstance();
        Integer role = (Integer) session.getAttribute("role");
        Long uid = (Long) session.getAttribute("userId");
        List<User> all = userService.listUsers();
        if (role != null && role == 0) {
            result.setBody(all);
        } else {
            final Long self = uid;
            result.setBody(all.stream().filter(u -> self.equals(u.getId())).collect(Collectors.toList()));
        }
        return result;
    }

    @ResponseBody
    @RequestMapping("approveUser")
    public Result approveUser(@RequestBody Result result, HttpSession session) {
        Map<String, Object> map = (Map<String, Object>) result.getBody();
        Long id = ((Number) map.get("id")).longValue();
        Result r = Result.getInstance();
        if (!isAdmin(session)) {
            r.setErrorCode("000003");
            r.setErrorMsg("权限不足，仅超级管理员可审批");
            return r;
        }
        int idx = userService.approveUser(id);
        return CommUtils.handleDaoResult(idx);
    }

    @ResponseBody
    @RequestMapping("rejectUser")
    public Result rejectUser(@RequestBody Result result, HttpSession session) {
        Map<String, Object> map = (Map<String, Object>) result.getBody();
        Long id = ((Number) map.get("id")).longValue();
        Result r = Result.getInstance();
        if (!isAdmin(session)) {
            r.setErrorCode("000003");
            r.setErrorMsg("权限不足，仅超级管理员可操作");
            return r;
        }
        int idx = userService.rejectUser(id);
        return CommUtils.handleDaoResult(idx);
    }

    @ResponseBody
    @RequestMapping("deleteUser")
    public Result deleteUser(@RequestBody Result result, HttpSession session) {
        Map<String, Object> map = (Map<String, Object>) result.getBody();
        Long id = ((Number) map.get("id")).longValue();
        Result r = Result.getInstance();
        if (!isAdmin(session)) {
            r.setErrorCode("000003");
            r.setErrorMsg("权限不足，仅超级管理员可操作");
            return r;
        }
        int idx = userService.deleteUser(id);
        return CommUtils.handleDaoResult(idx);
    }

    private boolean isAdmin(HttpSession session) {
        Integer role = (Integer) session.getAttribute("role");
        return role != null && role == 0;
    }
}
```

- [ ] **Step 4: MessageServiceImpl 去掉 getUserName(ip) 依赖**

MessageServiceImpl 现用 `userService.getUserName(clientIp)` 取留言用户名（client 表已废弃，返回 null→显示"游客"）。改为：登录后留言用户名从 session 取。

Edit `app/src/main/java/com/cyz/service/MessageServiceImpl.java` 的 `queryMessageById`：把 `String userName = userService.getUserName(clientIp);` 改为从 session 取（MessageController 把 session user 传入，或 MessageServiceImpl 用 `RequestContextHolder` 取 session）。最简：保留 `userService.getUserName` 返回 null 的兜底逻辑（已显示"游客"），但更好的做法是 MessageController 在 `addMessage` 时把当前登录用户名一起存。**本 plan 取最小改动**：保留 getUserName 返回 null → 显示"游客"，后续留言板按登录用户名优化留作 follow-up。

> 即：本步**不改 MessageServiceImpl**，`UserSericeImpl.getUserName` 返回 null 已兼容（留言显示"游客"）。compile 不受影响。

- [ ] **Step 5: 删旧 white/client mapper 方法（若残留）**

确认 MysqlMapper.java/XML 已无 `queryWhiteInfo`/`addWhite`/`updateWhiteByUsername`/`updateWhiteByIp`/`deleteWhite`/`queryWhiteUrl`/`queryClientInfo`/`getUserName`/`getUserNameByStatus`（client 表废弃）。若有，删除声明 + 语句。

Run:
```powershell
Get-ChildItem app/src -Recurse -Include *.java,*.xml | Select-String 'queryWhiteInfo|addWhite|updateWhiteBy|deleteWhite|queryWhiteUrl|queryClientInfo|getUserNameByStatus'
```
预期：无命中（或仅 Task 6 新代码不再引用）。命中处删除。

- [ ] **Step 6: Compile → GREEN**

- [ ] **Step 7: Commit**

```powershell
cd D:/GIT/wxyd; git add -A; git commit -m "feat: redo user management (list/approve/reject/delete, session-role gated); drop IP whitelist

Co-Authored-By: Claude <noreply@anthropic.com>"
```

---

## Task 7: 钉钉机器人（RobotController + RobotService + 发送拆分）

**Files:**
- Create: `app/src/main/java/com/cyz/service/RobotService.java`、`app/src/main/java/com/cyz/service/RobotServiceImpl.java`
- Create: `app/src/main/java/com/cyz/controller/RobotController.java`
- Modify: `app/src/main/java/com/cyz/util/ZipUtils.java`（加 `compressToBase64(byte[], String)`）

**Interfaces:**
- Consumes: `Sm4KeyHolder`、`MysqlMapper` robot 方法、`DingTalkRobotUtil`、`ZipUtils`。
- Produces: `GET /robot/list`、`POST /robot/add`、`/robot/update`、`/robot/delete`、`POST /robot/send`（multipart）。

- [ ] **Step 1: ZipUtils 加压缩+base64 便捷方法**

Edit `app/src/main/java/com/cyz/util/ZipUtils.java`，在 `isValidZip` 后追加：
```java
    /**
     * 把单个文件字节数组 zip 压缩后转 Base64。
     *
     * @param fileBytes 原始文件字节
     * @param entryName zip 内条目名（通常=原文件名）
     * @return 压缩后 zip 字节的 Base64
     */
    public static String compressToBase64(byte[] fileBytes, String entryName) throws IOException {
        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
        try (java.util.zip.ZipOutputStream zos = new java.util.zip.ZipOutputStream(baos)) {
            java.util.zip.ZipEntry entry = new java.util.zip.ZipEntry(entryName);
            zos.putNextEntry(entry);
            zos.write(fileBytes);
            zos.closeEntry();
        }
        return zipToBase64(baos.toByteArray());
    }
```

- [ ] **Step 2: RobotService 接口**

Create `app/src/main/java/com/cyz/service/RobotService.java`：
```java
package com.cyz.service;

import com.cyz.pojo.DingRobot;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

public interface RobotService {

    List<DingRobot> list(Long userId);

    int addRobot(Long userId, DingRobot robot);

    int updateRobot(Long userId, DingRobot robot);

    int deleteRobot(Long userId, Long id);

    Map<String, Object> send(Long userId, Long robotId, String type, String text, MultipartFile file) throws Exception;
}
```

- [ ] **Step 3: RobotServiceImpl**

Create `app/src/main/java/com/cyz/service/RobotServiceImpl.java`：
```java
package com.cyz.service;

import com.cyz.config.Sm4KeyHolder;
import com.cyz.mapper.mysqlMapper.MysqlMapper;
import com.cyz.pojo.DingRobot;
import com.cyz.util.DingTalkRobotUtil;
import com.cyz.util.ZipUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class RobotServiceImpl implements RobotService {

    @Autowired
    private MysqlMapper mysqlMapper;

    @Autowired
    private Sm4KeyHolder sm4KeyHolder;

    @Override
    public List<DingRobot> list(Long userId) {
        List<DingRobot> list = mysqlMapper.listRobotsByUserId(userId);
        for (DingRobot r : list) {
            r.setSecret(mask(r.getSecret()));
        }
        return list;
    }

    @Override
    public int addRobot(Long userId, DingRobot robot) {
        robot.setUserId(userId);
        robot.setSecret(sm4KeyHolder.encrypt(robot.getSecret()));
        return mysqlMapper.insertRobot(robot);
    }

    @Override
    public int updateRobot(Long userId, DingRobot robot) {
        robot.setUserId(userId);
        if (robot.getSecret() != null && !robot.getSecret().isEmpty()) {
            robot.setSecret(sm4KeyHolder.encrypt(robot.getSecret()));
        }
        return mysqlMapper.updateRobot(robot);
    }

    @Override
    public int deleteRobot(Long userId, Long id) {
        return mysqlMapper.deleteRobot(id, userId);
    }

    @Override
    public Map<String, Object> send(Long userId, Long robotId, String type, String text, MultipartFile file) throws Exception {
        DingRobot robot = mysqlMapper.queryRobotById(robotId);
        Map<String, Object> resp = new HashMap<>();
        if (robot == null || !userId.equals(robot.getUserId())) {
            resp.put("error", "机器人不存在或无权操作");
            return resp;
        }
        String accessToken = robot.getAccessToken();
        String secret = sm4KeyHolder.decrypt(robot.getSecret());
        DingTalkRobotUtil.RobotConfig config = new DingTalkRobotUtil.RobotConfig(accessToken, secret);

        if ("text".equals(type)) {
            String r = DingTalkRobotUtil.sendTextMsg(config, text, null, false);
            resp.put("type", "text");
            resp.put("result", r);
            return resp;
        }

        // file
        byte[] fileBytes = file.getBytes();
        String filename = file.getOriginalFilename();
        String base64 = ZipUtils.compressToBase64(fileBytes, filename);

        int threshold = 20480;   // 20KB
        int maxChunk = 18432;     // 18KB
        List<String> chunks = new ArrayList<>();
        if (base64.length() > threshold) {
            for (int i = 0; i < base64.length(); i += maxChunk) {
                chunks.add(base64.substring(i, Math.min(i + maxChunk, base64.length())));
            }
        } else {
            chunks.add(base64);
        }
        int total = chunks.size();
        List<String> results = new ArrayList<>();
        for (int idx = 0; idx < total; idx++) {
            String msg = String.format("[FILE|%s|%d|%d|%s]", filename, idx, total, chunks.get(idx));
            String r = DingTalkRobotUtil.sendTextMsg(config, msg, null, false);
            results.add(r);
        }
        resp.put("type", "file");
        resp.put("filename", filename);
        resp.put("totalChunks", total);
        resp.put("results", results);
        return resp;
    }

    private String mask(String secret) {
        if (secret == null) return null;
        return "****";
    }
}
```

- [ ] **Step 4: RobotController**

Create `app/src/main/java/com/cyz/controller/RobotController.java`：
```java
package com.cyz.controller;

import com.cyz.pojo.DingRobot;
import com.cyz.pojo.Result;
import com.cyz.service.RobotService;
import com.cyz.util.CommUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpSession;
import java.util.List;
import java.util.Map;

@RestController
@Slf4j
public class RobotController implements CommController {

    @Autowired
    private RobotService robotService;

    @ResponseBody
    @RequestMapping("robot/list")
    public Result list(HttpSession session) {
        Long uid = (Long) session.getAttribute("userId");
        Result result = Result.getInstance();
        result.setBody(robotService.list(uid));
        return result;
    }

    @ResponseBody
    @RequestMapping("robot/add")
    public Result add(@RequestBody Result result, HttpSession session) {
        Long uid = (Long) session.getAttribute("userId");
        Map<String, Object> map = (Map<String, Object>) result.getBody();
        DingRobot robot = new DingRobot();
        robot.setName((String) map.get("name"));
        robot.setAccessToken((String) map.get("accessToken"));
        robot.setSecret((String) map.get("secret"));
        int idx = robotService.addRobot(uid, robot);
        return CommUtils.handleDaoResult(idx);
    }

    @ResponseBody
    @RequestMapping("robot/update")
    public Result update(@RequestBody Result result, HttpSession session) {
        Long uid = (Long) session.getAttribute("userId");
        Map<String, Object> map = (Map<String, Object>) result.getBody();
        DingRobot robot = new DingRobot();
        robot.setId(((Number) map.get("id")).longValue());
        robot.setName((String) map.get("name"));
        robot.setAccessToken((String) map.get("accessToken"));
        robot.setSecret((String) map.get("secret"));
        int idx = robotService.updateRobot(uid, robot);
        return CommUtils.handleDaoResult(idx);
    }

    @ResponseBody
    @RequestMapping("robot/delete")
    public Result delete(@RequestBody Result result, HttpSession session) {
        Long uid = (Long) session.getAttribute("userId");
        Map<String, Object> map = (Map<String, Object>) result.getBody();
        Long id = ((Number) map.get("id")).longValue();
        int idx = robotService.deleteRobot(uid, id);
        return CommUtils.handleDaoResult(idx);
    }

    @ResponseBody
    @RequestMapping("robot/send")
    public Result send(@RequestParam("robotId") Long robotId,
                       @RequestParam("type") String type,
                       @RequestParam(value = "text", required = false) String text,
                       @RequestParam(value = "file", required = false) MultipartFile file,
                       HttpSession session) {
        Long uid = (Long) session.getAttribute("userId");
        Result result = Result.getInstance();
        try {
            Map<String, Object> resp = robotService.send(uid, robotId, type, text, file);
            result.setBody(resp);
        } catch (Exception e) {
            log.error("机器人发送失败", e);
            result.setErrorCode("000003");
            result.setErrorMsg(e.getMessage());
        }
        return result;
    }
}
```

- [ ] **Step 5: Compile → GREEN**

- [ ] **Step 6: Commit**

```powershell
cd D:/GIT/wxyd; git add -A; git commit -m "feat: DingTalk robot (CRUD + text/file send with zip+base64, >20KB split to <=18KB chunks)

Co-Authored-By: Claude <noreply@anthropic.com>"
```

---

## Task 8: 前端（login/register/api.html/index.js/index.html）

**Files:**
- Create: `app/src/main/resources/static/login.html`、`app/src/main/resources/static/register.html`
- Modify: `app/src/main/resources/static/api.html`（新增钉钉机器人菜单+模块、用户管理区改造）
- Modify: `app/src/main/resources/static/Assets/js/index.js`（robot CRUD/send、用户管理 approve/reject）
- Modify: `app/src/main/resources/static/index.html`（跳 login.html）

**Interfaces:** 无 Java。

- [ ] **Step 1: login.html**

Create `app/src/main/resources/static/login.html`：
```html
<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <title>登录 — 微银行</title>
    <link rel="stylesheet" href="Assets/css/style.css">
</head>
<body>
<div class="login-box">
    <h2>登录</h2>
    <div class="cert-form-row">
        <label>用户名：</label><input id="loginUser" type="text">
    </div>
    <div class="cert-form-row">
        <label>密码：</label><input id="loginPwd" type="password">
    </div>
    <div class="cert-btn-group">
        <button class="btn-main" onclick="doLogin()">登录</button>
        <a href="register.html">注册新用户</a>
    </div>
    <div id="loginTip" style="color:red;margin-top:10px"></div>
</div>
<script src="https://cdn.bootcss.com/jquery/3.4.1/jquery.min.js"></script>
<script>
function doLogin(){
    var u=$("#loginUser").val(),p=$("#loginPwd").val();
    if(!u||!p){$("#loginTip").text("请输入用户名和密码");return;}
    $.post("login",{transData:btoa(unescape(encodeURIComponent(JSON.stringify({username:u,password:p}))))},
    function(data){
        if(data.errorCode=="000000"){location.href="api.html";}
        else{$("#loginTip").text(data.errorMsg);}
    },"json").fail(function(){$("#loginTip").text("网络异常");});
}
</script>
</body>
</html>
```

> 注意：`/login`/`/register` 端点接收的是 `Result` JSON（body 是 map）。上面用 `btoa(JSON)` 包了一层——但 AuthController 按 `Result` 解析 body。需对齐：**直接发 JSON 对象 `{username,password}` 作 body，但端点期望 `Result{body:{username,password}}`**。改为：发 `JSON.stringify({body:{username:u,password:p}})` 作 application/json。

修正后 login.html `<script>`：
```html
<script src="https://cdn.bootcss.com/jquery/3.4.1/jquery.min.js"></script>
<script>
function doLogin(){
    var u=$("#loginUser").val(),p=$("#loginPwd").val();
    if(!u||!p){$("#loginTip").text("请输入用户名和密码");return;}
    $.ajax({url:"login",type:"post",contentType:"application/json",
        data:JSON.stringify({body:{username:u,password:p}}),
        success:function(data){
            if(data.errorCode=="000000"){location.href="api.html";}
            else{$("#loginTip").text(data.errorMsg);}
        },error:function(){$("#loginTip").text("网络异常");}});
}
</script>
```

- [ ] **Step 2: register.html**

Create `app/src/main/resources/static/register.html`：
```html
<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8"><title>注册 — 微银行</title>
    <link rel="stylesheet" href="Assets/css/style.css">
</head>
<body>
<div class="login-box">
    <h2>注册新用户</h2>
    <div class="cert-form-row"><label>用户名：</label><input id="rUser" type="text"></div>
    <div class="cert-form-row"><label>密码：</label><input id="rPwd" type="password"></div>
    <div class="cert-form-row"><label>显示名：</label><input id="rName" type="text"></div>
    <div class="cert-btn-group"><button class="btn-main" onclick="doRegister()">提交注册</button>
        <a href="login.html">返回登录</a></div>
    <div id="rTip" style="color:green;margin-top:10px"></div>
</div>
<script src="https://cdn.bootcss.com/jquery/3.4.1/jquery.min.js"></script>
<script>
function doRegister(){
    var u=$("#rUser").val(),p=$("#rPwd").val(),n=$("#rName").val();
    if(!u||!p){$("#rTip").text("用户名和密码必填");return;}
    $.ajax({url:"register",type:"post",contentType:"application/json",
        data:JSON.stringify({body:{username:u,password:p,displayName:n}}),
        success:function(data){
            if(data.errorCode=="000000"){$("#rTip").text("注册成功，等待管理员审批");}
            else{$("#rTip").text(data.errorMsg);}
        },error:function(){$("#rTip").text("网络异常");}});
}
</script>
</body>
</html>
```

- [ ] **Step 3: index.html 改跳转**

Overwrite `app/src/main/resources/static/index.html`：
```html
<!DOCTYPE html>
<html>
<head><meta charset="UTF-8"><meta http-equiv="refresh" content="0;url=login.html"></head>
<body></body>
</html>
```

- [ ] **Step 4: api.html 加「钉钉机器人」菜单项 + 模块区**

Edit `app/src/main/resources/static/api.html`：在留言板菜单项后加：
```html
            <li class="menu-item" data-target="robotManage"><i>🤖</i>钉钉机器人</li>
```
在 messageBoard 模块区后加（`</div>` 主内容区前）：
```html
        <div class="module-box" id="robotManage">
            <div class="page-title">钉钉机器人</div>
            <div class="tool-bar">
                <button class="btn-main" id="btnAddRobot">新增机器人</button>
                <button class="btn-main" id="btnRefreshRobot">刷新</button>
            </div>
            <table class="data-table">
                <thead><tr><th>序号</th><th>名称</th><th>Token</th><th>操作</th></tr></thead>
                <tbody id="robotTableBody"></tbody>
            </table>
            <div class="cert-card" id="robotForm" style="display:none">
                <div class="cert-form-row"><label>名称：</label><input id="robotName" type="text"></div>
                <div class="cert-form-row"><label>ACCESS_TOKEN：</label><input id="robotToken" type="text" style="width:400px"></div>
                <div class="cert-form-row"><label>SECRET：</label><input id="robotSecret" type="text" style="width:400px"></div>
                <div class="cert-btn-group"><button class="btn-main" id="btnSaveRobot">保存</button>
                    <input type="hidden" id="robotId"></div>
            </div>
            <div class="cert-card">
                <div class="cert-card-tit">发送消息</div>
                <div class="cert-form-row">
                    <label>选择机器人：</label><select id="sendRobotSel"></select>
                    <label>类型：</label><select id="sendType"><option value="text">纯文本</option><option value="file">文件</option></select>
                </div>
                <div class="cert-form-item" id="textInput"><textarea id="sendText" style="width:600px;height:120px"></textarea></div>
                <div class="cert-form-item" id="fileInput" style="display:none"><input type="file" id="sendFile"></div>
                <div class="cert-btn-group"><button class="btn-main" id="btnSend">发送</button></div>
                <div id="sendResult" style="margin-top:10px;color:#555"></div>
            </div>
        </div>
```
（用户管理区已有模块，改造其 tbody + 按钮见 Step 5）

- [ ] **Step 5: api.html 用户管理区改造（待审批 + 已通过）**

Edit 用户管理 module-box：把原白名单表格替换为用户列表 + 审批按钮结构：
```html
        <div class="module-box" id="userManage">
            <div class="page-title">用户管理</div>
            <div class="cert-btn-group"><button class="btn-main" id="btnRefreshUser">刷新</button></div>
            <table class="data-table">
                <thead><tr><th>序号</th><th>用户名</th><th>显示名</th><th>角色</th><th>状态</th><th>注册时间</th><th>操作</th></tr></thead>
                <tbody id="userTableBody"></tbody>
            </table>
        </div>
```

- [ ] **Step 6: index.js — robot CRUD/send + 用户管理**

Edit `app/src/main/resources/static/Assets/js/index.js`，新增（在 `loadMsg` 前）：
```javascript
// ===================== 钉钉机器人 =====================
function loadRobots(){
    $.get("robot/list",function(res){
        let list=res.body||[]; let html="";
        for(let i=0;i<list.length;i++){
            let r=list[i];
            html+=`<tr><td>${i+1}</td><td>${r.name}</td><td>${(r.accessToken||'').slice(-6)}</td>
                <td><button class="btn-del" data-id="${r.id}">删除</button></td></tr>`;
        }
        $("#robotTableBody").html(html);
        let sel=$("#sendRobotSel").html("");
        for(let r of list){sel.append(`<option value="${r.id}">${r.name}</option>`);}
    },"json");
}
$("#btnRefreshRobot").click(loadRobots);
$("#btnAddRobot").click(function(){$("#robotForm").show();$("#robotId").val("");});
$("#btnSaveRobot").click(function(){
    let body={name:$("#robotName").val(),accessToken:$("#robotToken").val(),secret:$("#robotSecret").val()};
    let id=$("#robotId").val();
    let url=id?"robot/update":"robot/add";
    if(id)body.id=parseInt(id);
    $.ajax({url,type:"post",contentType:"application/json",data:JSON.stringify({body}),
        success:function(res){alterModal(res.errorCode=="000000"?"保存成功":res.errorMsg);loadRobots();$("#robotForm").hide();}});
});
$(document).on("click","#robotTableBody .btn-del",function(){
    let id=$(this).data("id");
    if(!confirm("确定删除？"))return;
    $.ajax({url:"robot/delete",type:"post",contentType:"application/json",data:JSON.stringify({body:{id:parseInt(id)}}),
        success:function(res){alterModal(res.errorCode=="000000"?"删除成功":res.errorMsg);loadRobots();}});
});
$("#sendType").change(function(){$("#textInput").toggle($(this).val()=="text");$("#fileInput").toggle($(this).val()=="file");});
$("#btnSend").click(function(){
    let fd=new FormData();
    fd.append("robotId",$("#sendRobotSel").val());
    fd.append("type",$("#sendType").val());
    if($("#sendType").val()=="text")fd.append("text",$("#sendText").val());
    else fd.append("file",$("#sendFile")[0].files[0]);
    $.ajax({url:"robot/send",type:"post",processData:false,contentType:false,data:fd,
        success:function(res){
            if(res.errorCode=="000000"){
                let b=res.body||{};
                $("#sendResult").text(b.type=="file"?`文件已发：共${b.totalChunks}块`:"文本已发");
            }else{$("#sendResult").text(res.errorMsg);}
        },error:function(){$("#sendResult").text("发送异常");}});
});

// ===================== 用户管理 =====================
function loadUsers(){
    $.get("user/list",function(res){
        let list=res.body||[]; let html="";
        for(let i=0;i<list.length;i++){
            let u=list[i];
            let role=u.role==0?"超管":"普通";
            let st=u.status==0?"已通过":u.status==1?"待审批":"已拒绝";
            let op=u.status==1?`<button class="btn-main" onclick="approveUser(${u.id})">通过</button> <button class="btn-del" onclick="rejectUser(${u.id})">拒绝</button>`:"";
            html+=`<tr><td>${i+1}</td><td>${u.username}</td><td>${u.displayName||''}</td><td>${role}</td><td>${st}</td><td>${u.createTime||''}</td><td>${op}</td></tr>`;
        }
        $("#userTableBody").html(html);
    },"json");
}
$("#btnRefreshUser").click(loadUsers);
function approveUser(id){$.ajax({url:"approveUser",type:"post",contentType:"application/json",data:JSON.stringify({body:{id:id}}),success:function(){loadUsers();}});}
function rejectUser(id){$.ajax({url:"rejectUser",type:"post",contentType:"application/json",data:JSON.stringify({body:{id:id}}),success:function(){loadUsers();}});}
```
在 ready 块里加 `loadRobots();loadUsers();` 的初始调用（登录后）。

- [ ] **Step 7: Commit**

```powershell
cd D:/GIT/wxyd; git add -A; git commit -m "feat: frontend login/register pages + robot module + user-management redo

Co-Authored-By: Claude <noreply@anthropic.com>"
```

---

## Task 9: 验证（package → boot → smoke）

**Files:** 无。

- [ ] **Step 1: 执行 DDL**

运营在 dev MySQL 执行 `app/src/main/resources/sql/V2__user_robot.sql`。

- [ ] **Step 2: env.properties 配好**

确认 `wxyd.sm4.key` / `wxyd.admin.username` / `wxyd.admin.password` 已写（Task 4 Step 3）。**不 git add。**

- [ ] **Step 3: Package**

```powershell
$env:JAVA_HOME = "D:\cz\project\jdk-17.0.19+10"
cd D:/GIT/wxyd; mvn -pl app -am clean package -DskipTests -q 2>&1 | Select-Object -Last 5
```
预期：产出 `app/target/app-1.0.0.jar`。

- [ ] **Step 4: Boot（Java8，含发送测试需 mock=false；或 mock=true 新端点也直通）**

```powershell
cd D:/GIT/wxyd
$p = Start-Process -FilePath "D:\cz\project\jdk1.8.0_181\bin\java.exe" -ArgumentList @('-Dwxyd.mock.enabled=true','-jar','app\target\app-1.0.0.jar') -RedirectStandardOutput "boot.log" -RedirectStandardError "boot.err" -NoNewWindow -PassThru
Start-Sleep -Seconds 14
Get-Content boot.log | Select-String "Started WxydApplication|已初始化超级管理员|APPLICATION FAILED" | Select-Object -First 3
```
预期：`Started WxydApplication` + `[INIT] 已初始化超级管理员: admin`。进程存活期间做 smoke。

- [ ] **Step 5: Smoke — 注册 + 审批 + 登录**

```powershell
# 注册普通用户
Invoke-RestMethod -Uri "http://localhost:8090/api/register" -Method Post -ContentType "application/json" -Body '{"body":{"username":"u1","password":"123","displayName":"张三"}}'
# admin 登录拿 cookie
$resp = Invoke-WebRequest -Uri "http://localhost:8090/api/login" -Method Post -ContentType "application/json" -Body '{"body":{"username":"admin","password":"<你设的密码>"}}' -SessionVariable s
# 审批 u1
Invoke-RestMethod -Uri "http://localhost:8090/api/approveUser" -Method Post -ContentType "application/json" -Body '{"body":{"id":2}}' -WebSession $s
# u1 登录
Invoke-WebRequest -Uri "http://localhost:8090/api/login" -Method Post -ContentType "application/json" -Body '{"body":{"username":"u1","password":"123"}}' -SessionVariable s2
```
预期：注册成功→admin 登录成功(200)→审批成功→u1 登录成功。

- [ ] **Step 6: Smoke — 机器人 CRUD + 发送**

```powershell
# u1 加机器人
Invoke-RestMethod -Uri "http://localhost:8090/api/robot/add" -Method Post -ContentType "application/json" -Body '{"body":{"name":"测试","accessToken":"<token>","secret":"<SEC>"}}' -WebSession $s2
# 列表
Invoke-RestMethod -Uri "http://localhost:8090/api/robot/list" -WebSession $s2
# 发文本
Invoke-RestMethod -Uri "http://localhost:8090/api/robot/send" -Method Post -WebSession $s2 -Body @{robotId="1";type="text";text="hello"} 
```
预期：add 成功→list 返回该机器人（secret 脱敏）→发文本返回钉钉响应。

- [ ] **Step 7: Smoke — 越权 + 未登录**

```powershell
# admin 不能改 u1 的机器人（update robot id=1 as admin session $s）
Invoke-RestMethod -Uri "http://localhost:8090/api/robot/delete" -Method Post -ContentType "application/json" -Body '{"body":{"id":1}}' -WebSession $s
# 未登录访问 robot/list → 401
try { Invoke-WebRequest -Uri "http://localhost:8090/api/robot/list" } catch { $_.Exception.Response.StatusCode.value__ }
```
预期：越权删除返回错误（updateRobot/deleteRobot 的 SQL 带 user_id 条件，affected rows=0 → handleDaoResult 报失败）；未登录返回 401。

- [ ] **Step 8: 停止 + 清理**

```powershell
Stop-Process $p.Id -Force -ErrorAction SilentlyContinue
Remove-Item boot.log,boot.err -ErrorAction SilentlyContinue
```

- [ ] **Step 9: 更新 progress.md**

在 `.superpowers/sdd/progress.md` 追加本阶段完成记录。

---

## Self-Review

1. **Spec 覆盖**：① users/ding_robot 表→Task 3 ✓；② SM4 密钥 env.properties→Task 4 ✓；③ 注册/审批/登录/session→Task 5/6 ✓；④ 超管播种→Task 4 ✓；⑤ 机器人 CRUD + 文本/文件拆分→Task 7 ✓；⑥ 前端登录/注册/机器人/用户管理→Task 8 ✓；⑦ 免密登录清理→Task 2 ✓；⑧ 包名重命名→Task 1 ✓。
2. **占位符**：无 TBD；Task 6 Step 3 中间出现的 ⚠️ 修正块已给出最终版完整代码。
3. **类型一致**：`User.id=Long`、`DingRobot.id/userId=Long`、session 键 `userId`/`username`/`role` 跨 Task 4-7 一致；`RobotService.send` 签名与 `RobotController` 调用一致。
4. **Mock 直通**：Task 5-7 新 Controller 实现 CommController，dev 模式 MockAspect 无 mock 文件 → proceed（spec 已确认 MockAspect:88）。
```
