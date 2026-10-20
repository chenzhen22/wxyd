# 巡检日报改造：资产管理 → 零配置系统自检

日期：2026-10-20

## 背景与动机

巡检日报（O2）最初为「有公网资产要盯」的运维场景设计：手动添加 url / domain / server 三类资产，定时探测后汇总推送。当前实际部署形态是 **WSL 跑应用 + 远程 MySQL**，三类资产一项都用不上，真正值得巡检的是远程数据库健康与应用自身状态。

产品决策：**删除资产管理体系，改为零配置固定四项系统自检**。

## 巡检项（固定，不可配置）

| # | 巡检项 | 内容 | 判定 |
|---|--------|------|------|
| 1 | 远程 MySQL | `SELECT 1` 实测延迟；HikariCP 活跃/空闲/等待连接；`SELECT VERSION()`、uptime、Threads_connected | 连不上=fail；延迟 >500ms=warn |
| 2 | 应用自身 | JVM 运行时长、堆内存使用率、系统 CPU 负载 | 堆 >80%=warn |
| 3 | 磁盘水位 | 应用工作目录所在盘 总/用/余 | 已用 ≥85%=warn |
| 4 | 推送通道 | SMTP 是否配置；日报设置绑定的钉钉机器人/邮箱是否仍有效 | 设置启用但通道资源失效=warn，否则 ok |

## 改动清单

### 后端

- **新增** `patrol/model/PatrolReport.java`：`CheckResult{name,status,detail}` + `failCount/warnCount/lastRunTime` + `toMarkdown()`（沿用原日报文案结构）
- **删除** `patrol/model/PatrolAsset.java`
- **重写** `patrol/service/PatrolProbe.java`：固定四项探测器（注入 `mysqlDataSource`），异常归一化为 CheckResult 不上抛
- **改造** `patrol/service/PatrolService.java`：删资产 CRUD 与 SM4 加密；`runForUser` → `runAll()`，结果存内存（volatile lastReport，单实例语义与滑块/registerToken 一致）；`dailyReportJob` 每小时跑一次巡检、按命中的用户设置逐个推送；`testPush` 语义不变
- **改造** `patrol/controller/PatrolController.java`：删 `list/add/delete`；`runNow` 改调 `runAll`；新增 `GET patrol/status` 返回最近一次巡检结果；`setting` GET/POST、`testPush` 保留；管理员守卫不变
- **改造** `PatrolMapper` + XML：删资产五个方法；保留 `getSetting/upsertSetting/listUsersToPush`；新增 `countRobot(userId, robotId)` 校验机器人归属
- **改造** `DataInitializer.ensureRadarPatrolTables`：删 `patrol_asset` 建表，改为 `DROP TABLE IF EXISTS patrol_asset`（幂等）；radar_subscribe / patrol_setting 不动
- **清理** `app/pom.xml`：移除 jsch 依赖（仅 SSH 探测使用）

### 前端

- **api.html**：巡检模块的资产列表 + 添加资产卡片 → 固定四项结果列表（状态点 + 详情 + 巡检时间）
- **patrol.js**：`patrol/list` → `patrol/status`；删添加/删除逻辑；立即巡检、日报设置、测试推送保留
- **h5-patrol.js**：资产卡片 → 固定四项；添加抽屉 → 仅设置抽屉（推送时间/通道/测试推送）

## 数据流

    每日 push_hour（每小时整点触发）
      └─ PatrolService.dailyReportJob
           └─ PatrolProbe.runAll() → PatrolReport（内存 + 返回）
                └─ patrol_setting 命中用户 → PushService.pushDingtalk / pushEmail

    页面：patrol/status 读最近一次结果；patrol/runNow 即时跑一轮并返回

## 非目标

- 不做多实例落库持久化巡检历史（沿用内存语义）
- 不保留旧三类资产的任何兼容入口
- 菜单名「巡检日报」与 patrol_setting 表结构不变