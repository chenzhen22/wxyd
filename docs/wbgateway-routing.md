# 模型网关路由机制（wbgateway）

> **维护约定（强制）**：任何涉及 `com.cyz.wbgateway` 包的代码改动（含 provider 新增/调整、
> 候选链逻辑、账号池、协议转换、开关语义、错误分类），**动手前必须先读本文件**；
> **改完后必须同步更新本文件**，保持与实际代码一致。详见仓库根 `CLAUDE.md` 的
> 「文档同步约束」一节。
>
> 文档基于 `V3.0` 分支、代码版本截至 2026-10-23（含提交 `8a74e20` 静默映射失败续切）。

---

## 一、整体架构

```
客户端（Anthropic 协议 / OpenAI 协议）
   │
   ▼
入口层  WbAnthropicController (/anthropic/**)  ──┐
        WbOpenAiController    (/openai/**)     ──┤ x-api-key / Bearer 鉴权
   │                                             │ 查 wb_gateway_key
   ▼                                             ▼
协议归一  WbProtocol.openAiInboundToAnthropic()
   │    （OpenAI 入站先转内部 Anthropic 形态，之后两协议共用同一管线）
   ▼
执行核心  WbGatewayChatService.execute()
   │    「模型候选链」+「对外模型名」决策 + 用量记录 + 输出
   ▼
ProviderRegistry  resolve() / candidates()
   │    跨 provider 择优 + 合并目录
   ├──► WbWorkBuddyProvider（账号池，多账号轮换）
   └──► QoderProvider       （单账号，COSY 协议）
            │
            ▼
       上游 SSE ──► WbUpstreamSseReader / QoderStream 双层解包
                ──► WbAnthropicSseWriter / WbOpenAiSseWriter 按协议输出
```

关键设计：**网关只依赖 `UpstreamProvider` 接口**（`provider/UpstreamProvider.java`）。
新增一种上游（再买个渠道、接个中转站）只需实现该接口并注册为 Spring Bean，路由层零改动。

### 源码地图

| 文件 | 职责 |
|------|------|
| `controller/WbAnthropicController.java` | `/anthropic/v1/messages` 入口，自带 key 鉴权 |
| `controller/WbOpenAiController.java` | `/openai/v1/chat/completions` 入口（兼容误配 base_url 的客户端） |
| `WbProtocol.java` | 协议互转、token 估算、消息脱毒、双协议响应组装 |
| `WbGatewayChatService.java` | **路由决策核心**：候选链、provider 选择、重试、用量记录 |
| `provider/UpstreamProvider.java` | provider 抽象（目录/归属判定/发起流/诊断） |
| `provider/ProviderRegistry.java` | provider 注册、合并目录、跨 provider 择优排序 |
| `provider/CompositeCatalog.java` | 跨 provider 同名模型合并 |
| `provider/FreePriority.java` | 免费模型手动优先级排序 |
| `provider/WbWorkBuddyProvider.java` | WorkBuddy 适配器（账号池容错在内部完成） |
| `WbWorkBuddyClient.java` | WorkBuddy HTTP 客户端、目录拉取、令牌刷新 |
| `WbBrowserLoginService.java` | 浏览器扫码/网页登录（buddy-oauth 协议），凭据注入账号池 |
| `pool/WbAccountPool.java` | 多账号池：扫描/合并/冷却/持久化/注入 |
| `pool/WbAtRestKeyService.java` | at-rest 密钥（账号池读取用） |
| `pool/WbAuthParser.java` | 桌面 App auth 文件解密解析 |
| `qoder/QoderProvider.java` | Qoder 适配器（单账号 + 双层 SSE + prefetch） |
| `qoder/QoderClient.java` | Qoder HTTP 客户端、COSY 协议、签到；凭据来源 config > device > desktop |
| `qoder/QoderDeviceLoginService.java` | Qoder device login（PKCE + poll + 凭据持久化 + rotation merge 刷新） |
| `WbUpstreamException.java` | 上游失败分类（kind）与对外 HTTP 映射 |
| `WbAnthropicSseWriter` / `WbOpenAiSseWriter` | 双协议流式输出 |
| `WbUpstreamSseReader.java` | WorkBuddy SSE 解包 |

---

## 二、两个配置开关（路由的「总阀门」）

都存在 `wb_gateway_config`（id=1），可**全局**配置，也可**按 Key 覆盖**：

