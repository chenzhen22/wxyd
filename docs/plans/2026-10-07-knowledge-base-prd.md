# PRD：个人知识库（记忆笔记 2.0）

| 项 | 内容 |
|----|------|
| 版本 | v2.0（升级已有 v1「记忆笔记」，非新建模块） |
| 日期 | 2026-10-07（2026-10-08 更新：Q1 全文检索范围定稿） |
| 状态 | Q1/Q2/Q3 全部定稿，Phase 1–3 已实现并通过权限红线验证 |
| 读者 | 研发 Leader / 技术评审 + 管理层立项 + 全角色通用 |
| 一句话定位 | 把 wxyd 里散落在 Java API、Shell、Dubbo 资料页和自己脑子里的经验，收敛成**能搜、能连、能复用**的个人知识资产 |

---

## 0. 现状盘点（评审前必读）

v1「记忆笔记」已经上线并可用，PRD 必须建立在它之上，而不是重复造轮子。

**v1 已具备：**

| 能力 | 实现现状 |
|------|---------|
| 存储 | 文件系统 `wxyd.share.dir`（默认 `/apps/shareFile`）下 `{userId}/{净化标题}.md`，**无数据库表** |
| 元数据 | md 内嵌 YAML frontmatter：`name` / `category` / `visibility`（public、private） |
| 接口 | `GET /note/list`、`GET /note/search?keyword=`、`GET /note/get`、`POST /note/save`、`POST /note/delete`、`POST /note/migrateOld`（超管） |
| 前端 | `note.js`（411 行），双 tab「我的笔记 / 公共搜索」；纯 textarea + 自研 `renderMarkdown`（含 TOC 抽取）；H5 版在 `h5.js` |
| 安全 | `/note/**` 已纳入 `AuthInterceptor`；文件名净化 + canonicalPath 前缀校验 |
| 约束 | 正文 ≤500KB，标题 ≤50 字符，同用户标题唯一，改名=删旧写新 |

**v1 的四个真实缺口（本次要解决的问题）：**

1. **搜不到内容**——`searchPublic` 只匹配**标题**，正文里写了什么完全检索不到。知识库的核心价值是"记得内容、找得到"，标题匹配等于没有。
2. **知识是孤岛**——Java API、Shell、Dubbo 三套资料页（合计数十篇 md）和个人笔记之间**零关联**。"这条 ArrayList 用法记在笔记里"和"这条 API 速查在 data/array-list.md"是两条平行线。
3. **分类靠手填**——`category` 纯文本手打，无联想无规范，同一用户很容易出现"Java"/"java"/"JAVA"三类分裂。
4. **沉淀成本高**——想到一条经验要切页面→新建→填标题→填分类→选公开性→写正文，六步。知识是在"有空闲"时才被写下来的，实际发生率极低。

> **结论**：v2 不是"加个搜索"，而是把"随手记"改造成"**顺手沉淀 + 高效找回**"。

---

## 1. Problem Statement

wxyd 的使用者是平台内的开发者与运维。他们在排障、写脚本、调 Dubbo 接口的过程中产生了大量高价值经验——某条命令的坑、某个 API 的边界、某次故障的根因。但这些经验目前只有两个去处：烂在脑子里，或者散落成孤立的笔记文件。

**不解决的代价：**

- **重复劳动**：同一个问题第二次排查时仍然要从头试一遍，因为"上次好像记录过，但搜不到"。
- **知识无法复用**：值得沉淀的经验沉在个人笔记里，无法被他人检索到，团队知识资产为零。
- **上手成本高**：新成员只能读源码和试错，没有沉淀路径。
- **已有投入浪费**：Java API / Shell / Dubbo 三套精心整理的资料（约 50+ 篇）与个人笔记割裂，两者本该互相跳转。

---

## 2. Goals

| # | 目标 | 衡量方式 |
|---|------|---------|
| G1 | **找回成本降到 10 秒内** | 新增全文检索（含正文），搜索结果 P95 响应 < 500ms |
| G2 | **沉淀成本降到 2 步以内** | 任意资料页支持"一键存入我的笔记"，从看到到存完 ≤2 次点击 |
| G3 | **资料与笔记双向连通** | Java API / Shell / Dubbo 三套资料可被反向链接到个人笔记，形成图谱 |
| G4 | **分类不再分裂** | 分类从自由文本改为受控词表 + 输入联想，重复分类归零 |
| G5 | **知识可共享且可控** | 公开笔记可被他人检索复用，私有笔记绝不泄露；权限边界零缺陷 |

