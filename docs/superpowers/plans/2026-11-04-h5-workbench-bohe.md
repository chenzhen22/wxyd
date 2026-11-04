# H5 工作台 + 薄荷账 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** H5 底部导航改为 工作台/薄荷账/我的 三 tab，留言板/群聊移入工作台网格，新建薄荷账 H5 三页面（账单/统计/关注），照 bookkeeping 小程序样式移植，复用现有 `bohe/*` 后端接口。

**Architecture:** 纯前端 + 菜单种子改动。新文件 `h5-bohe.js` 通过 `registerH5Module` 注册三个页面（bohe/boheStats/boheFriends），hash 路由由既有 `goPage` 承担；留言板/群聊改为带 `hashUrl` 的普通菜单项，`renderHome` 现有过滤逻辑自然将其渲染为工作台卡片。后端 Java 零改动。

**Tech Stack:** jQuery 3.7.1 + h5.css（CSS 变量主题），Spring Boot `DataInitializer` 菜单 seed（幂等 upsert）。

**Spec:** `docs/superpowers/specs/2026-11-04-h5-workbench-bohe-design.md`

## Global Constraints

- Java 8 / Spring Boot 2.7.18；后端不改，`mvn -pl app -am clean package -DskipTests` 必须通过。
- 前端无构建步骤、无测试框架：验证 = `node --check <js>` 语法检查 + 浏览器手动验证由用户做。
- 金额单位：后端为**整数分**；显示时 `分/100` 保留两位小数，输入时乘 100。
- 分类固定 8 类（key→名）：food 餐饮 / transport 交通 / shop 购物 / house 居住 / fun 娱乐 / medical 医疗 / study 教育 / other 其他（与 Android `BoheCategories` 对齐）。
- 统一响应：`res.errorCode === '000000'` 为成功，否则 `toast(res.errorMsg)`。
- 接口：GET `bohe/bill/list?month=yyyy-MM[&period=][&friendId=]`；POST `bohe/bill/save` `{id?,amount(分),category,note,date}`；POST `bohe/bill/delete` `{id}`；GET `bohe/stats?month=&period=day|month|year&friendId=`；GET `bohe/friend/list`；POST `bohe/friend/request|handle|remove`；GET `bohe/user/search?keyword=`；GET/POST `bohe/visibility`。
- 静态资源缓存：改动的 css/js 引用处 `?v=` 递增（h5.css → v=9；h5.js → v=14；h5-bohe.js 新建 v=1）。
- 所有动态文案入 HTML 前过 `esc()`（h5.js 已提供）。
- commit 信息结尾带 `Co-Authored-By: Claude Code <noreply@anthropic.com>`。

## Review Focus

- 好友查看模式：进入 `boheFriends` 点好友看账单后，账单页必须隐藏"记一笔/删除"且显示"xx的账单 + 切回我"；切回我后恢复可写。（Task 3 步骤中实现，Task 4 联动验证）
- 菜单显隐四态：管理员把 `message`/`group` 配置为隐藏时，工作台卡片必须消失，hash 直达被 `goPage` 的 `menuVisible` 兜底拦回工作台。
- 金额浮点：输入 `12.34` 存 1234 分；显示 `1234` 分渲染 `12.34`；不允许出现 `12.3400000001` 之类浮点尾数（用 `Math.round`）。
- 老用户升级：`menu_item` 表已有 message/group 行（hashUrl 空），seed 必须用 upsert 更新而非跳过（`DataInitializer` 现有 seed 机制本身幂等，验证即可）。
- 空 data-theme：h5.css 变量在 light/dark 两主题下都要可读（沿用既有变量，不写死颜色）。

---

### Task 1: 导航改版（tabbar 3 tab + 菜单种子 + 工作台卡片）

**Files:**
- Modify: `app/src/main/resources/static/h5.html:92-97`（tabbar）、`:154`（h5.js v=14）
- Modify: `app/src/main/java/com/cyz/config/DataInitializer.java`（MENU_SEED h5 段）
- Modify: `app/src/main/resources/static/Assets/js/h5.js`（menuVisible 不需改；确认 `PAGE_RENDER.message/group` 已存在）

**Interfaces:**
- Produces: hash `#/message`、`#/group` 可直达（页面渲染已存在，无需新代码）；`#/bohe` 路由给 Task 2 使用（本任务先在 tabbar 放薄荷账 tab，点击后 `#page-bohe` 已在 h5.html 预置但渲染函数 Task 2 才有）。
- 依赖：`DataInitializer` 只在首次播种时执行 seed（看现有代码确认；若 seed 每次启动 upsert 则老库自动生效；若仅首启，则说明 h5 段 seed 行为并在验证步骤给出手工 SQL）。