| 配置 | 含义 |
|------|------|
| `model_switch` 模型开关 | **开** = 用户自选模型；**关** = 静默映射到免费模型 |
| `free_priority` 免费优先级 | 逗号分隔的手动模型顺序，管理员可控 |

**Key 级覆盖优先**（`WbGatewayChatService.java:82-85`）：

```java
Integer keyOverride = key.getModelOverride();   // 1=开 0=关 null=跟随全局
boolean switchOn = keyOverride != null ? keyOverride == 1
                                       : !Integer.valueOf(0).equals(wbGatewayMapper.queryModelSwitch());
```

此外每个 Key 还能**绑定固定模型**（`key.model`）。若 Key 绑了模型而请求带了不同的模型
→ 直接 400 `model_mismatch`；Key 未绑定 = 「跟随系统」，不校验用户请求的模型。

---

## 三、模型开关开启时（用户自选模型）

代码：`WbGatewayChatService.java:111-138`

**候选链构建：**

1. 先在合并目录里找 Key 绑定的模型，其次找请求的模型 → 命中即作为链头，
   `publicModel` = 规范展示名；
2. 都没命中 → 退回路由 provider 的默认模型；
3. **链头免费 → 到此为止**，不追加候选（`chain = [请求模型]`）；
4. **链头是计量模型 → 追加同 provider 的免费模型作为兜底链**，按 `free_priority` 排序：

```
chain = [用户要的模型, 免费模型A, 免费模型B, ...]
```

含义：用户明确要付费模型时尊重选择；真失败了（欠费/限流）再降级到免费模型顶上，
保证可用性。

**注意**：开关开启时 `allowModelFallback = !switchOn = false`，即**不允许跨 provider
静默换模型**——只在该模型确实有货的 provider 间接力（见第五节）。

---

## 四、模型开关关闭时（静默映射免费模型）

代码：`WbGatewayChatService.java:139-167`

这是「用户看不到实际模型」的模式：

1. 若请求/选中的模型**本身就是免费** → 直接用它，不映射；
2. 否则 → **静默映射**：链 = 全部 provider 的免费模型（按 `free_priority` 跨 provider
   排序），若全为空则退回路由 provider 的默认模型；
3. **响应回写用户视角的名字**（`publicModel` 仍是用户请求的那个），用户完全无感。

对应提交 `8a74e20`「静默映射支持失败续切 + 请求免费模型直连」。

---

## 五、Provider 选择：三层择优

### 5.1 合并目录 `CompositeCatalog`（同名模型只展示一条）

`provider/CompositeCatalog.java:32-69`

跨 provider 建目录时，按 `normalize()`（忽略大小写/空格/连字符/点）合并同名模型：

- `qoder:GLM-5.3` 与 `glm-5.3` → **合并为一个 Group**；
- **展示 id 规则**：有无前缀的 provider（WorkBuddy）同名时用它的原生 id
  （兼容存量 API Key），否则用去掉前缀的名字；
- 每个 Group 记住 `nativeByProvider: {workbuddy→"glm-5.3", qoder-cn→"qoder:GLM-5.3"}`，
  请求命中后按账号状况选实际 provider。

模型 id 前缀机制（`UpstreamProvider.owns()`）：带前缀的只归该 provider（Qoder 用
`qoder:`），无前缀的归无前缀 provider——**路由不硬编码厂商名**。

### 5.2 候选 provider 排序 `ProviderRegistry.candidates()`

`provider/ProviderRegistry.java:98-142`，无厂商特判的通用规则：

| 优先级 | 规则 |
|--------|------|
| 1 | **免费优先于计量** |
| 2 | 都计量时，**积分到期早的先消耗**（未知到期视为最晚） |
| 3 | 静默映射时（模型不在任何目录），**优先选手里有免费模型的 provider** |
| 4 | 同档位按**可用账号数**、**剩余积分**降序 |

不可用的（`usableAccountCount()==0` 或余额明确为 0）排到最后仅作兜底。
评分缓存 10 分钟，不会每请求都查余额。

### 5.3 最终 provider 选择 + 静默模式追加兜底

`WbGatewayChatService.java:171-218`

```java
candidates = providers.candidates(chain.get(0));      // 链头模型在哪些 provider 有货
if (!switchOn) {                                       // 静默模式：追加所有其它 provider
    for (p : providers.candidates(null)) if (!candidates.contains(p)) candidates.add(p);
    candidates.sort(按 firstChainIndex);               // 手里有更靠前免费模型的先上
}
```

