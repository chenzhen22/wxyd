# 福利专区（welfare）设计

日期：2026-10-08

## 背景

dataScan（/apps/GIT/dataScan）每小时抓取 linux.do「福利羊毛」分类帖子，清洗后存入与 wxyd 共用的 MySQL `wxyd_123` 库 `scan_post` 表（7 天保留）。wxyd 增加「福利专区」页面，只读展示这些帖子。

## 原则

- dataScan 端零改动；wxyd 通过现有 MyBatis DataSource 直接只读查 `scan_post`。
- **纯展示福利**：不展示作者、图片数、浏览/点赞/回复数、来源站点名、原帖链接等任何原站元信息。
  - 列表卡片：标题、正文摘要（~120 字）、首图、发布时间。
  - 详情：标题、发布时间、正文（Markdown 渲染，含图片）。

## 后端

- `pojo/ScanPost.java`：`id`、`title`、`summary`、`firstImage`、`publishedAt`、`contentMd`、`images`。
- `WelfareController`（实现 `CommController`，登录后可见）：
  - `GET welfare/list?page=&size=20` → `[{id,title,summary,firstImage,publishedAt}]`，`ORDER BY published_at DESC` 分页。
  - `GET welfare/detail?id=` → `{title,publishedAt,contentMd,images}`。
- SQL 加在 `MysqlMapper.xml`，只查所需列；`images` JSON 以字符串取出后 Java 侧解析。
- 图片代理：`WebMvcConfig` 映射 `/welfare-img/**` → `file:/apps/shareFile/datascan/`，纳入 `AuthInterceptor` 登录校验。前端用 `local_path` 把正文远程图片地址替换为本地地址；无本地文件的图不渲染（不暴露原图 URL）。

## 安全

- 图片路径由 Spring `ResourceHttpRequestHandler` 规范化，拒绝 `../` 越界。
- Markdown 前端渲染，白名单标签（段落/标题/列表/图片/粗斜体等），剔除 script/iframe/on* 属性防 XSS。
- 详情不渲染外链。

## 前端

- PC：`api.html` + `Assets/js/welfare.js`；响应式卡片网格（2~4 列），卡片=首图+标题+摘要+时间；点击进详情视图，返回按钮回列表；滚动到底自动加载下一页。
- H5：`h5.html` 加 `page-welfare` section + `Assets/js/h5-welfare.js`；单列卡片流，详情用底部抽屉；无限滚动。
- 菜单：key `welfare`，名称「福利专区」，默认全部可见，走现有菜单管理系统。

## 错误处理

- 列表/详情查询失败返回 `Result` 统一错误码；空数据列表前端显示「暂无福利」。
- `scan_post` 表不存在时不影响应用启动（SQL 仅在调用时执行）。