- [ ] **Step 1: 改 MENU_SEED h5 段**

在 `MENU_SEED` h5 段：`{"message","h5",...}` 行 hashUrl 由 `""` 改 `"#/message"`、sortNo 由 `10` 改 `15`；`{"group","h5",...}` 行 hashUrl 由 `""` 改 `"#/group"`、sortNo 由 `20` 改 `25`；新增三行（放在 patrol 之前、sortNo 140 段附近避免撞号，用 145/146/147 之间未被占用的值——实际以文件内现存 sortNo 为准，message=15/group=25/bohe=18/boheStats=19/boheFriends=20 均空闲）：

```java
{"bohe", "h5", "薄荷账", "💰", "#/bohe", "18", "0", "1"},
{"boheStats", "h5", "薄荷统计", "📊", "#/boheStats", "19", "0", "0"},
{"boheFriends", "h5", "薄荷关注", "🤝", "#/boheFriends", "20", "0", "0"},
```

先读 `DataInitializer` 确认 seed 执行时机（仅首启 or 每次启动 upsert）。若仅首启播种：同时在验证步骤提供手工 UPDATE SQL（更新 message/group 两行 hashUrl + INSERT 三行），写进 `app/src/main/resources/sql/V31__h5_workbench_menu.sql`。

- [ ] **Step 2: 改 h5.html tabbar**

```html
<nav class="h5-tabbar" id="h5Tabbar">
    <div class="h5-tab active" data-target="home"><span class="h5-tab-ico">🏠</span>工作台</div>
    <div class="h5-tab" data-target="bohe"><span class="h5-tab-ico">💰</span>薄荷账</div>
    <div class="h5-tab" data-target="profile"><span class="h5-tab-ico">👤</span>我的</div>
</nav>
```

同时在 `<main>` 内新增三个 section（放在 `page-profile` 之前）：`<section class="h5-page" id="page-bohe"></section>`、`id="page-boheStats"`、`id="page-boheFriends"`。h5.js 引用升 `?v=14`。

- [ ] **Step 3: h5.js 兼容**

检查 `menuVisible`（`h5.js:30`）——`bohe` 不在硬编码白名单，走 `visibleKeys`：seed 中 `fixed=1` 的 bohe 默认入库即可见，但**管理员通过菜单管理隐藏后要能生效**，现有逻辑已覆盖，无需改。确认 `PAGE_RENDER` 中 `message: renderMessage`、`group`（group 页由 `group-chat.js` 注册，检查其注册 key 为 `group`）。无需改代码则此步仅为确认。

- [ ] **Step 4: 验证**

Run: `node --check app/src/main/resources/static/Assets/js/h5.js && mvn -pl app -am clean package -DskipTests -q`
Expected: 两个命令均无错误。

- [ ] **Step 5: Commit**

```bash
git add app/src/main/resources/static/h5.html app/src/main/java/com/cyz/config/DataInitializer.java app/src/main/resources/sql/V31__h5_workbench_menu.sql
git commit -m "feat(h5): 底部导航改版——首页改工作台，留言板/群聊移入工作台，新增薄荷账 tab"
```

---

### Task 2: 薄荷账主页（bohe）：月度总览 + 账单列表 + 记一笔

**Files:**
- Create: `app/src/main/resources/static/Assets/js/h5-bohe.js`
- Modify: `app/src/main/resources/static/h5.html`（引入 `h5-bohe.js?v=1`，放在 h5.js 之后）
- Modify: `app/src/main/resources/static/Assets/css/h5.css`（追加薄荷账样式段，h5.html 引用升 v=9）

**Interfaces:**
- Consumes: `registerH5Module(name, fn)`（h5.js:8）；`api/postJSON/toast/esc/openSheet/closeSheet/loading/confirm`（h5.js 全局函数）；`H5State.username` 判空由 `goPage` 负责。
- Produces: `registerH5Module('bohe', renderBohe)`；全局 `window.BoheViewing = { friendId: null, friendName: null }`（Task 3/4 读写，控制好友查看模式）；`window.BOHE_CATEGORIES`（8 类数组 `[{key,label}]`）。

- [ ] **Step 1: 写 h5-bohe.js 骨架 + 分类表 + 金额工具**

