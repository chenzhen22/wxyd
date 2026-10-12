# 菜单与卡片模块化实施计划

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** H5/桌面菜单注册表与权限全入库，前端菜单和卡片由接口渲染，每菜单一个 hash URL。

**Architecture:** 新表 `menu_item`（menu_key+platform 唯一）承载菜单注册；现有 `menu_config` 继续承载四态权限；`DataInitializer` 幂等建表+seed+存量迁移；`MenuServiceImpl` 删硬编码改读库；两端前端删硬编码菜单/卡片，改 `menu/list` 渲染 + hash 路由。

**Tech Stack:** Spring Boot 2.2.5 / MyBatis / jQuery 3.7.1（无构建工具，直接改静态文件）

**Worktree:** `/apps/GIT/wxyd/.worktrees/menu-modular`（分支 `feature/menu-modular`）。以下所有相对路径基于 worktree 根。设计文档：`docs/plans/2026-10-12-menu-modular-design.md`。

**测试说明:** 本项目无测试源码（见 CODEBUDDY.md），验证方式为：每任务后 `mvn -pl app -am clean package -DskipTests -q` 编译通过 + diff 走查，最终任务做功能走查清单。

---

### Task 1: SQL 迁移文件 V13__menu_item.sql

**Files:**
- Create: `app/src/main/resources/sql/V13__menu_item.sql`

**Step 1:** 写文件（建表 + 参照 seed，真实 seed 由 DataInitializer 幂等执行，SQL 文件供手工部署参考）：