然后**逐个 provider 尝试**，把展示链翻译成该 provider 的原生链
（`catalog.nativeIdFor(display, p.id())`，缺失时退回 `p.catalog().match()`）。
某个 provider 连这些模型都没有 → 静默模式下用它自己的免费模型/默认模型接住。

**失败重试的 kind 判定**（`WbGatewayChatService.java:208-214`）：

| kind | 含义 | 换下一个候选？ |
|------|------|----------------|
| `soft_rate` | 429 模型级限流 | ✅ |
| `session_dead` | 登录态失效 | ✅ |
| `server` | 上游 5xx | ✅ |
| `hard_credit` | 402 积分/当日额度用尽 | ✅（其它通道通常还能接） |
| `client` | 参数/拦截类 | 仅静默模式（用户看不到模型，被拦截也换） |
| `not_found` | 模型不存在 | ❌ 直接断 |

---

## 六、WorkBuddy 内部：账号池容错（provider 自治）

`provider/WbWorkBuddyProvider.java:110-227` —— 候选链推进在 provider 内部完成，
网关只拿最终的流。

```
for 每个候选模型:
    acquireAccount(model) ──► WbAccountPool.pick() 跳过冷却账号
         │ 无可用账号（全部冷却）→ 换下一个候选模型（不同模型限流桶不同）
         ▼
    chatStream(cred, body)
         │ 成功 → onSuccess() → break
         ▼ 失败
    session_dead → refreshAccount() 刷新令牌重试一次（refreshedOnce）
    soft_rate(429) → onModelRateLimited() 模型级冷却 60s → 换号（最多 8 次）
    hard_credit(402) → onHardCredit() 账号级冷却 5min → 换号
    其它可重试 → 换下一个候选模型
```

**冷却时长**（`WbAccountPool.java:37-42`）：模型级 429 冷却 **60s**；积分耗尽账号级
**5min**；登录态判死 **30min**（只有重登能解）。

**判死（`credential_dead`）有两条独立触发链**，都收敛到 `onCredentialDead()`：

1. 对话路径：`WbWorkBuddyProvider.open()` 内 session_dead → 刷新一次失败；
2. **积分查询路径**（无对话也会触发）：管理页轮询 `bridgeStatus()` →
   `creditsFor()` 查积分 401 → 刷新一次失败 → 判死
   （`WbWorkBuddyClient.java:219-227`）。日志关键字
   `积分查询 401 且刷新失败：<具体原因>`。

**排查判死**：看日志 `登录态被上游判死` 行的原因后缀——
`refresh 无 accessToken（需重新登录）` / `no refresh token` = 真过期，需桌面 App 重登；
每次冷却到期（30min）后立刻再次判死 = 同样是真过期（刷新永久失败）；
偶发一次、之后不再出现 = 上游抖动误判，会自愈。

账号来源：扫描桌面 App 的 auth 目录（live 文件 + 历史 `.info` 快照），按计费身份合并
去重，与本地 `data/wb-pool/accounts.json` 双向合并（刷新出的令牌跨重启存活），
TTL 5 分钟重扫。

---

## 七、Qoder 通道的特殊之处

`qoder/QoderProvider.java`

- **单账号**凭据 + COSY 协议，**双层 SSE 解包**（data 帧的 `body` 字段本身是
  OpenAI chunk JSON 字符串）；
- **`prefetch()` 预读首帧**（`QoderProvider.java:196-200`）：把上游「排队失败」信封
  （code=10605）在 open 阶段就转成 `soft_rate`，让网关**还没写响应头**就能换模型/
  换 provider。否则会变成流里的解析错误 → 502；
- **当日额度用尽 → 整天跳过本通道**（`markCreditExhausted`）：跳至次日 00:05。
  只有「daily count / billing / credit usage limit」类才算，单个模型欠费不黑掉
  整个通道。

---

## 八、错误脱敏

`WbGatewayChatService.sanitize()` —— 任何应答里的 `workbuddy`、`qoder-cn` 一律替换为
「模型服务」，客户端看不到上游厂商信息。kind → 对外 HTTP 状态映射在
`WbUpstreamException:28-34`（402/429/401/502/400）。

---

## 九、一张图总结路由决策

