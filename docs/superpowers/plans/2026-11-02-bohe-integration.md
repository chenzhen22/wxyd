# 薄荷账整合 wxyd 实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** wxyd Android 端新增底部中间「薄荷账」Tab（账单/统计/我的关注 3 卡片 + 快速记账 FAB），服务端新增 bohe 账单/好友模块 + 超管导入接口，迁移声仲账单至 chenzhen。

**Architecture:** 服务端新建 `com.cyz.bohe` 包（controller/service/mapper + XML），两张新表 `bohe_bill`/`bohe_friend`，沿用 `Result` 响应与 Session 鉴权；Android 端新增 `ui/bohe` 包（BoheScreen + 3 子卡片区 + 记账弹窗），`MainScreen.kt` 底部 Tab 由 2 变 3，数据走 `ApiClient`。

**Tech Stack:** Spring Boot 2.7 / MyBatis / MySQL / Jetpack Compose (Material3) / Kotlin coroutines。

**Spec:** docs/superpowers/specs/2026-11-02-bohe-android-integration-design.md

## Global Constraints

- Java 8、Spring Boot 2.7.18、MyBatis `mapUnderscoreToCamelCase`（新表字段 snake_case 自动映射 camelCase 属性）。
- 金额一律**整数分**（BIGINT），不存浮点。
- Controller 返回 `com.cyz.pojo.Result`（`Result.getInstance()` + `setBody`/`setErrorCode`/`setErrorMsg`）；业务 Controller 实现 `CommController` 标记接口（受 ControllerAspect 管理）。
- 鉴权：`session.getAttribute("userId")` 取 Long/Integer 用户 id；超管判断 `role == 0`（照 SqlQueryController 写法）。
- 导入接口仅超管；好友互看要求 friend 关系 `status=1`。
- 新 SQL 迁移脚本放 `app/src/main/resources/sql/V29__bohe_bill.sql`，同时提示用户在 dev/prod 手工执行（仓库惯例：注释注明"运营在各环境执行一次"）。
- Android：Compose + material3；新增字符串/颜色直接写在 Kotlin 内（沿用现有代码风格，无 strings.xml 集中管理）。
- 仓库文档同步约束不涉及 bohe（CLAUDE.md 未登记），但提交信息用 conventional commits 中文风格。

## Review Focus

1. **好友越权**：查好友账单/统计必须校验 friend `status=1`（双向），未通过/已删除关系返回空列表——测试见 Task 3 步骤 1。
2. **导入去重**：同一批 JSON 重复导入不得产生重复账单（`cloud_id` upsert，cloud_id 为 null 的记录跳过）——测试见 Task 5。
3. **导入越权**：非超管调用 `/api/bohe/import` 必须拒绝——测试见 Task 5。
4. **金额精度**：导入解析 amount 必须保持分（JSON 里可能是数字字符串），解析失败该行跳过并计数，不中断整批——测试见 Task 5。
5. **好友自己看自己**：`friendId` 等于自己时按查自己处理（Android 端不会传，但接口防御）——测试见 Task 3。

---

### Task 1: 数据库表 + Mapper 骨架

**Files:**
- Create: `app/src/main/resources/sql/V29__bohe_bill.sql`
- Create: `app/src/main/java/com/cyz/bohe/model/BoheBill.java`
- Create: `app/src/main/java/com/cyz/bohe/model/BoheFriend.java`
- Create: `app/src/main/java/com/cyz/bohe/mapper/BoheMapper.java`
- Create: `app/src/main/resources/mapper/BoheMapper.xml`

**Interfaces:**
- Produces:
  - `BoheBill { Long id; Long userId; String cloudId; Long amount; String category; String note; LocalDate billDate; Date createTime; }`（getter/setter）
  - `BoheFriend { Long id; Long userId; Long friendId; Integer status; Date createTime; Date updateTime; }`
  - Mapper 方法签名（XML 实现同名 SQL）：
    - `List<BoheBill> listBills(@Param("userId") long userId, @Param("month") String month)` — month 格式 `yyyy-MM`，null 查全部
    - `BoheBill findByCloudId(@Param("cloudId") String cloudId)`
    - `int insertBill(BoheBill bill)`
    - `int updateBillByCloudId(BoheBill bill)`
    - `List<Map<String, Object>> sumByCategory(@Param("userId") long userId, @Param("month") String month)` — 返回 `[{category, total}]`
    - `Long sumMonth(@Param("userId") long userId, @Param("month") String month)`
    - `BoheFriend findFriend(@Param("a") long a, @Param("b") long b)` — 查 (a,b) 或 (b,a) 任一方向
    - `List<BoheFriend> listFriendsByUser(@Param("userId") long userId, @Param("status") Integer status)`
    - `int insertFriend(BoheFriend f)`
    - `int updateFriendStatus(@Param("userId") long userId, @Param("friendId") long friendId, @Param("status") int status)` — 只按精确方向更新
    - `List<Map<String, Object>> searchUsers(@Param("keyword") String keyword)` — `SELECT id, username, display_name FROM users WHERE username LIKE ... OR display_name LIKE ... LIMIT 20`