**用户目标**：G1、G3、G4 —— 让存下来的东西真的能被找到、用上。
**业务目标**：G2、G5 —— 让沉淀行为发生得足够频繁，且平台上形成可流通的知识存量。

---

## 3. Non-goals（明确不做）

| 不做什么 | 为什么现在不做 |
|---------|--------------|
| **多人实时协作编辑笔记** | 与已有群聊模块职责重叠；个人知识库的核心矛盾是"找回"，不是"共写"。留作 P2 |
| **全文检索引擎（ES / Lucene）** | 数据量级（个人笔记百篇级）远未到需要外部引擎；引入即增加部署与运维成本。v2 用 MySQL + LIKE 已足够，且留了演进口（见 6.2 存储选型） |
| **附件 / 图片上传** | md 文件存储模型下附件管理成本高、收益低；先用外链 |
| **笔记富文本编辑器（所见即所得）** | 现有 textarea + Markdown 预览已够用。引入第三方编辑器会带来 XSS 面与体积成本 |
| **版本历史 / 差异对比（类似维基）** | 频次低。v2 用"删除前导出"作为兜底，v3 再考虑 |
| **AI 自动摘要 / 自动打标** | 有价值但非核心。先把结构化地基（标签、关联）打好，AI 能力后置可叠加 |
| **公开笔记的点赞 / 评论 / 关注** | 社区化指标尚未验证；先只做"可搜索复用"这一最小闭环 |

> 范围控制原则：**任何新增需求必须对应等量的范围删减或时间线延长**，不接"顺手加个"。

---

## 4. User Stories

### 4.1 核心故事（P0，按优先级排序）

**US-1 全库全文检索**
> 作为**平台用户**，我希望搜索框能同时匹配笔记的标题**和正文内容**，以便我想起"大概提过某个关键词"时也能立刻找到那篇笔记，而不用回忆标题。

- 验收：搜索 `ThreadLocal`，能命中标题为"线程池笔记"但正文含 `ThreadLocal` 的笔记
- 边界：搜索仅覆盖**自己可见**的笔记（自己的全部 + 他人的 public）

**US-2 一键沉淀（收藏夹入笔记）**
> 作为**查阅 Java API / Shell / Dubbo 文档的开发者**，我希望在这些资料页上直接点"存入我的笔记"，以便不用复制粘贴、不用离开当前页面就能把有价值的内容留下来。

- 验收：在 `shellscript.html` 的 curl 条目页点「存入笔记」，自动生成一篇笔记并跳转到笔记编辑页，内容已填充

**US-3 资料 ↔ 笔记互链**
> 作为**阅读笔记的开发者**，我希望笔记里能直接跳到相关的官方资料页；反过来在资料页也能看到"有哪些人写过关于它的笔记"，以便快速建立上下文。

- 验收：笔记 frontmatter 中写 `source: javaapi/array-list` 时，正文顶部渲染出"参考：ArrayList API 速查"的跳转链接

**US-4 受控分类 + 联想**
> 作为**维护自己知识库的开发者**，我希望分类从下拉里选而不是手打，以便分类不会因为大小写或近义词分裂成好几堆。

- 验收：分类输入框改为输入联想，选项来自 `{用户已有分类} ∪ {平台预置分类}`，已选分类可一键清理

**US-5 标签（多值）**
> 作为**有跨领域笔记的用户**，我希望一篇笔记能打多个标签，以便按标签横向聚合——一篇讲"线程池调优"的笔记同时属于 `Java`、`性能`、`故障案例`。

- 验收：笔记详情页展示标签，点击标签可筛选出全部同标签笔记

**US-6 隔离与权限（红线）**
> 作为**笔记属主**，我希望私有笔记绝不出现在任何人的搜索结果里，以便我对"谁能看到我的笔记"有确定预期。

- 验收：用户 A 存私有笔记，用户 B 用任何关键词搜索，**零命中**；B 直接构造 `ownerId` 调用 `/note/get` 返回"不存在"

### 4.2 增强故事（P1）

**US-7 关联笔记（双向链接）**
> 作为**梳理知识网络的开发者**，我希望笔记里能用 `[[标题]]` 引用另一篇笔记，以便形成知识网络而不是孤立节点。