```sql
-- 菜单注册表（H5 与桌面按平台各一行，同一功能互不共享配置）
-- 权限仍由 menu_config 承载（visible × user_ids 四态），其 menu_key 指向 (menu_key, platform) 行。
CREATE TABLE IF NOT EXISTS `menu_item` (
  `id`         BIGINT NOT NULL AUTO_INCREMENT,
  `menu_key`   VARCHAR(50) NOT NULL COMMENT '前端模块名/路由名',
  `platform`   VARCHAR(10) NOT NULL COMMENT 'h5 | desktop',
  `name`       VARCHAR(50) NOT NULL COMMENT '显示名',
  `icon`       VARCHAR(20) NOT NULL DEFAULT '' COMMENT 'emoji 图标',
  `hash_url`   VARCHAR(100) NOT NULL DEFAULT '' COMMENT 'hash 路由如 #/welfare；空=固定tab入口',
  `sort_no`    INT NOT NULL DEFAULT 0,
  `admin_only` TINYINT NOT NULL DEFAULT 0 COMMENT '1=仅管理员可见',
  `fixed`      TINYINT NOT NULL DEFAULT 0 COMMENT '1=结构性入口，菜单管理页禁止删除',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_key_platform` (`menu_key`, `platform`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='菜单注册表';
-- seed 数据由 DataInitializer 启动自检幂等补种（项目惯例，参考 V9/V10/V11/V12），
-- 也可参照 AppMenuSeed 常量手工 INSERT。
```

**Step 2:** Commit: `git add -A && git commit -m "feat(menu): V13 menu_item 菜单注册表建表脚本"`

---

### Task 2: Mapper 层（menu_item CRUD + menu_config 删除）

**Files:**
- Modify: `app/src/main/java/com/cyz/mapper/mysqlMapper/MysqlMapper.java`（`// ===== menu_config =====` 段后追加）
- Modify: `app/src/main/resources/mapper/MysqlMapper.xml`（`<!-- ===== menu_config ===== -->` 段后追加）

**Step 1:** MysqlMapper.java 追加：

```java
	// ===== menu_item =====
	List<Map<String, Object>> listMenuItems();

	int insertMenuItem(@Param("menuKey") String menuKey, @Param("platform") String platform,
			@Param("name") String name, @Param("icon") String icon, @Param("hashUrl") String hashUrl,
			@Param("sortNo") Integer sortNo, @Param("adminOnly") Integer adminOnly, @Param("fixed") Integer fixed);

	int updateMenuItem(@Param("menuKey") String menuKey, @Param("platform") String platform,
			@Param("name") String name, @Param("icon") String icon, @Param("hashUrl") String hashUrl,
			@Param("sortNo") Integer sortNo, @Param("adminOnly") Integer adminOnly);

	int deleteMenuItem(@Param("menuKey") String menuKey, @Param("platform") String platform);

	int deleteMenuConfig(@Param("menuKey") String menuKey);
```

**Step 2:** MysqlMapper.xml 追加：

```xml
	<!-- ===== menu_item ===== -->
	<select id="listMenuItems" resultType="java.util.HashMap">
		select `menu_key` as menuKey, `platform`, `name`, `icon`, `hash_url` as hashUrl,
		       `sort_no` as sortNo, `admin_only` as adminOnly, `fixed`
		from `menu_item`
	</select>

	<insert id="insertMenuItem">
		insert into `menu_item` (`menu_key`,`platform`,`name`,`icon`,`hash_url`,`sort_no`,`admin_only`,`fixed`)
		values (#{menuKey},#{platform},#{name},#{icon},#{hashUrl},#{sortNo},#{adminOnly},#{fixed})
	</insert>

	<update id="updateMenuItem">
		update `menu_item` set `name`=#{name}, `icon`=#{icon}, `hash_url`=#{hashUrl},
		       `sort_no`=#{sortNo}, `admin_only`=#{adminOnly}
		where `menu_key`=#{menuKey} and `platform`=#{platform}
	</update>

	<delete id="deleteMenuItem">
		delete from `menu_item` where `menu_key`=#{menuKey} and `platform`=#{platform}
	</delete>

	<delete id="deleteMenuConfig">
		delete from `menu_config` where `menu_key`=#{menuKey}
	</delete>
```

**Step 3:** 编译：`mvn -pl app -am clean package -DskipTests -q`，期望无输出（成功）。

**Step 4:** Commit: `feat(menu): menu_item Mapper 接口与 SQL`

---

### Task 3: DataInitializer 建表 + seed + 存量迁移

**Files:**
- Modify: `app/src/main/java/com/cyz/config/DataInitializer.java`

**Step 1:** `run()` 中 `ensureRadarPatrolTables();` 之后追加 `ensureMenuItems();`。

**Step 2:** 新增方法（关键：seed 清单 = 现 api.html 侧栏 + h5.js MODULE_ITEMS 的完整对照；key 映射 = 存量 menu_config 是两端共用桌面 key 存的）：

```java
    /** 菜单 seed：{menuKey, platform, name, icon, hashUrl, sortNo, adminOnly, fixed} */
    private static final Object[][] MENU_SEED = {
            // ---- desktop（顺序 = 现 api.html 侧栏顺序，sort_no 步进 10）----
            {"userManage",  "desktop", "用户管理",     "👤", "#/userManage",  10, 0, 0},
            {"messageBoard","desktop", "留言板",       "💬", "#/messageBoard",20, 0, 0},
            {"robotManage", "desktop", "钉钉机器人",   "🤖", "#/robotManage", 30, 0, 0},
            {"groupChat",   "desktop", "群聊",         "👥", "#/groupChat",   40, 0, 0},
            {"javaApi",     "desktop", "Java8-API",   "☕", "#/javaApi",     50, 0, 0},
            {"shellScript", "desktop", "Shell脚本",    "🐚", "#/shellScript", 60, 0, 0},
            {"note",        "desktop", "记忆笔记",     "📝", "#/note",        70, 0, 0},
            {"shareFile",   "desktop", "文件共享",     "📁", "#/shareFile",   80, 0, 0},
            {"pdfTool",     "desktop", "证据材料整理", "📑", "#/pdfTool",     90, 0, 0},
            {"sqlTool",     "desktop", "SQL 查询",    "🗄️", "#/sqlTool",    100, 1, 0},
            {"dubboCall",   "desktop", "Dubbo调用",   "🔌", "#/dubboCall",  110, 0, 0},
            {"archiveTool", "desktop", "归档下载",     "📦", "#/archiveTool",120, 0, 0},
            {"welfare",     "desktop", "福利专区",     "🎁", "#/welfare",    130, 0, 0},
            {"patrol",      "desktop", "巡检日报",     "🛡️", "#/patrol",     140, 1, 0},
            {"menuManage",  "desktop", "菜单管理",     "🧩", "#/menuManage", 150, 1, 1},
            // ---- h5（卡片顺序 = 现 h5.js MODULE_ITEMS；message/group 为固定 tab，hashUrl 空）----
            {"message",     "h5", "留言板",       "💬", "",              10, 0, 1},
            {"group",       "h5", "群聊",         "👥", "",              20, 0, 1},
            {"user",        "h5", "用户管理",     "👤", "#/user",       110, 1, 0},
            {"welfare",     "h5", "福利专区",     "🎁", "#/welfare",    120, 0, 0},
            {"patrol",      "h5", "巡检日报",     "🛡️", "#/patrol",     130, 1, 0},
            {"robot",       "h5", "钉钉机器人",   "🤖", "#/robot",      140, 0, 0},
            {"notes",       "h5", "记忆笔记",     "📝", "#/notes",      150, 0, 0},
            {"share",       "h5", "文件共享",     "📁", "#/share",      160, 0, 0},
            {"shell",       "h5", "Shell脚本",    "🐚", "#/shell",      170, 0, 0},
            {"javaapi",     "h5", "Java API",   "☕", "#/javaapi",    180, 0, 0},
            {"sql",         "h5", "SQL查询",     "🗄️", "#/sql",        190, 1, 0},
            {"dubbo",       "h5", "Dubbo调用",   "🔌", "#/dubbo",      200, 0, 0},
            {"archive",     "h5", "归档下载",     "📦", "#/archive",    210, 0, 0},
            {"pdfTool",     "h5", "证据材料整理", "📑", "#/pdfTool",    220, 0, 0},
            {"menuManage",  "h5", "菜单管理",     "🧩", "#/menuManage", 230, 1, 1},
    };

    /** 存量 menu_config 迁移映射：旧桌面 key → H5 key（历史上两端共用一份配置按桌面 key 存储） */
    private static final String[][] MENU_KEY_MIGRATION = {
            {"userManage", "user"}, {"messageBoard", "message"}, {"robotManage", "robot"},
            {"groupChat", "group"}, {"javaApi", "javaapi"}, {"shellScript", "shell"},
            {"note", "notes"}, {"shareFile", "share"}, {"sqlTool", "sql"},
            {"dubboCall", "dubbo"}, {"archiveTool", "archive"},
            {"welfare", "welfare"}, {"patrol", "patrol"}, {"pdfTool", "pdfTool"},
            {"menuManage", "menuManage"},
    };

    /**
     * 幂等确保 menu_item 表存在并 seed 全量菜单（对应 sql/V13__menu_item.sql）。
     * 另将存量 menu_config（历史按桌面 key 存储）复制一份到对应 H5 key（仅当 H5 key 无配置时）。
     */
    private void ensureMenuItems() {
        try {
            Integer cnt = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM information_schema.TABLES " +
                            "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME='menu_item'", Integer.class);
            if (cnt == null || cnt == 0) {
                jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS `menu_item` (" +
                        "`id` BIGINT NOT NULL AUTO_INCREMENT," +
                        "`menu_key` VARCHAR(50) NOT NULL COMMENT '前端模块名/路由名'," +
                        "`platform` VARCHAR(10) NOT NULL COMMENT 'h5 | desktop'," +
                        "`name` VARCHAR(50) NOT NULL COMMENT '显示名'," +
                        "`icon` VARCHAR(20) NOT NULL DEFAULT '' COMMENT 'emoji 图标'," +
                        "`hash_url` VARCHAR(100) NOT NULL DEFAULT '' COMMENT 'hash 路由；空=固定tab入口'," +
                        "`sort_no` INT NOT NULL DEFAULT 0," +
                        "`admin_only` TINYINT NOT NULL DEFAULT 0 COMMENT '1=仅管理员可见'," +
                        "`fixed` TINYINT NOT NULL DEFAULT 0 COMMENT '1=结构性入口，禁止删除'," +
                        "`create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP," +
                        "PRIMARY KEY (`id`)," +
                        "UNIQUE KEY `uk_key_platform` (`menu_key`,`platform`)" +
                        ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='菜单注册表'");
                log.info("[INIT] 已自动执行 V13 迁移：创建 menu_item 菜单注册表");
            }
            // 幂等 seed：逐条 insert ignore（存在即跳过，不覆盖运营已改名/排序的配置）
            for (Object[] m : MENU_SEED) {
                jdbcTemplate.update("INSERT IGNORE INTO `menu_item` " +
                        "(`menu_key`,`platform`,`name`,`icon`,`hash_url`,`sort_no`,`admin_only`,`fixed`) " +
                        "VALUES (?,?,?,?,?,?,?,?)", m[0], m[1], m[2], m[3], m[4], m[5], m[6], m[7]);
            }
            // 存量 menu_config 迁移：旧桌面 key 配置 → H5 key（仅当目标 key 无配置时复制）
            Map<String, Map<String, Object>> cfg = new HashMap<>();
            for (Map<String, Object> row : mysqlMapper.listMenuConfigs()) {
                cfg.put(String.valueOf(row.get("menuKey")), row);
            }
            int copied = 0;
            for (String[] pair : MENU_KEY_MIGRATION) {
                Map<String, Object> src = cfg.get(pair[0]);
                if (src == null || cfg.containsKey(pair[1])) continue;
                mysqlMapper.upsertMenuConfig(pair[1],
                        ((Number) src.get("visible")).intValue(),
                        String.valueOf(src.get("userIds")));
                copied++;
            }
            if (copied > 0) log.info("[INIT] 已迁移 {} 条 menu_config 到 H5 key", copied);
        } catch (Exception e) {
            log.error("[INIT] menu_item 建表/seed 失败（请手动执行 sql/V13__menu_item.sql）：{}", e.getMessage());
        }
    }