- [ ] **Step 1: 写 V29__bohe_bill.sql**

按 spec 中给出的两张表 DDL 原文（bohe_bill 含 `uk_cloud_id` 唯一键、`idx_user_date`；bohe_friend 含 `uk_pair`），文件头注释注明"运营在 dev/prod MySQL 各执行一次"。

- [ ] **Step 2: 写 model 两个 POJO**

字段即 Interfaces 所列，Lombok `@Data`（项目已用 Lombok 1.18.30）。

- [ ] **Step 3: 写 BoheMapper 接口 + XML**

XML 放 `resources/mapper/`（与现有 MysqlMapper.xml 同目录，MyBatis 配置已扫描该目录）。所有 SQL 手写；`sumByCategory` 按 `category` GROUP BY、`SUM(amount) AS total`，month 过滤用 `DATE_FORMAT(bill_date, '%Y-%m') = #{month}`。`findFriend` 的 WHERE 为 `(user_id=#{a} AND friend_id=#{b}) OR (user_id=#{b} AND friend_id=#{a})`。

- [ ] **Step 4: 编译验证**

Run: `mvn -pl app -am compile -q`
Expected: BUILD SUCCESS

- [ ] **Step 5: Commit**

```bash
git add app/src/main/resources/sql/V29__bohe_bill.sql app/src/main/java/com/cyz/bohe app/src/main/resources/mapper/BoheMapper.xml
git commit -m "feat(bohe): 账单与好友表结构及 Mapper"
```

---

### Task 2: BoheService 核心逻辑

**Files:**
- Create: `app/src/main/java/com/cyz/bohe/service/BoheService.java`
- Test: 无独立测试目录（仓库无 `app/src/test`），验证走 Task 4 编译 + Task 6 联调。本任务的正确性由 Task 3/5 的 controller 层防御与 SQL 单义性保证。

**Interfaces:**
- Consumes: Task 1 的 `BoheMapper` 全部方法。
- Produces（`@Service`，方法均为 public）：
  - `List<BoheBill> listBills(long userId, String month)` — month 为空传 null
  - `Map<String, Object> stats(long userId, String month)` — 返回 `{ total: Long, byCategory: [{category,total}] }`
  - `boolean canView(long viewerId, long targetId)` — viewer==target 返回 true；否则 `findFriend` 非空且 status==1
  - `void saveBill(long userId, Long amount, String category, String note, LocalDate billDate)` — amount 为 null 或 <=0 抛 `IllegalArgumentException("金额无效")`
  - `List<Map<String, Object>> searchUsers(String keyword)` — keyword 空返回空列表
  - `List<BoheFriend> listFriends(long userId)`
  - `void requestFriend(long userId, long friendId)` — 不能加自己（抛 IllegalArgumentException("不能添加自己")）；已存在非删除关系抛 IllegalArgumentException("已存在好友关系或申请")
  - `void handleFriend(long userId, long friendId, boolean accept)` — 只操作"对方发起、自己接收"方向（userId 作 friend_id 侧），不存在抛 IllegalArgumentException
  - `Map<String, Integer> importBills(long targetUserId, String json)` — 解析云导出 JSON 数组（fastjson2 `JSON.parseArray`），字段 `_id/amount/category/note/date/createTime`；`cloudId=_id`，amount 按 Long.parseLong(String.valueOf(...)) 解析、失败该行跳过；date 为 `yyyy-MM-dd`（容错 ISO 时间串取前 10 位）；返回 `{inserted, updated, skipped}`；先 `findByCloudId`，存在则 `updateBillByCloudId` 否则 `insertBill`；userId 固定 targetUserId

- [ ] **Step 1: 实现 BoheService**

注入 BoheMapper，按 Interfaces 语义实现；`importBills` 用 fastjson2（`com.alibaba.fastjson2.JSON`，项目已在用）。`canView` 中 viewer==target 短路在先。

- [ ] **Step 2: 编译验证**