**US-8 最近浏览**
> 作为**中断后回来的用户**，我希望能看到"最近打开的 10 篇"，以便接上刚才的思路。

**US-9 笔记导出**
> 作为**担心数据锁死的用户**，我希望一键把某篇/全部笔记导出成 md 打包下载，以便自己留底。

**US-10 标签/分类管理**
> 作为**整理癖用户**，我希望查看所有标签的使用次数，并能重命名、合并标签。

---

## 5. Requirements

### 5.1 Must-Have（P0，缺一则核心问题未解决）

#### R1. 全文检索（对应 US-1、G1）

**行为定义**
- `GET /note/search?keyword=&scope=mine|public&field=all|title|content`
- `scope` 默认 `all`；`field` 默认 `all`（标题+正文），可限定仅标题
- 匹配规则：**大小写不敏感的子串匹配**，不引入分词器（与用户"记得关键词"的实际心智一致）
- 结果字段：除现有 `name`/`ownerName`/`preview` 外，新增 **`titleMatched`、`contentMatched`、`contentSearchable`** 三个布尔量
  - `contentSearchable=false` 表示该结果来自他人，正文未入库（见下方 Q1 决策），前端应提示"仅按标题匹配"
- 排序：标题命中优先 → 内容命中者按 `updateTime` 倒序
- 命中片段：内容命中时，`preview` 返回**以关键词为中心的 ±40 字上下文**，而非正文开头 150 字

**🔒 Q1 已定稿（2026-10-08，产品决策）**

> **全文检索只对属主生效。** 任何人保存自己的笔记时，正文都会入库
> （否则本人无法全文检索自己写的笔记 —— 这是功能本身的要求）。
> Q1 的保证来自**查询条件**，不是来自"库里没有这份数据"：
> 正文命中条件必须写作 `owner_id = #{currentUserId} AND content_indexed = 1`。

**产品理由**：public 仅代表"可读"，不代表"正文可被他人检索"。真正的边界在检索层 ——
他人无论怎么构造查询，都只能按标题命中，拿不到你的正文。

> ⚠️ **实现纠错（2026-10-08 实测）**：初版实现只在正文匹配条件里判了 `content_indexed`，
> 漏掉 `owner_id` 限定，导致 B 用户用 `field=content` 能搜到 A 的 public 笔记正文，
> **Q1 形同失效**。已修复并在 SQL 注释与验收脚本中固化为红线检查项。
> 这类漏洞查库计数是发现不了的（public 笔记本来就该有正文），**必须用真实检索路径验证**。

**连带影响（必须同步到实现与文案）**：

| 场景 | 行为 |
|------|------|
| 搜**自己**的笔记 | 标题 + 正文全文命中 |
| 搜**他人 public** 笔记 | **仅标题命中**（正文不入库，无法匹配） |
| 搜**他人 private** | 零命中（红线） |

- 前端在 `scope=public` 搜索框旁需有明确文案说明「他人笔记仅支持按标题搜索」，避免用户误以为功能坏了
- 指标口径相应调整：见 7.2「搜索成功率」仅统计本人笔记
- 将来若要放开（产品有异议时），只需重建索引 + 放开查询侧条件，**表结构无需变更**

**技术约束（重要）**
- 当前 v1 的 `searchPublic` 是**遍历所有用户目录逐个读文件**（`listFiles` + 解析 frontmatter）。加全文检索后这是 O(用户数 × 文件数) 的磁盘 IO，必须改为**入库检索**。
- 选型见 6.2。核心要求：MyBatis + MySQL 8 可实现，不引入新中间件。
- 写入侧：`saveNote` 时仅当 `ownerId == 当前登录用户` 才写 `content` 并置 `content_indexed=1`；`rebuildIndex` 重建时按同一规则回填。

**验收标准（Given/When/Then）**

```
Given 用户 A 有笔记《线程池笔记》，正文含 "ThreadLocal" 段，标题不含该词
When  A 在搜索框输入 "ThreadLocal" 并选择 scope=mine
Then  该笔记出现在结果首行，preview 中高亮 "ThreadLocal"
And   contentMatched = true, contentSearchable = true

Given 用户 A 有一篇 public 笔记《性能调优笔记》，正文含 "Arthas" 但标题不含
When  用户 B 在 scope=public 下搜索 "Arthas"
Then  该笔记【不出现在结果中】（正文未入库，仅能按标题匹配 —— Q1 决策）
And   B 改为搜索 "性能调优"（标题词）则能命中该笔记
And   该结果的 contentSearchable = false

Given 用户 A 有私有笔记《薪资数据》，内容含 "机密"
When  用户 B 搜索 "机密"
Then  结果中不包含 A 的任何笔记
And   结果中不包含 A 的《薪资数据》

Given 用户 A 搜索关键词，该关键词在任何可见笔记中都匹配不到
When  A 提交搜索
Then  返回空列表 + 前端展示"未找到相关笔记"，而非报错或 500
```