```
请求进来（带 key + model）
  │
  ├─ Key 绑定了模型且与请求不符 ────────────► 400 model_mismatch
  │
  ▼
switchOn = key.model_override ?? 全局 model_switch
  │
  ├─ 开（用户自选）:
  │     chain = [请求模型]            （模型免费，不追加）
  │     chain = [请求模型, +免费兜底链] （模型计量，按 free_priority）
  │     只在该模型有货的 provider 间接力，不跨 provider 静默换
  │
  └─ 关（静默映射）:
        请求模型本身免费 → chain = [该模型]，直接跑
        否则 → chain = 全部 provider 免费模型（free_priority 跨 provider 排序）
        响应 model 字段回写用户视角名，映射不可见
        追加所有其它 provider 兜底
  │
  ▼
candidates = candidates(链头) 按「免费>到期早>账号多>积分多」排序
  │
  ▼
逐个 provider 尝试（展示链 → 该 provider 原生链）
  ├─ WorkBuddy: 账号池 pick → 失败换号/刷新/冷却 → 换候选模型
  └─ Qoder:     prefetch 首帧 → 失败换模型；日额度尽则整天跳过
  │
  ├─ 全部失败 ──► 按 kind 映射 HTTP 状态（402/429/401/502）+ sanitize 后返回
  └─ 成功 ──► SSE 流按入站协议输出，同时记录 usage（wb_gateway_usage，保留 7 天）
```

---

## 十、改动本模块时的检查清单

新增/修改 provider：

- [ ] 实现 `UpstreamProvider` 全部抽象方法，注册为 `@Component`（`@Order` 决定默认排序）
- [ ] 确认 `modelPrefix()` / `owns()` 语义：带前缀则不参与同名合并
- [ ] `open()` 内部完成自己的凭据轮换与链内重试，kind 分类要准确
      （误分类会导致网关提前放弃重试）
- [ ] 流式失败若发生在**已开始出字之后**，只能中断、不能换路由
- [ ] 管理页诊断：实现 `status()` 与 `accountRows()`

改候选链 / 开关语义：

- [ ] 同时考虑开关开与关两条路径
- [ ] 静默模式下 `publicModel` 必须回写用户视角名
- [ ] 检查 `allowModelFallback` 的传参（`!switchOn`）
- [ ] 同步更新本文第三节/第四节与第九节决策图

改账号池：

- [ ] 冷却时长改动需评估对可用性的影响（见第六节表格）
- [ ] `pick()` 必须能优雅抛出 `IllegalStateException`（由 provider 转成链推进）

---

## 十一、已知注意点

1. **余额排序是近实时**：`resolve()` 与 `candidates()` 共用 `publicCatalog()` 60s TTL，
   provider 账号/额度各自 10 分钟缓存。刚用完积分可能要等一个 TTL 才会在排序里体现。
2. **静默映射时用户完全无感**，`usage` 表里 `usedModel` 记的是 `publicModel`（用户视角），
   排查实际用了哪个模型要看日志的「静默映射 ... 实际链=」。
3. `WbGatewayChatService.defaultOrFirst()` 是**死代码**，无调用方，清理时可删。
4. 对外错误信息一律过 `sanitize()`，禁止把上游厂商名/域名透传给客户端。
5. **前端拉取管理配置必须 `cache: false`**：`gateway/admin/config`、`gateway/admin/keys`
   承载开关/override 状态，`$.getJSON` 默认缓存会导致「保存后刷新回旧值」（2026-10-23 修复，
   `gateway.js` / `h5-modelmanage.js`）；新增管理员读接口时沿用此约定。

---

## 十二、账号失效的「浏览器重登」可行性（2026-10-23 研究结论）

**需求**：账号判死（登录态失效）后，不用开桌面 App，在管理页点按钮 → 浏览器完成登录 → 账号恢复。

### WorkBuddy / CodeBuddy：**可行（优先实现）**——channel-pack 的 buddy-oauth 协议

xdpool README「插件不替你登录 WorkBuddy」指的是**它自己的账号池卡片**；但 vendored
的 `vendor/channel-pack/pack.js`（`src/buddy-oauth.ts`）里有完整的**浏览器登录协议**，
登录后的账号经 `pack-accounts.ts` 桥接进同一个池，凭据结构与 wxyd 的
`WbPoolCredential` 同构。CodeBuddy 国内版与 WorkBuddy 国际版**同一套协议**，
只换 endpoint / UA / productCode（国内版与 CodeBuddy 共用端点与积分）：