Run: `mvn -pl app -am compile -q`
Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/cyz/bohe/service/BoheService.java
git commit -m "feat(bohe): 账单/好友/导入业务逻辑"
```

---

### Task 3: BoheController（业务接口 + 好友权限）

**Files:**
- Create: `app/src/main/java/com/cyz/bohe/controller/BoheController.java`

**Interfaces:**
- Consumes: Task 2 `BoheService` 全部方法；`session.getAttribute("userId")`（Number → longValue）。
- Produces（`@RestController`，实现 `CommController`，前缀省略 context-path）：
  - `@RequestMapping("bohe/bill/list")` GET，参数 `month`(可选)、`friendId`(可选 long) → `Result{bills: List<BoheBill>}`；有 friendId 时 `canView` 为 false 返回错误码 000003 "无权查看该用户账单"
  - `@RequestMapping("bohe/bill/save")` POST body `{amount, category, note, date:yyyy-MM-dd}` → 记自己的账
  - `@RequestMapping("bohe/stats")` GET，参数同 bill/list → `Result{stats}`（含 canView 校验，同上）
  - `@RequestMapping("bohe/friend/list")` GET → `Result{friends}`（每项附 friendInfo: id/username/display_name，组装自 users 表）
  - `@RequestMapping("bohe/friend/request")` POST body `{username}` → 按 username 查出目标用户 id 后 requestFriend
  - `@RequestMapping("bohe/friend/handle")` POST body `{userId, accept}` → handleFriend
  - `@RequestMapping("bohe/user/search")` GET 参数 `keyword` → `Result{users}`

- [ ] **Step 1: 实现 BoheController**

照 SqlQueryController 风格（try/catch → setErrorMsg，IllegalArgumentException 的 message 直接进 errorMsg，错误码 000003）。userId 从 session 取：`Object uid = session.getAttribute("userId"); long me = ((Number) uid).longValue();`。

- [ ] **Step 2: 编译验证**

Run: `mvn -pl app -am compile -q`
Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/cyz/bohe/controller/BoheController.java
git commit -m "feat(bohe): 业务接口（账单/统计/好友/搜索）"
```

---

### Task 4: 导入接口（超管）

**Files:**
- Modify: `app/src/main/java/com/cyz/bohe/controller/BoheController.java`

**Interfaces:**
- Consumes: Task 2 `importBills`；Task 1 mapper `searchUsers` 不用于此。
- Produces:
  - `@RequestMapping("bohe/import")` POST body `{targetUsername, bills: [...]}` → 仅 `session.role == 0`；按 username 查 users 表得 targetUserId（不存在报错），调 `importBills`，返回 `Result{inserted, updated, skipped}`。bills 以 JSON 数组直接放 body 字段（云导出文件内容整体读入后作为数组传给该接口；Android 端用 multipart 上传文件则先读文本——Task 7 决定传法，此处接收 JSON 对象含 bills 数组即可）。

- [ ] **Step 1: 加 import 端点**

isAdmin 判断照 SqlQueryController（`session.getAttribute("role") == 0`）。targetUserId 查询直接复用 mapper 的 `searchUsers` 不合适——加 mapper 方法 `Long findIdByUsername(@Param("username") String username)`（SELECT id FROM users WHERE username=# LIMIT 1）到 BoheMapper + XML。

- [ ] **Step 2: 编译验证**