#### R2. 一键沉淀（对应 US-2、G2）

**行为定义**
- 在 Java API 页、Shell 脚本页、Dubbo 页的**每个条目**旁增加「存入笔记」按钮
- 点击后：`POST /note/saveFromSource`，body 携带 `{ sourceKey, sourceTitle, sourceCategory }`
- 服务端从 classpath 资源（`data/*.md` 或 `data-shell/*.md`）读取该条目的**完整原始内容**，组装为笔记：
  - `name` = `{原条目名}（摘录）` —— 若已存在同名，自动追加序号 `（摘录 2）`
  - `category` = 来源分类（如 `Shell 脚本`）
  - `visibility` = `private`（**默认私有**，沉淀到知识库是个人行为，公开需用户主动切换）
  - `source` = `sourceKey`，写入 frontmatter 供 R3 反查
- 成功后前端跳转到笔记编辑页，用户可继续补充
- 前端要求：统一走**原生 fetch**（与项目既有约定一致 —— jQuery 1.8.3 不便处理 blob，且全局 ajaxError 会抢 401 跳转）

**验收标准**

```
Given 用户在 Shell 页面浏览 "curl" 条目
When  点击「存入笔记」
Then  新建一篇私有笔记，内容为 curl 条目的完整语法+示例
And  source frontmatter = "shell/curl"
And  页面跳转至该笔记的编辑视图

Given 用户已存在《curl（摘录）》
When  再次点击 curl 条目的「存入笔记」
Then  新建《curl（摘录 2）》，原笔记不被覆盖

Given 用户点击「存入笔记」时未登录
Then  跳转登录页，不产生任何笔记文件
```

#### R3. 双向关联（对应 US-3、G3）

**行为定义**
- frontmatter 新增 `source` 字段（单值，指向来源条目 key）
- 正向：笔记详情页顶部渲染「参考：{来源标题}」跳转链接，跳到对应资料页锚点
- 反向：资料页条目底部渲染"关于此条目的笔记（N）"，点击进入笔记列表（`scope=public` 且已按 source 过滤）
- 兼容性：`source` 缺失的老笔记**不报错**，仅不渲染正/反向链接

**验收标准**

```
Given 笔记 frontmatter 含 source: shell/curl
When  打开该笔记
Then  顶部显示「参考：curl」，点击跳转至 Shell 页面 curl 条目位置

Given 老笔记无 source 字段
When  打开该笔记
Then  正常渲染，不显示参考链接，控制台无异常

Given 用户 B 访问 curl 资料条目
When  存在 3 篇 public 笔记引用了它
Then  条目底部显示"关于此条目的笔记（3）"，点击可看到这 3 篇
```

#### R4. 分类词表化（对应 US-4、G4）

**行为定义**
- 平台预置分类（参考 v1 已有分类体系 + 现有三套资料分类）：`Java API`、`Shell 脚本`、`Dubbo`、`SQL`、`故障案例`、`踩坑记录`、`环境配置`、`性能优化`、`其他`
- `GET /note/categories` 返回 `{预置分类} ∪ {该用户已有分类}`，供前端联想
- **不强制**：用户仍可输入新分类，但新分类自动进入个人词表，下次可联想
- 规范化：保存时 trim + 合并同形变体（`Java` / `java` / `JAVA` → 首次出现的写法）

**验收标准**

```
Given 用户输入框已有分类 "Shell脚本" 和新输入 "shell 脚本"
When  保存后查看分类联想列表
Then  只出现一个条目（不产生分裂）

Given 用户输入框当前值为 "Java"
When  联想列表展开
Then  "Java API"（预置）和该用户已建的 "Java"（个人）都可选
```

#### R5. 标签（对应 US-5）

