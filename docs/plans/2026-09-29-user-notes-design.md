# 记忆笔记用户级改造设计

日期：2026-09-29

## 需求

- 笔记改为用户级：每个用户可新增/编辑自己的笔记（markdown 格式）。
- 旧笔记（resources/data-note/ 8 篇）归到 admin 用户下。
- md 文件存储在 `/apps/shareFile/{用户id}/`。
- 笔记分公共/私有；公共笔记可被其他用户按标题模糊搜索到。

## 已确认决策

| 决策点 | 结论 |
|--------|------|
| 元数据存储 | md 内嵌 YAML frontmatter（name/category/visibility），无数据库表 |
| 旧笔记迁移 | 手动脚本迁移（管理端触发接口，幂等） |
| md 渲染 | 复用现有渲染器：桌面 renderMarkdown、H5 mdToHtml |
| 编辑器 | 纯 textarea + 实时预览，不引入第三方 md 编辑器 |

## 存储布局

```
/apps/shareFile/
  {userId}/
    {净化标题}.md
```

- frontmatter：`name`（标题）、`category`、`visibility`（public/private）；**缺省按 public**（兼容旧笔记）。
- 文件名 = 净化后的标题（去 `/ \ ..`、≤50 字符）；同用户标题重复拒绝；改名 = 删旧写新。
- 路径防护：净化 + canonicalPath 前缀校验。
- 列表/搜索扫描目录解析 frontmatter，正文截 150 字预览。

## 后端接口（NoteController 改造，/note 前缀）

| 接口 | 说明 |
|------|------|
| `GET /note/list` | 我的笔记（扫描自己目录），按 updateTime 倒序 |
| `GET /note/search?keyword=` | 按标题模糊搜公共笔记（所有用户目录 visibility=public），带 ownerName |
| `GET /note/get?ownerId=&name=` | 读单篇：自己的任意 / 他人的 public；他人私有返回不存在 |
| `POST /note/save` | `{name, category, visibility, content, oldName?}` 新增/更新自己的笔记；oldName≠name 时改名；正文 ≤500KB |
| `POST /note/delete` | 删自己的笔记 |
| `POST /note/migrateOld` | 仅超管：classpath data-note/*.md 拷入 /apps/shareFile/{adminId}/，同名跳过幂等 |

- 移除旧接口 `GET /note`、`GET /note/{name}`、`POST /note/reload`。
- 不实现 CommController（维持现状）。

## 前端

**桌面版（note.js）**：「我的笔记 / 公共搜索」双 tab；我的笔记按 category 分组 + 编辑/删除/新增；详情复用 renderMarkdown + TOC；编辑视图 = 标题/分类/公私/textarea + 预览切换；公共搜索结果只读（带 ownerName）。

**H5（h5.js）**：同样双 tab；FAB 新增；详情 mdToHtml；编辑用底部抽屉表单。

## 验证方式

构建 + curl 全链路（list/save/get/search/delete/migrateOld）+ 前端 diff 审查；部署后用 admin 账号实测。项目无测试源码，不新增单测。
