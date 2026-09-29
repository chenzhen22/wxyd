# 群聊功能设计（桌面版 + H5）

日期：2026-09-29

## 需求

- 桌面版（api.html）和 H5（h5.html）均支持群聊。
- 每个用户最多创建 5 个群聊。
- 用户模糊搜索群聊名称可申请加入。
- 群管理员（创建者）可审批加入申请、踢出成员。
- 点击群聊进入群聊界面，仅支持发送文字、表情、图片。
- 消息记录保存在浏览器缓存（localStorage）；服务器只做消息中转，所有成员均已送达后服务器删除中转消息。

## 已确认的决策

| 决策点 | 结论 |
|--------|------|
| 实时机制 | HTTP 轮询（2 秒），无 WebSocket/SSE |
| 管理员定义 | 创建者即管理员，不可移交 |
| 图片存储 | 服务器本地目录，消息中只传 URL |
| 浏览器缓存 | localStorage 按群 ID 分 key 存储 |
| 离线处理 | 中转消息 7 天自动清理兜底 |

## 数据模型

新增迁移脚本 `app/src/main/resources/sql/V4__group_chat.sql`：

```sql
-- 群聊
group_chat (id, name, owner_id, create_time)
-- 群成员
group_member (id, group_id, user_id, role('owner'/'member'), join_time)
-- 入群申请
group_join_apply (id, group_id, user_id, status(0待审/1通过/2拒绝), apply_time, handle_time)
-- 中转消息：每条消息按“接收人”逐行落库
group_msg_transit (id, group_id, from_user_id, to_user_id,
                   msg_type('text'/'emoji'/'image'), content, send_time, expire_time)
```

核心机制：

- **送达即删**：发一条群消息 = 为除发送者外的每个成员插入一行 `group_msg_transit`。成员客户端轮询 `/group/pull` 时查出自己名下的行 → 返回 → 立即 DELETE。最后一个成员拉走后，该消息在服务端自然消失。
- **7 天兜底**：`expire_time = send_time + 7天`；每次轮询/发送顺带清理过期行，防止长期离线成员导致表膨胀。
- **图片**：上传后存 `{shareDir}/group/`，消息 `content` 只存 URL。
- **5 群上限**：`create` 时 `SELECT COUNT(*) FROM group_chat WHERE owner_id=?` 校验。

## 后端接口

新增 `controller/GroupController.java`（implements `CommController`）、`service/GroupService.java`、`mapper/GroupMapper` + XML。全部走统一 `Result` 响应，受 `AuthInterceptor` 保护，身份从 Session 取 `userId`。

| 接口 | 说明 |
|------|------|
| `POST /group/create` | `{name}`；校验群名非空/长度≤20、每人≤5群；创建者写入 group_member(role=owner) |
| `GET /group/my` | 我加入的群列表 |
| `GET /group/search?keyword=` | 群名 `LIKE %keyword%` 模糊搜索，返回群ID/名称/成员数/是否已申请 |
| `POST /group/apply` | `{groupId, reason}`；已是成员或存在待审记录则拒绝 |
| `GET /group/applies` | 我作为管理员的待审申请列表 |
| `POST /group/handle` | `{applyId, approve}`；校验申请属于我的群且待审；通过则插 group_member |
| `POST /group/kick` | `{groupId, userId}`；仅群主、不能踢自己、目标须是成员 |
| `GET /group/members?groupId=` | 成员列表（须为群成员） |
| `POST /group/img` | MultipartFile 上传，仅 jpg/png/gif/webp、≤5MB，UUID 重命名按日期分目录，返回 URL |
| `POST /group/send` | `{groupId, type, content}`；校验是成员；按成员逐行写 group_msg_transit |
| `GET /group/pull` | 事务内 `SELECT ... FOR UPDATE` 查我的中转消息 → 返回 → DELETE；顺带清理过期行 |

## 前端

**共用逻辑**：新建 `Assets/js/group-chat.js`（两端共用）：群聊渲染、轮询循环、localStorage 读写、表情面板、图片上传发送。

**页面结构**：

- 桌面版 `api.html`：左侧菜单新增「群聊」→ 面板内两个视图：① 群列表视图（我的群 + 创建 + 搜索申请加入）② 群聊会话视图（消息区 + 输入栏）。
- H5 `h5.html`：新增「群聊」section + 底部 tabbar 入口，同样的双视图，适配移动端全屏切换。

**消息与缓存**：

- localStorage key：`wxyd_group_msg_{群ID}`，存该群消息 JSON 数组。
- 发送：本地乐观渲染 → send 成功确认；失败标红可重发。图片先 `img` 上传拿 URL 再 `send`。
- 接收：进入群聊界面启动 2 秒轮询 → 新消息追加 localStorage 并渲染；离开界面停止轮询。未进入的群不拉取，留在服务器中转（7 天兜底）。
- 表情：Unicode emoji 选择面板（分类 + 常用约 100 个），纯文本插入。
- 图片消息：按 URL 渲染 `<img>`，点击全屏预览。
- localStorage 容量超限（QuotaExceededError）：丢弃该群最旧一半消息并提示。
- 身份显示：消息旁显示发送者用户名；成员表进群时拉一次，缓存在内存。

## 错误处理与边界

- 后端校验（ErrorEnum 新增错误码）：建群超 5 个、群名重复/超长、非成员调 send/pull/members、踢人/审批权限、重复申请拦截、图片类型与大小。
- 并发：`pull` 用事务 + `FOR UPDATE` 再删，避免双开页面重复拉取。
- 消息顺序：按 `group_msg_transit.id` 排序，前端按到达顺序追加。
- 轮询 401（Session 过期）→ 停止轮询并提示重新登录。
- 图片文件为永久存储，不受中转消息 7 天清理影响。

## 验证方式

构建通过后用 curl 带 Session Cookie 验证全链路：建群→搜索→申请→审批→发送→拉取→中转删除。前端逻辑通过阅读 diff 确认，不重打包重启。项目无测试源码，本次不新增单测。