```

注意：DataInitializer 需补 `import java.util.HashMap; import java.util.Map;`。

**Step 3:** 编译通过后 Commit: `feat(menu): DataInitializer 幂等建表+seed+存量权限迁移`

---

### Task 4: MenuService 重写（删硬编码注册表）

**Files:**
- Modify: `app/src/main/java/com/cyz/service/MenuService.java`
- Modify: `app/src/main/java/com/cyz/service/MenuServiceImpl.java`

**Step 1:** MenuService 接口改为：

```java
public interface MenuService {

    /** 当前用户在某平台可见的菜单列表（已合并四态权限，按 sortNo 排序） */
    Result list(String platform, Long userId, boolean isAdmin);

    /** 全部菜单及权限配置（仅管理员，菜单管理页用） */
    Result allConfigs(String platform);

    /** 保存某菜单的可见性配置（仅管理员） */
    Result save(Long userId, String platform, String menuKey, Integer visible, String userIds);

    /** 新增/编辑菜单项（仅管理员；存在则更新，否则插入） */
    Result saveItem(Long userId, String platform, String menuKey, String name,
                    String icon, String hashUrl, Integer sortNo, Integer adminOnly);

    /** 删除菜单项（仅管理员；fixed=1 禁止；连带删权限配置） */
    Result deleteItem(Long userId, String platform, String menuKey);
}
```

**Step 2:** MenuServiceImpl 重写要点（完整替换）：

- 删除 `REGISTRY`、`ADMIN_ONLY` 两个静态字段及 static 块。
- 私有方法 `List<Map<String,Object>> loadItems(String platform)`：`mysqlMapper.listMenuItems()` 过滤 platform。
- `list()`：admin 直接返回全部行（转 `{key,name,icon,hashUrl,adminOnly,sortNo}`）；非管理员先剔除 `adminOnly==1`，再按 `loadConfigMap()`（复用现有方法）做四态判断（算法照抄旧 visibleKeys 的白/黑名单逻辑）。
- `allConfigs(platform)`：REGISTRY 遍历改为 loadItems 遍历，每行附 visible/userIds（逻辑同旧）。
- `save(...)`：`REGISTRY.containsKey(menuKey)` 校验改为 `existsItem(platform, menuKey)`；platform 参数透传。
- `saveItem(...)`：校验 menuKey 匹配 `[a-zA-Z0-9_]{1,50}`、name 非空≤50、icon≤20、hashUrl 以 `#/` 开头或空、sortNo 0~9999、adminOnly 0/1；存在则 updateMenuItem 否则 insertMenuItem（fixed=0）。
- `deleteItem(...)`：查行不存在报参数错；`fixed==1` 返回 ERROR000015；先 `deleteMenuConfig` 再 `deleteMenuItem`。
- 保留 `loadConfigMap/parseIds/normalizeIds` 私有方法原样。

