# O1 羊毛雷达 + O2 巡检日报 + O4 PWA 设计

日期：2026-10-09
状态：待签收
决策记录：双通道推送（钉钉+邮件，用户自选）｜巡检四项全上（URL/SSL/WHOIS/SSH 资源）｜每日 8 点汇总日报｜验收驱动（无 JUnit，构建 + curl 全链路）

---

## Chunk 1：公共基建

**定时任务**：`WxydApplication` 加 `@EnableScheduling`（项目目前没有任何 @Scheduled，这是首个）。

**推送抽象** `PushService`（新，`com.cyz.push`）：
- `pushDingtalk(long userId, long robotId, String markdown)` → 委托 `RobotService.send(userId, robotId, "markdown", text, null)`（实现时核对 DingTalkRobotUtil 支持的类型，缺 markdown 则补）
- `pushEmail(String to, String subject, String md)` → 委托 `MailService.send`
- 两个功能（雷达/巡检）共用，失败只 log.error 不抛出（推送是旁路，不阻塞主流程）

**新依赖**：`com.jcraft:jsch:0.1.55`（SSH 探测磁盘/内存，唯一新增依赖）。

## Chunk 2：O1 羊毛雷达

**表（V12__radar_patrol.sql，与 O2 合一个迁移脚本）**：

```sql
radar_subscribe (
  id BIGINT PK AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  keyword VARCHAR(64) NOT NULL,
  channel VARCHAR(10) NOT NULL,          -- dingtalk | email
  robot_id BIGINT NULL,                  -- channel=dingtalk 必填（用户自己的 ding_robot）
  email VARCHAR(128) NULL,               -- channel=email 必填
  last_post_id BIGINT NOT NULL DEFAULT 0,
  enabled TINYINT NOT NULL DEFAULT 1,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  KEY idx_user (user_id), UNIQUE KEY uk_user_keyword_channel (user_id, keyword, channel)
)
```

- 每用户上限 10 条（超限报错）。同 keyword 不同 channel 是两条独立订阅（可同时推钉钉和邮件）。

**接口**（`RadarController`，前缀 `/radar/**`，加入 AuthInterceptor，不实现 CommController）：
- `GET radar/list` → 我的订阅（含最近命中数）
- `POST radar/add` {keyword, channel, robotId?, email?}
- `POST radar/delete` {id}
- `POST radar/toggle` {id, enabled}
- `POST radar/test` {id} → 立即按当前数据推一条测试消息（配置后自测用）

**推送任务** `RadarPushJob`（@Scheduled，每小时第 10 分 `0 10 * * * *`，错开 dataScan 整点抓取）：
1. 查全部 `enabled=1` 订阅
2. 每条查 `scan_post WHERE id > last_post_id AND (title LIKE ? OR summary LIKE ?) ORDER BY id ASC LIMIT 20`
3. 命中 → 组装 markdown（标题/时间/摘要，**不含原站链接**，遵守福利区纯展示原则）→ 按通道推送
4. 成功 → `last_post_id = 本批最大 id`；失败 → 不更新（下轮自然重试），log.error
5. 零命中不发消息（不骚扰）

