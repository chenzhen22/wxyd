# 2026-11-02 薄荷账整合进 wxyd Android 端 设计

## 意图

把微信小程序「薄荷账」（D:\GIT\bookkeeping，纯云开发）的记账能力整合进 wxyd Android 端。小程序继续独立使用，wxyd 端通过**管理员导入功能**持续同步账单数据（方案 A）。数据主库迁到 wxyd MySQL，user 关联 wxyd 用户体系。

## 背景与现状

- 小程序：云函数 + 云数据库，集合 `bills`（金额分、分类、日期）/ `users` / `categories` / `follows`，另有消息聊天（`chats`/`messages`，**本次不做**）。
- wxyd：Spring Boot 2.7 + MyBatis + MySQL，Session 鉴权，`users` 表（id/username/display_name/role）。Android 端 Compose，底部 2 Tab（工作台/我的）。
- 数据迁移对象：声仲的账单 → chenzhen 名下；**只迁 bills**。

## 决策记录（用户已确认）

| 决策点 | 结论 |
|------|------|
| 同步方式 | 方案 A：小程序不动，管理员页面导 JSON 导入，可重复导，`_id` 去重 |
| 关注/分享 | 做成**加好友**：搜索用户 → 申请 → 通过后互见账单；不做二维码 |
| 迁移范围 | 只迁 bills；users/categories/follows 不迁 |
| 消息 | 不做 |

### 追加决策（builder check 后确认）

- 导入 JSON 由调用方预先过滤好（Task 5 只筛声仲记录），接口不再做 openid 区分；**重复导入覆盖 wxyd 端数据**（快照语义）。
- 未知分类原样入库，Android 显示兜底为"其他"。
- 好友：任一方向 status=1 即互见；被拒后可重新申请（重置旧记录）；提供删除好友（status=2）。
- 用户搜索：模糊匹配 LIMIT 20；薄荷账 Tab 所有登录用户固定可见；bohe 不建独立设计文档（简单模块）。
- **可删除账单**：删除时同时清空 cloud_id（防重复导入复活）；**不做编辑**。

## Android 端

1. **底部 Tab**：工作台 / **薄荷账（中间）** / 我的（`MainScreen.kt` 的 `MAIN_TABS` 加一项）。薄荷账是内嵌 Tab 页，非压栈模块页。
2. **薄荷账主页**：3 张卡片（布局参照小程序 bills/stats/mine）：
   - **账单**：按日期倒序的账单列表（金额、分类、备注、日期），只读展示。
   - **统计**：本月合计 + 按分类占比（简单条形即可，沿用小程序口径：支出合计/分类汇总）。
   - **我的关注**：好友列表（昵称=对方 display_name/username），点进查看对方账单（只读）；底部「加好友」入口 = 搜索用户名 → 发申请 → 等待/已通过状态。
   - **加号 FAB**：直接记账（分类、金额、备注、日期），保存后返回并落在账单卡片。
3. 好友查看权限：accepted 即互见（我加了你也等于你能看我），不设单向。

## 服务端（wxyd app 模块）

新包 `bohe`（controller/service/mapper），接口前缀 `/api/bohe/**`，沿用 `AuthInterceptor` Session 鉴权。

### 表（V29__bohe_bill.sql）

```sql
CREATE TABLE bohe_bill (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT NOT NULL,            -- wxyd users.id
  cloud_id VARCHAR(64) DEFAULT NULL, -- 小程序云数据库 _id，导入去重键
  amount BIGINT NOT NULL,            -- 分
  category VARCHAR(32) NOT NULL,
  note VARCHAR(255),
  bill_date DATE NOT NULL,
  create_time DATETIME,
  UNIQUE KEY uk_cloud_id (cloud_id),
  KEY idx_user_date (user_id, bill_date)
) CHARSET=utf8mb4;

CREATE TABLE bohe_friend (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT NOT NULL,           -- 发起方
  friend_id BIGINT NOT NULL,         -- 接收方
  status TINYINT NOT NULL DEFAULT 0, -- 0=待通过 1=已通过 2=已拒绝/删除
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_pair (user_id, friend_id)
) CHARSET=utf8mb4;
```

### 接口

- `GET /api/bohe/bill/list?month=&friendId=` 账单（自己的，或好友的——好友校验 status=1）
- `POST /api/bohe/bill/save` 记账
- `GET /api/bohe/stats?month=&friendId=` 统计
- `GET /api/bohe/friend/list` / `POST friend/request` / `POST friend/handle`（通过/拒绝）
- `GET /api/bohe/user/search?keyword=` 用户搜索（用于加好友）
- `POST /api/bohe/import` **仅超管**：上传云数据库导出的 bills JSON，`cloud_id` upsert；请求参数 `targetUserId`（如 chenzhen）。openid 映射在导入时人工指定（本次：声仲 openid → chenzhen）。

### 迁移

前置：微信开发者工具导出 `bills` 集合 JSON。导入走上面超管接口，一次性完成（功能常驻，以后可持续导）。

## 非目标

- 不动小程序代码与云函数。
- 不做消息/聊天。
- 不迁 users/categories/follows。
- wxyd 端不做预算/收入（与小程序口径一致，纯支出）。

## 留给实现阶段

- 卡片视觉细节、分类 icon/颜色（沿用小程序 constants）。
- 统计图的呈现形式（列表条形即可起步）。
- 导入 JSON 的字段容错（云导出格式字段名 `_id/amount/category/note/date/createTime`）。