**行为定义**
- frontmatter 新增 `tags` 字段（字符串数组，**可为空**，逗号分隔存储）
- 笔记详情展示标签；点击标签 → 筛选出该用户全部含此标签的笔记
- `GET /note/tags` 返回该用户标签及使用次数
- 标签规范：≤20 字符/个，最多 10 个/篇，不区分大小写去重

#### R6. 权限隔离（对应 US-6，红线，**必须实现**）

- 所有列表/搜索/标签接口**只返回**「当前用户的全部笔记」+「其他用户的 public 笔记」
- 私有笔记在**任何**搜索路径下均不可见（含标签筛选、来源反查、公共搜索）
- `GET /note/get` 读取他人私有笔记返回 `ERROR000018`（不存在），**不返回"无权限"** —— 不泄露"该笔记存在"这一信息
- 文件路径防护沿用 v1：文件名净化 + `canonicalPath` 前缀校验（防止 `../` 穿越）
- 新增的 `saveFromSource` 必须校验 `sourceKey` 合法性，**不得允许任意 classpath 路径**（仅允许匹配 `data/**` 与 `data-shell/**` 下的已知条目 key，否则报参数错误）
- **数据最小化（Q1 决策衍生的红线）**：`note_index.content` 只允许存属主自己的笔记正文。
  他人 public 笔记必须 `content=''` 且 `content_indexed=0` —— 这不只是"不查"，而是**库里根本不该有这份数据**。
  验收时需直接查库确认：`SELECT COUNT(*) FROM note_index WHERE owner_id <> #{当前用户} AND content <> ''` 必须为 **0**。

> ⚠️ **验收红线**：R6 任一条不通过则功能不予上线。

### 5.2 Nice-to-Have（P1，可作为上线后第一迭代）

| 需求 | 说明 | 对应 |
|------|------|------|
| R7 `[[双向链接]]` | 笔记正文引用另一篇笔记，详情页解析为可点击链接 | US-7 |
| R8 最近浏览 | `GET /note/recent`，基于前端 localStorage 记录最近 10 篇 | US-8 |
| R9 导出 | `GET /note/export?ownerId=&name=` 返回 md；`GET /note/exportAll` 打包 zip | US-9 |
| R10 标签管理 | 标签列表 + 使用次数 + 重命名/合并（合并时批量改写所有笔记 frontmatter） | US-10 |

### 5.3 Future Considerations（P2，v1 架构上需留口）

| 设想 | 架构上必须提前保证 |
|------|-------------------|
| AI 自动摘要 / 自动打标 | 元数据（frontmatter）需保持**结构化且可扩展**，新增字段不改存储格式 |
| 多人协作编辑 | 每篇笔记的 `ownerId` 必须唯一归属，不能是多值字段 |
| 全文检索引擎升级 | 检索层需收敛到**单一 Service 接口**（如 `NoteSearchService`），切换实现不影响 Controller |
| 知识图谱视图 | `source` 与 R7 的 `[[链接]]` 共用一套引用解析器 |

---

## 6. Technical Considerations

### 6.1 复用与扩展（照既有模块套路，遵循项目 MEMORY 约定）

| 项 | 结论 |
|----|------|
| 控制器 | 扩展现有 `NoteController`（`com.cyz.note.controller`），**不新建 controller 包**；**仍不实现 `CommController`**（否则被 `MockAspect`/`ControllerAspect` 兜底拦截） |
| 鉴权 | `/note/**` 已在 `AuthInterceptor` 的 `addPathPatterns` 中，**无需改动** `WebMvcConfig` |
| 前端 | 扩展 `note.js`（不新建文件）；入口 `api.html` 已有 `<li class="menu-item" data-target="note">`，**无需改动**菜单 |
| H5 | `h5.js` 内同步一份精简实现（保持桌面/H5 双端可用） |
| 迁移 | 新增字段（`tags`/`source`）全部**可选**，老笔记零迁移即可正常读取 |
| CSS | 优先复用既有 `note-*` 系列类，不新造轮子 |

### 6.2 存储选型（v2 最重要的架构决策）

**v1 现状**：`searchPublic` 遍历**所有用户目录**逐文件读盘解析 frontmatter。加全文检索后，每次搜索 = 全库文件 IO + 逐篇全文比对。用户数增长后必然超时。

**三选一对比：**