**验收标准**：
- 添加订阅「Redis」→ test 立即收到钉钉/邮件 → 每小时任务只推增量 → keyword 无匹配时静默 → 删除后不再推
- 未登录访问 /radar/** → 401/302

**前端**：福利区页头部加「📡 订阅雷达」按钮（PC welfare.js + H5 h5-welfare.js 各一份抽屉表单），列表展示我的订阅 + 删除/启停/测试。

## Chunk 3：O2 巡检日报

**表（并入 V12）**：

```sql
patrol_asset (
  id BIGINT PK, user_id BIGINT, name VARCHAR(64),
  asset_type VARCHAR(10),                -- url | domain | server
  target VARCHAR(255),                   -- url:完整URL / domain:域名 / server:host
  port INT NULL,                         -- server 的 SSH 端口，默认 22
  ssh_user VARCHAR(32) NULL,
  ssh_password VARCHAR(256) NULL,        -- SM4 密文（与 ding_robot.secret 同待遇）
  enabled TINYINT DEFAULT 1,
  last_status VARCHAR(10),               -- ok | warn | fail | unknown
  last_detail VARCHAR(1000),
  last_check_time DATETIME NULL,
  create_time DATETIME, KEY idx_user (user_id)
)
patrol_setting (
  user_id BIGINT PK,
  push_hour TINYINT NOT NULL DEFAULT 8,  -- 日报推送小时（0-23）
  channel VARCHAR(10) NOT NULL,
  robot_id BIGINT NULL, email VARCHAR(128) NULL,
  enabled TINYINT DEFAULT 1
)
```

**探测器**（`PatrolService.check`，纯 Java）：
- `url`：OkHttp GET 5s 超时；2xx/3xx=ok，超时/5xx/拒连=fail；HTTPS 顺带读证书 `notAfter`，剩余 <30 天在 detail 附加 warn
- `domain`：Socket 43 WHOIS，正则匹配 `Registry Expiry Date|Expiration Date|Expiry Date|Expiration Time`，取首个 ISO 日期；剩余 <30 warn、<7 fail、解析失败=unknown（不是 fail，避免误报）
- `server`：JSch SSH 执行 `df -h /`（根分区使用率）+ `free -m`（内存使用率）；≥80% warn、≥90% fail

**日报任务** `PatrolJob`（@Scheduled 每小时整点检查 `patrol_setting.push_hour == 当前小时` 的用户，保证用户可自选推送时间且服务重启不丢）：
1. 巡检该用户全部 enabled 资产，更新 last_*
2. 汇总 markdown：异常在前（❌/⚠️ 前缀 + 详情），正常一行带过（✅ 全部正常时一句话）
3. 按 setting 通道推送；未配置 setting 的用户不发（页面仍可见 last_status）

**接口**（`PatrolController`，`/patrol/**`）：list / add / delete / runNow（立即巡检并返回结果）/ setting 读写。
**前端**：新菜单「巡检日报」（PC + H5）：资产卡片列表（状态色点 + last_detail）+ 新增抽屉（按类型动态字段）+ 设置表单。

**验收标准**：
- 加 URL 资产（如 https://www.baidu.com）→ runNow 返回 ok
- 加 domain 资产（如 baidu.com）→ WHOIS 解析出到期日
- 加 server 资产（本机 SSH）→ 返回磁盘/内存百分比
- setting 设为当前小时的下一分钟 → 下一分钟整点收到日报 → 一天后不重复收（push_hour 只在匹配小时发）
- ssh_password 落库为 SM4 密文（查库确认非明文）

## Chunk 4：O4 H5 PWA

- `static/manifest.json`：name「小泽助手」、`start_url: "./h5.html"`、`scope: "./"`、`display: standalone`、theme/background 与暗色主题一致、icons 用现有 favicon.png（192/512 两条声明，同一文件）
- `static/sw.js`：cache-first 缓存壳（h5.html/manifest/核心 css/js，版本号命名 cache）；**API 请求（含 /api/ 下非静态路径）一律不缓存**；fetch 失败回退缓存
- `h5.html`：`<link rel=manifest>`（相对路径，适配 context-path /api）+ `<link rel=apple-touch-icon>` + 注册 SW（`navigator.serviceWorker.register('./sw.js')`，try-catch 包裹，非 HTTPS/非 localhost 静默失败）
- 注意：SW 只在 localhost 或 HTTPS 下生效；当前 wxyd.us.ci 若是 HTTPS 则直接可用

**验收标准**：Chrome DevTools → Application → Manifest 无报错；SW registered；二次加载离线壳可显示；API 请求不进 cache。

## 范围外（本轮不做）

告警分级路由、静默时段、多接收人、WHOIS 多注册商深度适配（主流正则覆盖不到的显示 unknown）、agent 上报、iOS PWA 深度适配。

## 实施顺序（plan.md 展开为任务）

1. 基建：EnableScheduling + PushService + jsch 依赖 + V12 迁移脚本 + DataInitializer 自检
2. O1 后端 → 验收 → O1 前端
3. O2 后端 → 验收 → O2 前端（PC+H5）
4. O4 PWA
5. 全量回归（构建 + 双用户 curl 清单）→ review → 提交