```js
/* 薄荷账 H5（照 bookkeeping 小程序移植）；金额：后端整数分 */
window.BOHE_CATEGORIES = [
    {key:'food',label:'餐饮',icon:'🍜'},{key:'transport',label:'交通',icon:'🚌'},
    {key:'shop',label:'购物',icon:'🛍️'},{key:'house',label:'居住',icon:'🏠'},
    {key:'fun',label:'娱乐',icon:'🎮'},{key:'medical',label:'医疗',icon:'💊'},
    {key:'study',label:'教育',icon:'📚'},{key:'other',label:'其他',icon:'📦'}
];
function boheYuan(fen){ return (Math.round(Number(fen||0))/100).toFixed(2); }
function boheFen(yuanStr){ return Math.round(parseFloat(yuanStr||'0')*100); }
function boheCatLabel(key){ var c=BOHE_CATEGORIES.find(function(x){return x.key===key;}); return c?c.label:'其他'; }
```

- [ ] **Step 2: renderBohe 主页**

`registerH5Module('bohe', renderBohe)`。结构（从上到下）：
1. 顶部总览卡：当前月（yyyy-MM）+ 本月支出合计（`bohe/stats?month=` 的 `total`，分→元）+ 上/下月切换箭头。
2. 好友查看条（仅 `BoheViewing.friendId` 非空时）：`正在看 {friendName} 的账单` + 「切回我」按钮（点击置空 BoheViewing 后重渲染）。
3. 入口卡片行：📊 统计 → `goPage('boheStats')`；🤝 关注 → `goPage('boheFriends')`。
4. 账单列表：`bohe/bill/list?month={cur}[&friendId=]` 按 billDate 倒序渲染（后端返回顺序为准），每行：分类 icon + 备注/分类名 + 日期 + 金额（支出显示 `-¥xx.xx`）；本人模式每行带删除按钮（`data-del-id`）。
5. FAB「＋ 记一笔」固定右下（`BoheViewing.friendId` 非空时隐藏，同时调 `setFab(false)` 无关——用页面内自有按钮）。

数据拉取用 `api({url:'bohe/bill/list?...',type:'GET'})` 与 `api({url:'bohe/stats?...',type:'GET'})` 并行（`$.when`）。

- [ ] **Step 3: 记一笔抽屉**

`openSheet` 表单：金额（`<input type="number" step="0.01" inputmode="decimal">`）、8 分类宫格（点击选中，默认 food）、备注（text）、日期（`<input type="date">` 默认今天）。提交 `postJSON('bohe/bill/save', {amount:boheFen(v), category, note, date})`，成功 `toast('已记一笔') + closeSheet() + renderBohe()`。

- [ ] **Step 4: 删除**

列表事件委托：`$('#page-bohe').on('click','[data-del-id]',...)` → `confirm('删除该笔账单？')` → `postJSON('bohe/bill/delete',{id:Number(...)})` → 成功 toast + 重渲染。（委托绑定放 renderBohe 内一次性标记 `data-bound` 防重复。）

- [ ] **Step 5: h5.css 薄荷账样式段 + h5.html 引 script**

追加 `.bohe-` 前缀样式：总览卡（大号金额，`var(--accent)`）、列表行、分类宫格、入口卡片行；全部用 CSS 变量（`--glass-3`、`--text-2` 等），不写死色值。h5.html：`<script src="Assets/js/h5-bohe.js?v=1">`（group-chat.js 之后）；h5.css 引用升 `?v=9`。

- [ ] **Step 6: 验证**

Run: `node --check app/src/main/resources/static/Assets/js/h5-bohe.js && grep -c "h5-bohe.js" app/src/main/resources/static/h5.html`
Expected: 语法通过；h5.html 恰有 1 处引用。

- [ ] **Step 7: Commit**

```bash
git add app/src/main/resources/static/Assets/js/h5-bohe.js app/src/main/resources/static/h5.html app/src/main/resources/static/Assets/css/h5.css
git commit -m "feat(h5): 薄荷账主页——月度总览/账单列表/记一笔/删除"
```

---

### Task 3: 统计页（boheStats）+ 好友查看模式打通

**Files:**
- Modify: `app/src/main/resources/static/Assets/js/h5-bohe.js`
- Modify: `app/src/main/resources/static/Assets/css/h5.css`

**Interfaces:**
- Consumes: `BOHE_CATEGORIES`、`boheYuan`、`BoheViewing`（Task 2）。
- Produces: `registerH5Module('boheStats', renderBoheStats)`；统计页支持 `BoheViewing.friendId` 透传 `&friendId=`，同样显示"xx的统计 + 切回我"。

- [ ] **Step 1: renderBoheStats**

