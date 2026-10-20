# WorkBuddy 凭据纯 Java 化 + 多账号池（移植 dsh-workbuddy-xdpool）

日期：2026-10-20

## 背景与动机

wxyd 网关的凭据解析当前依赖 Windows node.exe 跑 win-bridge.mjs（内部 import dsh-workbuddy-connect 插件 bundle），
单账号、链路重。用户要求换成 /apps/GIT/dsh-workbuddy-xdpool（dsh-xdpool）插件的能力并尽量纯 Java 实现。
逆向其 src/at-rest.ts / accounts.ts / upstream.ts 后确认：**除「向 WorkBuddy.exe 要一次 at-rest 密钥」外全链路可纯 Java**，
凭据文件（CodeBuddyExtension/Data/Public/auth/*.info）WSL 侧直接可读，加密为 App 自带 AES-256-GCM（非 DPAPI）。
真机已校准：WorkBuddy 5.8.3 在 D:\Program Files\WorkBuddy\（注册表 DisplayIcon 可查），auth 目录 32 个 .info（多账号）。

## 决策

- **范围：完整多账号池**（xdpool 核心）：扫描全部账号快照、按身份合并、429 按模型冷却 / 401/403 会话死长冷却、自动换号。
- **密钥获取**：interop 直接 exec WorkBuddy.exe（ELECTRON_RUN_AS_NODE=1 + 内联脚本问 native binding loggerGet），
  密钥按 keyId 内存缓存、失败 60s 负缓存；可配置 wxyd.wbgateway.workbuddy-exe 覆盖。不再依赖 node.exe 与 dsh-workbuddy-connect。

## 新增（com.cyz.wbgateway.pool）

| 类 | 职责 |
|---|---|
| WbAtRestKeyService | 找 WorkBuddy.exe（配置覆盖 → powershell.exe 查注册表 → 固定候选路径/盘符一级扫描）→ interop exec 拿 payload → key=sha256(base64串) keyId=sha256(key)前16hex → Map 缓存 |
| WbAtRestCrypto | envelope 解密：base64→JSON{suite,keyId,nonce,authTag,ciphertext}，AES-256-GCM，AAD 转录 = "WB-AAD\0"‖0x01‖lp("WBEV1")‖lp("sym-v1")‖u32(suite)‖lp(keyId)‖0x02‖0x00‖0x00，nonce 12B、tag 16B |
| WbAuthParser | .info 解析：auth/account 分层，plain 字符串与 $wbEncrypted 包装兼容；expiresAt/refreshExpiresAt/lastRefreshTime 秒毫秒归一；refreshExpiresAt 已过期整档跳过 |
| WbPoolCredential / WbPoolAccount | 凭据数据 + 池内状态（冷却/按模型冷却/会话死/最近成功） |
| WbAccountPool | 扫描 auth 目录（/mnt/c/Users/*/AppData/{Local,Roaming}/CodeBuddyExtension/Data/Public/auth；Windows/Linux 路径同理）→ accountId=sha256("workbuddy\0"+uin‖uid‖nickname)前16 合并去重、freshness 排序（live > lastRefreshTime > expiresAt）；与本地持久化 store（data/wb-pool/accounts.json，原子写）合并（刷新后的令牌跨重启存活）；pick() 跳过冷却账号 |
| （client 内）刷新 | POST {chatBase}/v2/plugin/auth/token/refresh，头 = common + X-Refresh-Token + X-Auth-Refresh-Source: workbuddy |
| （client 内）积分 | POST {billingBase}/v2/billing/meter/get-user-resource（ProductCode p_tcaca，CapacityType 4=月度包取 CycleCapacity*），每账号积分缓存 10 分钟 |

## 改造

- **WbWorkBuddyClient**：删 win-bridge/node/bridge 全部代码与配置（win-node/win-bridge-script/bridge-ttl/bridge-timeout/electron-bin）；
  resolveCredential() → 池选号 + 临期刷新；session_dead → 刷新一次，仍死则该号 credentialDeadUntil=30min 换下一号；
  bridgeStatus() → 池概览（每账号 label/uid/domain/积分/到期/冷却），60s 缓存。
- **WbGatewayChatService**：重试循环改为「凭据级重试」：session_dead/限流 → 池标记 + 换号重试（上限=池内健康号数），模型候选链逻辑不变。
- **前端** gateway.js / api.html：状态徽章改池视角（健康号数/总积分），新增池内账号表（账号、域名、积分、令牌到期、状态/冷却原因）；gateway.js v9→v10。

## 冷却策略（照 xdpool）

| 失败 | 处理 |
|---|---|
| 429 | 该账号该模型冷却 60s（同账号其他模型继续可用） |
| 402/积分耗尽 | 账号级冷却 5 分钟 |
| 401/session_dead | 先刷新令牌重试一次；仍死 → credentialDeadUntil 30 分钟（只有重新登录能解） |
| 刷新成功 | 清该账号全部冷却 |

## 非目标

- 不做扫码登录/积分自动化/聚合渠道（xdpool 其余页面）
- 不做模型链路由与 billion-context
- 国际版（WorkBuddyAI）凭据随扫描自然进池，不单独适配页面