**Step 3:** 编译 + Commit: `feat(menu): 菜单注册表读库，四态权限按平台独立，支持菜单项增改删`

---

### Task 5: MenuController 新接口

**Files:**
- Modify: `app/src/main/java/com/cyz/controller/MenuController.java`

**Step 1:** 改动：

- `menu/all` 加 `@RequestParam(defaultValue = "desktop") String platform`，透传 `menuService.allConfigs(platform)`；platform 白名单校验（h5/desktop）不合法返回 ERROR000015。
- `menu/visible` 保留（内部 service 已改为读库实现，desktop key 语义不变）——在 MenuServiceImpl 里为兼容保留一个 `visibleKeys` 默认实现调 `list("desktop",...)` 取 key 集合，或直接保留旧逻辑读库版；标记 `@Deprecated` 注释。
- 新增：

```java
    /** 当前用户在某平台可见的菜单列表（前端渲染菜单/卡片用） */
    @GetMapping("menu/list")
    public Result list(@RequestParam String platform, HttpSession session) {
        Result result = Result.getInstance();
        if (!"h5".equals(platform) && !"desktop".equals(platform)) {
            return result.setErrorEnum(ErrorEnum.ERROR000015);
        }
        Long userId = (Long) session.getAttribute("userId");
        Integer role = (Integer) session.getAttribute("role");
        return menuService.list(platform, userId, role != null && role == 0);
    }

    /** 新增/编辑菜单项（仅管理员） */
    @PostMapping("menu/item/save")
    public Result saveItem(@RequestBody(required = false) Result req, HttpSession session) { ... isAdmin 校验同 menu/save ... }

    /** 删除菜单项（仅管理员） */
    @PostMapping("menu/item/delete")
    public Result deleteItem(@RequestBody(required = false) Result req, HttpSession session) { ... }
```