```
1. POST {endpoint}/v2/plugin/auth/state?platform=ide        ← 请求头 X-Domain + X-No-Authorization/No-User-Id/
   → data: { state, authUrl }                                  No-Enterprise-Id/No-Department-Info 均 true + 产品 UA
2. 浏览器打开 authUrl（腾讯 SSO 扫码页；可附加 loginSessionId=<uuid>）
3. GET {endpoint}/v2/plugin/auth/token?state=...            ← 1s 间隔轮询，5min 超时
   code=11217 → 未就绪，continue；200+data → access_token/refreshToken/
   expiresAt/expiresIn/refreshExpiresAt/domain
4. GET {endpoint}/v2/plugin/login/account?state=...         ← Bearer <accessToken>，同样轮询
   code=12151 → 未就绪；data → uid/nickname/enterpriseId/type
5. 组装凭据（buildCredential）：access_token/refresh_token/expires_at/user_id/
   enterprise_id/nickname/domain —— 字段与 WbPoolCredential 一一对应
6. 过期时间解析顺序：expiresAt 绝对值(秒/毫秒/ISO) → expiresIn 相对秒（基于 JWT iat 或 now）
```

**产品常量**（`CODEBUDDY` / `WORKBUDDY`，pack.js 34297/34452）：

| | CodeBuddy 国内版 | WorkBuddy 国际版 |
|---|---|---|
| endpoint / apiDomain | `copilot.tencent.com` | `www.workbuddy.ai` |
| productCode | `codebuddy` | `workbuddy` |
| User-Agent | `CodeBuddyIDE/1.106.1` | `WorkBuddy/5.5.2 WorkBuddy AI/5.5.2 CLI/5.5.2`（国内系模型用 `WorkBuddy/5.5.2 WorkBuddy/5.5.2 CLI/5.5.2`） |
| refresh 端点 | `/v2/plugin/auth/token/refresh`（同 wxyd 现有 `refreshAccount`） | 同左 |

**落地要点**：
- 登录成功后把凭据写入池 store（`data/wb-pool/accounts.json` 已有双向合并 +
  saveStore 机制），live 文件优先级更高——桌面重登时自然覆盖；判死账号同 uid 重登
  会因「fresher」胜出并清冷却（`WbAccountPool.scan()` 的合并语义已支持）；
- 判死账号重登后 uid 不变 → `accountId` 不变 → 直接复活；如果上游换 uid（新账号），
  池里新增一行、旧行 7 天后过期出池；
- state 是本次登录的凭证，**不落日志**；轮询接口要在 open 阶段消费（wxyd 现有
  「预读首帧」模式同理——登录流程与对话流不同，这里本来就是 open 阶段完成）。

**已实现（2026-10-23）**：`WbBrowserLoginService` + 管理端点
`gateway/admin/browser-login/start|poll|cancel`（仅管理员）+ PC「账号池」页签 /
H5「账号池」卡片里的「🌐 扫码/网页登录」按钮。行为：

1. `start`（product=`buddy` 国内 / `workbuddy` 国际）：调上游 auth/state，返回
   `{sessionId, authUrl}`，前端引导管理员浏览器打开扫码页；
2. 前端每 2s 调 `poll`：服务端按阶段推进——先轮询 auth/token（业务码 11217=未就绪，
   继续等待），拿到 token 后再轮询 login/account（业务码 12151=未就绪）；
3. 授权完成 → `buildCredential` 组装凭据（expiresAt 绝对值(秒/毫秒/ISO) →
   expiresIn 相对秒 → JWT exp 兜底）→ **`WbAccountPool.inject()`** 注入池：
   同 uid 重登顶掉旧凭据并清全部冷却（判死账号复活），新 uid 入池为新账号，
   立即持久化到 store；
4. 会话 TTL 5 分钟，内存态；`status` 取值 pending / authorized / failed；
   sessionId/state/token 均不落日志；
5. 后续令牌刷新复用现有 `WbWorkBuddyClient.refreshAccount`
   （同一 `/v2/plugin/auth/token/refresh` 协议），桌面重登时 live 文件凭据按
   freshness 自然覆盖浏览器登录凭据（`sourcePath=browser-login`）。

### Qoder（国内版 qoder-cn）：**可行，等 WorkBuddy 做完再做**

`dsh-connect-qoder` 仓库实现了完整的 **device login** 协议
（`package/lib/index.js` 6022-6520 行 `src/host/device-login.ts`，纯协议层无依赖）：