Run: `mvn -pl app -am compile -q`
Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/cyz/bohe
git commit -m "feat(bohe): 超管导入云导出账单接口"
```

---

### Task 5: 声仲数据迁移执行

**Files:**
- Create: `docs/bohe-migration-notes.md`（记录 openid→用户 映射与导入结果，便于以后复跑）

**Interfaces:**
- Consumes: Task 4 的 `bohe/import` 接口。
- Produces: chenzhen 名下的声仲存量账单。

- [ ] **Step 1: 获取云导出 JSON**

提示用户：微信开发者工具 → 云开发控制台 → 数据库 → `bills` 集合 → 导出（JSON 格式）。文件路径由用户提供。**此步阻塞，等用户提供文件后继续。**

- [ ] **Step 2: 过滤声仲的记录并构造导入请求**

用 python（`-I`）或 jq 从导出文件筛出声仲 openid 的记录（openid 字段即 `_openid`），组装 `{targetUsername: "chenzhen", bills: [...]}`。声仲 openid 从数据中观察确定（列出所有 `_openid` 及条数，与用户确认哪个是声仲）。

- [ ] **Step 3: 调用导入接口**

以 chenzhen 登录态或超管 session 调 `POST /api/bohe/import`（可用 curl 携带 session cookie；服务器本地跑 `java -jar` 或已部署环境）。返回 `{inserted, updated, skipped}` 与导出条数核对。

- [ ] **Step 4: 抽查验证**

用 SqlQuery 工具或 SQL 抽查 `SELECT COUNT(*) FROM bohe_bill WHERE user_id=(SELECT id FROM users WHERE username='chenzhen')` 与筛选条数一致；重复执行 Step 3 一次，确认 inserted=0（updated=总数），验证去重。

- [ ] **Step 5: 记录迁移笔记并 Commit**

```bash
git add docs/bohe-migration-notes.md
git commit -m "docs(bohe): 声仲账单迁移记录"
```

---

### Task 6: Android 薄荷账 Tab 与三卡片

**Files:**
- Create: `android/app/src/main/java/com/cyz/wxyd/ui/bohe/BoheViewModel.kt`
- Create: `android/app/src/main/java/com/cyz/wxyd/ui/bohe/BoheScreen.kt`
- Modify: `android/app/src/main/java/com/cyz/wxyd/ui/nav/MainScreen.kt`（MAIN_TABS 加中间项 `TabSpec(route="bohe", label="薄荷账", icon=" mint")`，图标文案定为 `"🍃"`；NavHost 加 `composable(TabRoutes.BOHE)`；TabRoutes 加 `const val BOHE = "bohe"`）

**Interfaces:**
- Consumes: `ApiClient.getResult(path, query)` / `postBody(path, bodyFields)`（返回 JsonObject，`code==000000`? 以现有 Result.kt 解析惯例为准，实现者参照 NoteViewModel 等现有 ViewModel 的用法）。
- Produces:
  - `BoheViewModel`：状态流 `ui: StateFlow<BoheUiState>`；`BoheUiState(bills, monthTotal, byCategory, friends, showAddDialog)`；方法 `load()`（并发拉 bill/list + stats + friend/list）、`addBill(amount, category, note, date)`（成功后 reload 并收起弹窗）、`requestFriend(username)`、`handleFriend(userId, accept)`、`openFriend(friendId)`（切到好友只读视图：bills/stats 带 friendId 重拉，顶部显示对方名与"切回我"）
  - `BoheScreen(app: WxydApp)`：内部结构 = 顶部月份选择（上一月/下一月）+ 3 卡片纵向滚动：账单卡（列表项：分类 emoji + 备注 + 日期 + 金额元字符串）、统计卡（合计 + 分类金额条形行）、我的关注卡（好友列表 + "加好友"行内输入框 + 待处理申请的通过/拒绝按钮）；右下 FAB 加号弹记账对话框（分类下拉用 8 个内置分类常量、金额数字输入、备注、日期默认今天），保存调 `addBill`
- 金额展示用 `String.format("%.2f", cents / 100.0)`（仅展示层转元，存储仍分）

- [ ] **Step 1: 实现 BoheViewModel**

参照现有 ViewModel（如 `ui/notes`）的 ApiClient 调用与错误处理模式。

- [ ] **Step 2: 实现 BoheScreen 三卡片 + 记账弹窗 + 好友视图**

分类常量与小程序对齐（餐饮/交通/购物/居住/娱乐/医疗/教育/其他），emoji 用 🍚🚌🛍🏠🎮💊📚📦。

- [ ] **Step 3: MainScreen 加 Tab**

- [ ] **Step 4: 编译验证**

Run: `cd android && ./gradlew :app:compileDebugKotlin -q`
Expected: BUILD SUCCESS

- [ ] **Step 5: Commit**

```bash
git add android/app/src/main/java/com/cyz/wxyd/ui/bohe android/app/src/main/java/com/cyz/wxyd/ui/nav/MainScreen.kt
git commit -m "feat(android): 薄荷账 Tab（账单/统计/关注/快速记账）"
```

---

### Task 7: 导入入口（超管 UI）+ 全量联调

**Files:**
- Modify: `android/app/src/main/java/com/cyz/wxyd/ui/bohe/BoheScreen.kt`（仅超管显示"导入账单"入口：选文件 + 目标用户名输入，走 `ApiClient.postMultipart` 或读文本后 postBody）
- Modify: `android/app/src/main/java/com/cyz/wxyd/ui/profile/ProfileScreen.kt`（若超管工具区已有菜单则并入，否则放薄荷账页顶部小入口）

**Interfaces:**
- Consumes: Task 4 `bohe/import`；Android `ApiClient.postMultipart`。

- [ ] **Step 1: 超管导入 UI**

文件选择用 ActivityResultContracts.GetContent("application/json")；读文本后以 `{targetUsername, bills: parsed}` postBody。仅 session.isAdmin 时渲染入口。

- [ ] **Step 2: 编译 + 构建验证**

Run: `cd android && ./gradlew :app:assembleDebug -q`
Expected: BUILD SUCCESS

- [ ] **Step 3: 手工联调清单**

在 dev 环境跑通：记账 → 账单出现；统计月切换；加好友（两账号）→ 对方通过 → 查看对方账单；重复导入同一文件 → inserted=0。结果记入 `docs/bohe-migration-notes.md`。

- [ ] **Step 4: Commit**

```bash
git add android/app/src/main/java/com/cyz/wxyd/ui/bohe android/app/src/main/java/com/cyz/wxyd/ui/profile
git commit -m "feat(android): 超管导入入口与联调收尾"
```