结构：粒度切换（日/月/年三个 chip，默认月）+ 期切换（‹ 上期 / 下期 ›，期值随粒度：`period=day`→`yyyy-MM-dd`、`month`→`yyyy-MM`、`year`→`yyyy`，`bohe/stats?month={期值}&period={key}[&friendId=]`）+ 期支出合计大数字 + 分类占比列表（`byCategory`：分类名 + 金额 + 水平条形图，宽度 = `cat/total*100%`，用 `var(--accent)` 渐变透明度区分排名）。

锚点月存模块级变量 `boheStatsAnchor`（`yyyy-MM-01`），切粒度只改 period 重拉。

- [ ] **Step 2: 切换交互**

chip 与箭头点击 → 更新状态 → `renderBoheStats($('#page-boheStats'))` 重渲染。好友查看条复用 Task 2 的逻辑形态。

- [ ] **Step 3: 验证**

Run: `node --check app/src/main/resources/static/Assets/js/h5-bohe.js`
Expected: 通过。

- [ ] **Step 4: Commit**

```bash
git add app/src/main/resources/static/Assets/js/h5-bohe.js app/src/main/resources/static/Assets/css/h5.css
git commit -m "feat(h5): 薄荷统计页——日/月/年粒度 + 分类占比"
```

---

### Task 4: 关注页（boheFriends）+ 我的页可见开关

**Files:**
- Modify: `app/src/main/resources/static/Assets/js/h5-bohe.js`
- Modify: `app/src/main/resources/static/Assets/css/h5.css`

**Interfaces:**
- Consumes: Task 2/3 的工具与 `BoheViewing`。
- Produces: `registerH5Module('boheFriends', renderBoheFriends)`；点已通过好友 → 置 `BoheViewing={friendId,friendName}` 后 `goPage('bohe')`。

- [ ] **Step 1: renderBoheFriends**

`bohe/friend/list` 渲染三组：**待我处理**（`status=0 && !outgoing`，带 同意/拒绝 按钮 → `postJSON('bohe/friend/handle',{friendId,accept:true|false})`）、**我发起的待通过**（`status=0 && outgoing`，灰显 + 可撤回 → `bohe/friend/remove`）、**已通过**（`status=1`，点击 → `BoheViewing={friendId:item.userId, friendName:label}` + `goPage('bohe')`；长按或右侧 ⋯ 菜单提供 改备注（无后端 alias 接口，跳过）/ 取消关注 → confirm 后 `bohe/friend/remove`）。

label 取 `displayName || username || userId`（与 Android 一致）。

- [ ] **Step 2: 搜索发起关注**

页顶搜索框 + 搜索按钮 → `bohe/user/search?keyword=` → 结果列表（用户名 + 昵称 + 「+ 关注」按钮）→ `postJSON('bohe/friend/request',{friendId})` → toast 结果 + 重渲染。

- [ ] **Step 3: 可见性开关**

页底开关行「允许好友查看我的薄荷账」：`bohe/visibility` GET 初始化，toggle → POST `{hidden: !cur}`。开关样式复用 h5.css 内既有 checkbox/toggle（若无则新增 `.bohe-switch`）。

- [ ] **Step 4: 验证**

Run: `node --check app/src/main/resources/static/Assets/js/h5-bohe.js && mvn -pl app -am clean package -DskipTests -q`
Expected: 通过。

- [ ] **Step 5: Commit**

```bash
git add app/src/main/resources/static/Assets/js/h5-bohe.js app/src/main/resources/static/Assets/css/h5.css
git commit -m "feat(h5): 薄荷关注页——好友审批/搜索关注/可见开关 + 查看好友账单"
```

---

### Task 5: 收尾——菜单管理文案核对 + 全量构建验证

**Files:**
- Modify: 如菜单管理页（`menu-manage.js`）对 fixed 项的说明文案涉及"底部 tab"，需同步措辞（检查即可，大概率无硬编码）。

- [ ] **Step 1: 检查 menu-manage.js / MenuServiceImpl 对 message/group 的特殊处理**

Run: `grep -n "message\|group" app/src/main/resources/static/Assets/js/menu-manage.js`
若有"message/group 是底部 tab 不显示 hashUrl"之类逻辑或文案则改掉；无则此步为确认。

- [ ] **Step 2: 全量构建**

Run: `mvn -pl app -am clean package -DskipTests -q && node --check app/src/main/resources/static/Assets/js/h5-bohe.js && node --check app/src/main/resources/static/Assets/js/h5.js`
Expected: 全部通过。

- [ ] **Step 3: Commit（如有改动）**

```bash
git add -A && git commit -m "chore(h5): 菜单管理文案同步工作台改版"
```