| 方案 | 检索性能 | 新增运维成本 | 改造工作量 | 评价 |
|------|---------|------------|-----------|------|
| A. 保持纯文件 + 内存全文索引 | 好 | 低（无） | 中 | 需处理重启重建、多实例不一致 |
| **B. 引入 `note_index` 表，文件仍为真相源** | **好** | **低（MySQL 已有）** | **中** | **推荐** |
| C. 上 ES | 最好 | 高（需新中间件） | 高 | 数据量级远不需要 |

**推荐方案 B**：文件仍作真相源（保持 v1 的可导出、可手改优势），新增 `note_index` 索引表，在 `saveNote`/`deleteNote`/`migrateOld` 中同步维护。

```sql
CREATE TABLE IF NOT EXISTS `note_index` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `owner_id` BIGINT NOT NULL COMMENT '笔记属主用户 id',
  `note_name` VARCHAR(64) NOT NULL COMMENT '净化后的标题，与文件名一致',
  `title` VARCHAR(255) NOT NULL DEFAULT '' COMMENT 'frontmatter.name 原始标题',
  `category` VARCHAR(64) NOT NULL DEFAULT '' COMMENT '分类，受控词表或用户自建',
  `tags` VARCHAR(500) NOT NULL DEFAULT '' COMMENT '标签，逗号分隔',
  `source_key` VARCHAR(128) NOT NULL DEFAULT '' COMMENT '来源条目 key，空=非摘录',
  `is_public` TINYINT NOT NULL DEFAULT 1 COMMENT '1=公开 0=私有',
  `content_indexed` TINYINT NOT NULL DEFAULT 0 COMMENT '1=content 已入库 0=仅元数据（他人public不入库，Q1决策）',
  `content` MEDIUMTEXT COMMENT '正文全文，仅 content_indexed=1 时有值，供本人内容命中检索',
  `preview` VARCHAR(512) NOT NULL DEFAULT '' COMMENT '列表预览，150 字',
  `owner_name` VARCHAR(64) NOT NULL DEFAULT '' COMMENT '属主展示名，冗余避免列表 N+1',
  `update_time` DATETIME NOT NULL COMMENT '文件最后修改时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_owner_note` (`owner_id`,`note_name`),
  KEY `idx_public_time` (`is_public`,`update_time`),
  KEY `idx_source` (`source_key`),
  KEY `idx_owner_time` (`owner_id`,`update_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='笔记检索索引，md 文件为真相源';
```

> **已落地**：`sql/V11__note_index.sql`（人工存档）+ `DataInitializer.ensureNoteIndexTable()`（启动时幂等建表 —— 本机无 mysql 客户端）。
>
> 🔒 **`content_indexed` 是 Q1 决策的落地点：正文只对属主入库。**
> 写入侧判定 —— 仅当 `ownerId == 当前登录用户` 才写 `content` 并置 `content_indexed=1`；
> 他人 public 笔记 `content` 留空、置 0。查询侧据此只对 `content_indexed=1` 的行做正文 LIKE。
> `content` 列再叠一层纵深防御：即使查询条件写错，库里也没有他人正文可泄漏。
> 将来若产品放开，只需重建索引 + 放开查询条件，**表结构无需变更**。

**约束：**
- 检索统一走 MyBatis（`mapUnderscoreToCamelCase` 已开），**不加新依赖**
- 保留 `NoteDataService.listMyNotes` 的目录扫描路径，作为索引缺失时的兜底重建
- 提供 `POST /note/rebuildIndex`（仅超管），全库重建索引，幂等；重建时按同一规则回填 `content_indexed`
- `MEDIUMTEXT` 而非 `TEXT`：v1 正文上限 500KB，`TEXT` 的 64KB 装不下
- 唯一键用 `(owner_id, note_name)`：与 v1「同用户标题唯一」对齐，保证索引与文件一一对应

### 6.3 依赖与风险

| 风险 | 影响 | 应对 |
|------|------|------|
| 索引与文件不一致 | 搜到但文件已删 | 所有写操作走 `NoteDataService` 单一出口；提供 `rebuildIndex` 自愈 |
| 存量笔记未入索引 | 搜不到老笔记 | 上线时执行一次全量重建；或首次访问时懒加载入索引 |
| 目录扫描随用户数线性变慢 | 列表变慢 | `note_index` 承载列表，前端不再等目录扫描 |
| 摘录功能需读 classpath 资源 | 任意路径读取风险 | **白名单校验** `sourceKey` 必须匹配 `data/**` 或 `data-shell/**` 下已存在条目 |
| `sourceKey` 含特殊字符 | 注入风险 | 用现有路径净化逻辑 + 严格前缀匹配，参照 v1 的 `canonicalPath` 校验 |
| 摘录默认私有但用户想公开 | 认知成本 | 编辑页明确展示公开性开关，默认 private 且有文字说明 |

---

## 7. Success Metrics

### 7.1 北极星指标

> **知识复用次数（Weekly Reuse Count）**：一周内被打开 ≥2 次的笔记条目数 ÷ 笔记总数

知识库的价值不是"存了多少"，而是"被用了多少次"。这个指标同时惩罚"没人写"和"写了没人用"。

### 7.2 驱动指标（Leading，1–4 周可见）

| 指标 | 基线 | 目标 | 测量方式 |
|------|------|------|--------|
| 全库搜索使用率 | 无（v1 搜索仅标题，功能弱） | ≥40% 的活跃用户每周用 ≥3 次 | 新增 `note_search` 埋点（query/hit/miss） |
| 搜索成功率 | — | ≥70%（有命中），零结果查询 ≤15% | `hit=0` 的查询占比；**因 Q1 决策，口径仅统计本人笔记**（他人 public 只能标题匹配，计入会失真） |
| 一键沉淀使用量 | 0 | 上线 1 个月内 ≥100 次 | `saveFromSource` 调用计数 |
| 新增笔记中带标签比例 | 0%（无标签功能） | ≥50% | `note_index.tags != ''` 占比 |
| 资料页 → 笔记页跳转率 | 0 | ≥5% | 资料页跳转埋点 |

### 7.3 健康指标（Guardrail）

| 指标 | 阈值 | 说明 |
|------|------|------|
| 搜索 P95 响应 | < 500ms | 超阈值即索引方案失效，需优化 SQL |
| 私有笔记泄露事件 | **0（红线）** | 任何 1 例即回滚 |
| 笔记删除率 | 删/写 < 30% | 过高说明沉淀没价值或写入门槛太高 |
| 索引一致性 | 抽检差异 < 0.1% | 定期跑 `rebuildIndex` 比对 |

### 7.4 滞后指标（Lagging，1 季度）

- **NPS / 满意度**：wxyd 平台整体满意度变化
- **新成员上手时长**：能否通过搜索笔记解决常见问题（定性访谈）
- **存量知识资产**：公开笔记总篇数（体现团队知识沉淀）

### 7.5 评估节奏

- 上线 **1 周**：看搜索使用率 + 搜索成功率 + 响应时间，只判断"有没有人用、能不能用"
- 上线 **1 月**：看北极星 + 一键沉淀量 + 标签覆盖率，判断"用得好不好"
- 上线 **1 季度**：看滞后指标，决定是否投入 P2（AI 摘要 / 图谱视图）

---

## 8. Stakeholder Talking Points（给不同对象的一页纸）

### 8.1 管理层视角（投入产出）

- **投入**：约 1 个前端 + 1 个后端的人力，**零新增中间件**（复用现有 MySQL）
- **产出**：
  - 把"一次调通就忘"的经验变成可检索资产，降低重复排障成本
  - 公开笔记形成团队知识库，新成员可自助检索
  - 资料页 + 个人笔记双向连通，v1 已投入的 50+ 篇资料价值被放大
- **最大风险**：私有笔记泄露 —— 已列为**上线红线**，R6 不通过不上线
- **不做的事**：AI 摘要、协作编辑、富文本编辑器（见 Non-goals），避免范围蔓延

### 8.2 研发视角（技术要点）

- **不改存储真相源**：仍是 md 文件，只加一张 `note_index` 检索索引表
- **不引入新依赖**：MyBatis + MySQL LIKE 已足够；搜索层收敛到 `NoteSearchService` 接口，为将来换引擎留口
- **向后兼容**：新字段（`tags`/`source`/`category` 词表化）**全部可选**，存量 8 篇老笔记零迁移
- **安全要点**：
  - 所有查询强制 `is_public=1 OR owner_id=#{currentUserId}`
  - `saveFromSource` 的 `sourceKey` 必须白名单校验，禁止任意 classpath 读取
  - 文件操作沿用 v1 的「净化 + canonicalPath 前缀校验」
- **前端约定**：新请求一律用**原生 fetch**（jQuery 1.8.3 处理 blob 不便，且其全局 ajaxError 会抢 401 跳转）
- **不做单测**：项目无测试源码，沿用既有惯例；验证走构建 + curl 全链路 + 前端 diff 审查

### 8.3 客服/运营视角（场景化）

- 引导用户按「分类 + 标签 + 关键词」三层检索，而不是只搜标题
- 公共笔记默认**私有**，用户主动切换才公开 —— 引导时务必说明，避免"以为存了但别人看不到"
- 摘录笔记来源清晰（标注来自哪个资料页），可信度高，可作为内部答复依据

---

## 9. Open Questions

### 阻塞项（✅ 已全部关闭）

| # | 问题 | 归属 | 为何阻塞 |
|---|------|------|---------|
| ~~Q1~~ | ~~`note_index.content` 是否存储他人 public 笔记全文~~ | — | ✅ **已定稿 2026-10-08：不存他人 public 正文**，落地点为 `content_indexed` 标志位，见 R1 与 6.2 |
| ~~Q2~~ | ~~摘录笔记默认 private 还是 public~~ | — | ✅ **已定稿 2026-10-08：默认 `private`**（沉淀是个人行为，公开需用户主动切换），已在 `saveFromSource` 中落实 |
| ~~Q3~~ | ~~一键沉淀入口范围~~ | — | ✅ **已定稿 2026-10-08：三处全上**。Java API / Shell / Dubbo 均已加「存入笔记」入口（Dubbo 无资料页，摘录对象为可复用调用样例） |

### 非阻塞项（开发中可定）

| # | 问题 | 归属 |
|---|------|------|
| Q4 | 标签是否有预置词表，还是完全自由 | 产品 |
| Q5 | 搜索是否需要高亮渲染命中关键词（v1 只返回 preview 文本） | 前端 |
| Q6 | `note_index` 是否需要定时任务防漂移，还是仅靠 `rebuildIndex` 手动兜底 | 后端 |
| Q7 | H5 端是否需要同等功能，还是仅保留只读浏览 | 产品 + 前端 |

---

## 10. 交付节奏建议

| 阶段 | 内容 | 依赖 |
|------|------|------|
| **Phase 0**（阻塞期） | ✅ 已完成 —— Q1/Q2/Q3 全部定稿，存储选型落地为 note_index 表 | 无 |
| **Phase 1** | `note_index` 表 + 检索层 + 全量重建；`note/search` 升级为全文检索（US-1） | Phase 0 |
| **Phase 2** | frontmatter 扩字段（`tags`/`source`）+ 分类词表（US-4、US-5） | Phase 1 |
| **Phase 3** | 一键沉淀（US-2）+ 双向关联（US-3） | Phase 2、Q3 结论 |
| **Phase 4** | 权限红线专项回归（R6）+ 灰度 | Phase 3 |
| **Phase 5** | P1 顺次：R7 双向链接 → R8 最近浏览 → R9 导出 → R10 标签管理 | 上线后 |

> **关键路径**：Phase 1 → 2 → 3 不可并行（都依赖 frontmatter 与索引结构定稿）。Phase 4 是**不可裁剪的质量闸门**。

---

## 附录 A：现有接口变更清单

| 接口 | 状态 | 说明 |
|------|------|------|
| `GET /note/search` | **改造** | 加 `scope`/`field` 参数，返回值加 `titleMatched`/`contentMatched` |
| `GET /note/categories` | **新增** | 返回预置 ∪ 个人已有分类 |
| `GET /note/tags` | **新增** | 返回标签及使用次数 |
| `POST /note/saveFromSource` | **新增** | 从资料页一键摘录为笔记 |
| `GET /note/source/{sourceKey}/notes` | **新增** | 反查引用某来源条目的公开笔记 |
| `POST /note/rebuildIndex` | **新增** | 仅超管，全量重建索引（幂等） |
| `GET /note/list` | 微调 | 优先走索引表，目录扫描降级为兜底 |
| `GET /note/get` | 不变 | |
| `POST /note/save` | 扩展 | 支持 `tags` 字段；保存后同步索引 |
| `POST /note/delete` | 扩展 | 删除后同步清索引 |
| `POST /note/migrateOld` | 扩展 | 迁移时一并写入索引 |

## 附录 B：新增错误码（现有至 `ERROR000018`）

| 错误码 | 含义 |
|--------|------|
| `ERROR000019` | 笔记标签数量超限 |
| `ERROR000020` | 来源条目不存在或未开放摘录 |
| `ERROR000021` | 索引重建进行中，请稍后重试 |