1. 本地生成 PKCE（RFC 7636 S256，verifier 43~128 位）+ nonce + machineID(UUID)；
2. 引导用户打开 `{authBase}/device/selectAccounts?challenge=...&client_id=...`
   （CN 区 `https://qoder.cn`，clientId=`732aef47-9cf2-46a2-95fe-4cebb5d0d1fa`，
   clientMetadata: client_type=5 / business_product=cli / business_type=agent）；
3. 轮询 `{openApiBase}/api/v1/deviceToken/poll`（CN 区 `openapi.qoder.com.cn`，
   **注意 poll 打在 openApiBase 而非 authBase**）；**404 = 待授权**（正常轮询态，非错误）；
4. 授权成功 payload 里拿 `token/refresh_token/user_id/user_name/email/expires_at`；
5. 后续刷新走 `/api/v1/deviceToken/refresh`，**必须带 machineID**（poll 信封不含它，
   machineID 是本地要记住的）；rotation 要与原凭据 merge 而非替换（refresh 响应不带
   user_id，丢 uid 会导致 COSY 签名 403 Signature invalid）；
6. 拿到的 token 与桌面登录**完全等价**：COSY 签名就是
   `Bearer COSY.<payload>.<sig>`（`authHeaders`，1601-1633 行），签名输入
   `{uid, security_oauth_token, name, email}` + RSA 加密 aesKey + md5——
   device login 的 payload 字段齐备，可直接喂给 `QoderClient.authHeaders()`。

参考流程实现（`device-login-flow.ts`，8902-9040 行）：内存 session 按 region 键控、
TTL 5 分钟、前端每 2s 轮询一次「单次 poll」接口——wxyd 可照此模式做
`start → (前端轮询) poll → authorized` 三个端点，凭据落库/落文件交给现有
`QoderClient.credential()` 链路（需加一个「自持凭据」来源，优先级高于桌面文件）。

### 落地建议（Qoder 部分）

**已实现（2026-10-23）**：`QoderDeviceLoginService` + 端点
`gateway/admin/qoder/device-login/start|poll|cancel`（仅管理员，挂在
QoderAdminController）+ PC 自动化页签 Qoder 卡片 / H5 同卡片的
「🌐 扫码/网页登录」按钮。行为：

1. `start`：本地生成 PKCE（S256，verifier 43..128，66 字符字母表 byte%66 取样）
   + nonce + machineID，授权链接
   `{https://qoder.cn}/device/selectAccounts?challenge&challenge_method=S256&nonce&machine_id&client_id&client_metadata`，
   前端引导管理员浏览器打开选账号授权；
2. 前端每 2s 调 `poll`：服务端 GET
   `{https://openapi.qoder.com.cn}/api/v1/deviceToken/poll?nonce&verifier&challenge_method`
   —— **poll 打在 openApiBase**；404 / 2xx 无 token = pending（健康轮询态）；
3. 授权成功 → payload（token/refresh_token/expires_at/user_id/user_name/email，
   字段为拼写 LIST，时间三种形态：epoch ms / epoch s / ISO，1e12 阈值判单位）→
   原子写 `data/qoder/device-auth.json`（含 machineId）；
4. `QoderClient.credential()` 读取优先级 **config > device > desktop**；device 凭据
   临期（<60s）自动走 `/api/v1/deviceToken/refresh`（body 带 refresh_token +
   machine_id），**rotation 逐字段 merge 而非替换**——响应不带 user_id，丢 uid 会
   403 Signature invalid；machineId 一并写入 `Cosy-Machineid` 头；
5. 会话 TTL 5 分钟，内存态；status 取值 pending / authorized / failed；
   **verifier 是 PKCE 秘密、poll URL 含 verifier，均不落日志**。

### 落地建议（Qoder 原始方案，已被上面实现覆盖）

- 新增 `QoderDeviceLoginService`：start/poll/cancel 三个接口 + 内存 session（TTL 5min）；
- 授权成功后把 device 凭据持久化（如 `data/qoder/device-auth.json`，含 machineID），
  `QoderClient.credential()` 读取顺序：device 凭据 > 桌面 auth.v1.dat；
- 管理页（PC/H5）Qoder 卡片加「扫码/网页登录」按钮 → 弹二维码或跳转链接 →
  轮询至 authorized → 刷新状态；
- PKCE verifier 属于秘密，**绝不落日志**（协议层注释反复强调 poll URL 带 verifier）。