`menu/save` 增加 `platform` 参数（body 里取，缺省 desktop），透传 service。

**Step 2:** 编译 + Commit: `feat(menu): menu/list、menu/item/save、menu/item/delete 接口`

---

### Task 6: H5 前端（hash 路由 + 卡片接口化）

**Files:**
- Modify: `app/src/main/resources/static/h5.html`（版本号 v+1：h5.js）
- Modify: `app/src/main/resources/static/Assets/js/h5.js`

**Step 1:** h5.js 改动：

1. `H5State` 增加 `menus: []`（menu/list 原始数组）与派生 `menuKeys`；删除 `MODULE_ITEMS` 数组（约 263-277 行）。
2. `menuVisible(key)` 改为 `H5State.menuKeys.indexOf(key) !== -1`（空数组时默认放行 home）。
3. `bootstrap()` 中 `menu/visible` 改为：

```javascript
api({ url: 'menu/list?platform=h5', type: 'GET' }).always(function (mv) {
    var menus = (mv && mv.errorCode === '000000' && mv.body) || [];
    H5State.menus = menus;
    H5State.menuKeys = menus.map(function (m) { return m.key; });
    /* 底部 tab 按配置显隐 */
    ['message', 'group'].forEach(function (t) {
        $('#h5Tabbar .h5-tab[data-target="' + t + '"]').toggle(menuVisible(t));
    });
    /* 分享链接优先 */
    var share = getShareWelfareId();
    if (share && window.H5Welfare) { goPage('welfare'); window.H5Welfare.openShared(share); }
    else { goPage(currentHashKey() || 'home'); }
});
```

