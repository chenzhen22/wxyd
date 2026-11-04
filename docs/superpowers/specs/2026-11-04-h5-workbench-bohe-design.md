# H5 改版：工作台 + 薄荷账 设计（2026-11-04）

## 意图

H5 底部导航改版：首页→工作台，留言板/群聊移入工作台，底部中间新增薄荷账。
薄荷账 H5 界面照 `D:\GIT\bookkeeping`（微信小程序，/mnt/d/GIT/bookkeeping）移植，复用现有 `bohe/*` 后端接口（与 Android App 共用）。

## 目标

- 底部 4 tab（首页/留言板/群聊/我的）→ 3 tab：**工作台 / 薄荷账 / 我的**，薄荷账居中。
- 留言板、群聊从 tab 变为工作台网格卡片。
- 薄荷账 H5 从零建：账单、统计、关注三大功能（对齐 Android 版范围）。

## 非目标 / 不做

- 好友聊天、消息 tab（小程序走微信云函数，wxyd 后端无对应接口）
- 二维码分享关注（依赖微信体系）
- 收入、预算

## 做法（用户已选 B：独立页面）

页面拆分（做法 B，独立页面而非单页子 tab）：

| 页面 | key | 内容 |
|------|-----|------|
| 薄荷账主页 | `bohe` | 本月总额总览 + 按月翻账单列表（左滑删除）+ 统计/关注入口卡片 + 记一笔 FAB |
| 统计 | `boheStats` | 日/月/年粒度切换、期总额、分类占比 |
| 关注 | `boheFriends` | 好友列表（状态区分待通过/已通过）、搜索用户发起关注、审批、点好友查看其账单（只读 + 切回我） |

- 记一笔：底部抽屉表单（金额/分类 8 类/备注/日期），复用 `openSheet`。
- 分类对齐 Android `BoheCategories`：food/transport/shop/house/fun/medical/study/other。

## 改动点

1. `h5.html`：tabbar 改 3 tab（工作台/薄荷账/我的）；新增 `page-bohe`、`page-boheStats`、`page-boheFriends`；引入 `h5-bohe.js`。
2. `Assets/js/h5-bohe.js`（新）：三页渲染 + 逻辑，走 `PAGE_RENDER` 或 `H5Modules` 机制。
3. `Assets/css/h5.css`：薄荷账样式（升级 `?v=` 缓存参数）。
4. `h5.js`：`menuVisible` 白名单加入 `bohe`；工作台仍由 `menu/list` 驱动（message/group 补 hashUrl 后自然出卡片）。
5. `DataInitializer.MENU_SEED`：h5 段 `message`/`group` 补 `hashUrl`（`#/message`、`#/group`），新增 `bohe`/`boheStats`/`boheFriends` 菜单项（bohe 固定可见 fixed=1）。

## 后端

无改动（接口齐全：`bohe/bill/list|save|delete`、`bohe/stats`、`bohe/friend/*`、`bohe/user/search`、`bohe/visibility`）。

## 约定

- 金额分→元显示沿用 Android 逻辑（后端存储单位与 Android 一致）。
- 菜单可见性走 `menu/list?platform=h5`，被隐藏的模块自动从工作台消失（既有机制）。
