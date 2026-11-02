# 薄荷账迁移笔记

## 流程（可复跑）

1. 微信开发者工具 → 云开发控制台 → 数据库 → `bills` 集合 → 导出 JSON。
2. 从导出文件中筛出目标 openid 的记录（观察 `_openid` 字段，多个 openid 需人工确认归属）。
3. 以超管登录 wxyd，调用 `POST /api/bohe/import`：

```json
{ "targetUsername": "chenzhen", "bills": [ ...筛选后的记录... ] }
```

4. 返回 `{inserted, updated, skipped}` 与筛选条数核对；重复导入同一批应 `inserted=0`（按 `cloud_id` upsert）。

## 执行记录

| 日期 | 来源 openid（前8位） | 目标用户 | inserted | updated | skipped | 备注 |
|------|--------------------|---------|----------|---------|---------|------|
| （待执行） | | chenzhen | | | | 声仲存量账单 |

## 注意

- 导入为**快照语义**：重复导入会覆盖 wxyd 端对该账单的修改。
- 在 wxyd 删除的账单（物理删除，cloud_id 随行消失）**不会**被重复导入复活——同一条云记录会以原 cloud_id 重新插入；如需永久排除，请从云导出文件中剔除该条。
- categories/follows/users 不迁移；wxyd 端分类仅 8 个内置 key，未知分类显示为"其他"。