4. `renderHome()` 改为数据驱动：

```javascript
function renderHome() {
    var items = H5State.menus.filter(function (m) {
        return m.hashUrl && menuVisible(m.key);   /* hashUrl 空 = tab 入口，不进卡片 */
    });
    /* 头部问候 + h5-grid 渲染，ico/label/admin 角标全部来自 m.icon/m.name/m.adminOnly */
    /* 卡片点击：goPage($(this).data('mod'))，data-mod 取 m.key */
}
```

5. hash 路由（新增约 20 行）：

```javascript
function currentHashKey() {
    var h = (location.hash || '').replace(/^#\/?/, '');
    return h || null;
}
window.addEventListener('hashchange', function () {
    var k = currentHashKey();
    if (k && k !== H5State.currentPage) goPage(k);
});
```

6. `goPage(name)` 开头记录 `H5State.currentPage = name;`，校验通过后 `if (('#/' + name) !== location.hash) location.hash = '#/' + name;`（隐藏模块回首页的逻辑保留，注意避免 hashchange 死循环——hash 已相同时不再触发）。

**Step 2:** 走查无 `MODULE_ITEMS` 残留；`?welfare=` 分享逻辑未动。

**Step 3:** Commit: `feat(h5): 菜单卡片接口化 + hash 路由`

---

### Task 7: 桌面前端（侧栏接口化 + hash 路由）

**Files:**
- Modify: `app/src/main/resources/static/api.html`（110-126 行 `<ul class="menu-list">…</ul>` 整段替换为 `<ul class="menu-list" id="menuList"></ul>`；版本号引用无需改，无引入新 js）
- Modify: `app/src/main/resources/static/Assets/js/index.js`

**Step 1:** index.js 改动：

1. 76-121 行 `.menu-item` click 绑定改为**事件委托**（因为菜单是动态渲染的）：

```javascript
$("#menuList").on("click", ".menu-item", function () { showPage($(this).data("target")); });
```

2. 新增 `showPage(target)` 函数 = 原 click 回调体（module-box 显隐 + 各懒加载 if 分支原样搬入）+ 末尾 `if (('#/' + target) !== location.hash) location.hash = '#/' + target;`。
3. 314-339 行替换：删 `#menuSqlTool/#menuMenuManage` show 逻辑与 `applyMenuVisibility`，改为：

```javascript
$.get("user/info", function (res) {
    let info = (res && res.body) || {};
    if (info.isAdmin && window.initSqlTool) window.initSqlTool();
    $.get("menu/list?platform=desktop", function (r) {
        let menus = (r && r.errorCode === '000000' && r.body) || [];
        renderMenu(menus);
        /* hash 直达：可见才进，否则 welcome */
        let k = currentHashKey();
        if (k && menus.some(function (m) { return m.key === k; })) showPage(k);
    }, "json").fail(function () { renderMenu([]); });
}, "json");

function renderMenu(menus) {
    let html = '<li class="menu-item active" data-target="welcome"><i>🏠</i>首页</li>';
    menus.forEach(function (m) {
        html += '<li class="menu-item" data-target="' + m.key + '"><i>' + (m.icon || '•') + '</i>' + escName(m.name) + '</li>';
    });
    $("#menuList").html(html);
}
function currentHashKey() { return (location.hash || '').replace(/^#\/?/, '') || null; }
window.addEventListener('hashchange', function () {
    let k = currentHashKey();
    if (k && k !== currentPage) showPage(k);
});
```

