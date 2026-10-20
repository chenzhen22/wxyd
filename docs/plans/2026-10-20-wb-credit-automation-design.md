# 积分自动化（移植 dsh-xdpool scheduler，核心 5 类）

日期：2026-10-20

## 范围决策

移植 xdpool scheduler.ts 的积分自动化到账号池，**不做事件链合成**（task-events.ts 的桌面事件伪造不上）：
任务进度依赖网关真实流量 + 每日 report ping。共 6 个 job：

| job | 默认小时 | 动作 |
|-----|---------|------|
| checkin | 9 | POST /v2/billing/meter/daily-checkin（billingBase），重复签到（已签到/already）视为幂等成功 |
| report | 10 | POST /v2/report 发一条 chat_request_send 事件（点亮连登/热力图），回读 /activity/growth/streak 验证 |
| tasks | 11 | GET /v2/activity/growth/tasks → accept 未报名未锁定的 → 重读领奖（claim 走 www.workbuddy.cn web origin + x-client-platform: web） |
| streak | 12 | /activity/growth/streak 读档 → 兑换已解锁 tier（/activity/growth/redeem，client_token 幂等）→ 有几张抽奖券抽几张 |
| travel | 9, 21 | buddy info → 未领养先 agree+first（400=活动门未到，正常）→ arrived 领奖励 → idle 且未达日限则 depart(location 4) |
| compensation | 11 | /billing/meter/compensation-status（**无 /v2**）→ 有补偿则 claim-compensation |

## 调度（照 xdpool）

- 每分钟整点唤醒比对配置小时，`lastRunDate != 今天` 才跑（每任务每日一次）；手动「立即执行」不消耗当日档位。
- 账号串行、间隔 800ms；单账号失败跳过继续；CN 账号限定（domain 含 workbuddy.ai 的 global 号跳过）；会话死号跳过。
- 配置/状态持久化 `data/wb-pool/automation.json`（enabled、hours、lastRunDate、earnings 账本）。

## 实现

- `pool/WbGrowthClient.java`：growth/checkin/compensation/task 全部端点（envelope 校验、错误分类复用 WbUpstreamException）。
- `pool/WbCreditAutomationService.java`：tick 调度 + 6 个 runner + automation.json 持久化 + 任务状态视图。
- `WbGatewayManageController`：GET gateway/admin/automation（状态）、POST gateway/admin/automation/run {kind}（立即执行）、POST gateway/admin/automation/enabled {enabled} —— 全部 isAdmin 守卫。
- 前端：模型管理 → 账号池区块下方新增「🎁 积分自动化」折叠段：总开关、每任务一行（配置小时/最近运行/结果/领取明细/立即执行）。

## 非目标

- task-events 桌面事件链合成（专家召唤/Canvas/Playbook 等）
- 积分账本 UI（automation.json 已记账，页面先展示任务级明细）