（`escName` 用 index.js 现有 esc 逻辑或内联转义；`currentPage` 记录当前 target 防重复触发。）

4. 删除 123-128 行 `$("#welcome").click(...)` 中 `.children().eq(3)` 硬编码定位（改为 `$('#menuList [data-target="messageBoard"]').addClass("active")` 或等价）。
5. sqlTool 的 `initSqlTool` 调用保留（管理员语义由服务端 adminOnly 保障，前端 isAdmin 判断仅保留 initSqlTool 这一处）。

**Step 2:** 走查：api.html 无硬编码 menu-item；index.js 无 `menu/visible` 引用。

**Step 3:** Commit: `feat(desktop): 侧栏接口化 + hash 路由`

---

### Task 8: 菜单管理页双平台升级

**Files:**
- Modify: `app/src/main/resources/static/Assets/js/menu-manage.js`

**Step 1:** 改动要点：

1. `state` 增加 `platform: 'desktop'`；`load()` 改为 `$.getJSON('menu/all?platform=' + state.platform, ...)`。
2. 桌面 `mount()` 顶部加平台 Tab（两个按钮 desktop/h5，点击切 `state.platform` 后重载重绘）。
3. 表格增加列：图标（input 宽 3em）、名称（input）、排序（input number）、URL（input，只读展示即可）、操作列加「编辑」「删除」按钮；表格上方加「＋ 新增菜单」按钮。
4. 编辑/新增用行内展开或弹层（H5 用 openSheet，桌面用简单 prompt 风格 modal——复用现有样式，保持最小实现）：字段 name/icon/hashUrl/sortNo/adminOnly，提交 `menu/item/save`，body：`{body: {platform, menuKey, name, icon, hashUrl, sortNo, adminOnly}}`（编辑已有项时 menuKey 只读）。
5. 删除：`confirm('确认删除菜单「xx」？')` 后 POST `menu/item/delete`，body `{body:{platform, menuKey}}`；`fixed==1` 行不渲染删除按钮（后端也兜底）。
6. H5 版 `renderH5` 同样加平台切换与编辑入口（复用 openSheet 编辑器）。
7. 保存权限 `menu/save` body 增加 `platform: state.platform`。

**Step 2:** Commit: `feat(menu-manage): 双平台菜单项管理与权限配置`

---

### Task 9: 全量验证与收尾

**Step 1:** 编译：`mvn -pl app -am clean package -DskipTests -q` 通过。

**Step 2:** 残留走查（Grep 断言零命中）：

- `REGISTRY|ADMIN_ONLY` → 仅允许出现在注释或已删除
- `MODULE_ITEMS` → 0 命中
- `menu/visible` → 仅 MenuController 保留一处（@Deprecated）
- api.html `class="menu-item"` 静态项 → 0 命中

**Step 3:** 功能走查清单（需真机/浏览器时列出，代码层面先走查逻辑）：

- [ ] admin / 普通用户 / 白名单 / 黑名单 四种会话 `menu/list` 返回正确
- [ ] `#/welfare` 直达、隐藏菜单 hash 回首页、后退/刷新正常
- [ ] `?welfare=id` 分享链接（含免登录访客）不受影响
- [ ] menu-manage 双平台增改删、fixed 行无删除按钮
- [ ] menu/list 挂掉时两端降级不白屏（H5 home 空、桌面 welcome 正常）

**Step 4:** Commit 收尾 + 汇报（合并/PR 由用户决定）。
