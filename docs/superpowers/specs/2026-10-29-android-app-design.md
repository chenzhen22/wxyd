# 小泽助手 Android 原生 App 设计文档

- **日期：** 2026-10-29
- **作者：** 设计会话（Claude）
- **状态：** 已批准（2026-10-29 用户批准；builder check 完成，D7–D12 决策已落档）
- **目标仓库：** `/apps/GIT/wxyd`（后端不动，新增独立安卓模块）

---

## 1. 意图与为什么（Intent & Why）

把现有「小泽助手」的 **H5 移动端前端** 搬到 **安卓原生 App**。后端 Spring Boot（`/api/*`）零改动，安卓端作为新的前端客户端直连。

动机：H5 在手机上体验受限（手势、动画、离线、系统级集成弱）。用原生重写后，能拿到 Material 3 的规范视觉、系统级分享/下载、下拉刷新、性能与离线能力，同时复用已稳定的后端接口与鉴权体系，风险低。

---

## 2. 目标 / 非目标 / 反目标（Goals / Non-Goals / Anti-Goals）

### 目标
- 安卓原生（Kotlin + Jetpack Compose）实现 H5 保留功能，直连现有 `/api/*`。
- 后端零改动：不改 controller、不改接口契约、不改鉴权。
- 首页功能卡片**数据驱动**于 `/api/menu/list?platform=h5`（与 H5 一致，不硬编码）。
- 保留普通用户功能；管理员高级功能首版暂缓。
- 首版含**账号体系完整闭环**：密码登录、邮箱注册、找回密码（注册/找回的滑块验证码在原生端复刻）。

### 非目标（首版明确不做）
- 留言板、群聊、巡检日报、证据材料整理、Dubbo 调用、归档下载 —— **直接砍掉，不移植**。
- 用户管理、菜单管理、登录日志、SQL 查询 —— **暂缓**（首版不开发，不为其留复杂脚手架）。
- 批次任务（batchTask）、模型管理（modelManage）—— 管理员模块，**首版暂缓**。
- GitHub / Gitee OAuth 登录 —— **暂缓**（首版仅账号密码登录；OAuth 需 Custom Tab + 回调 deep link，后置）。

### 反目标（即使技术可行也不做）
- 不引入 WebView 壳（用户已选纯原生）。
- 不改造后端接口契约（分页/字段精简等留给以后单独评估）。
- 不把安卓做成"超管后台"——首版只服务普通用户 + 轻量管理员可见项（如 share 上传）。

---

## 3. 首版功能范围（保留模块）

| 模块 | key（menu_key） | 入口 | 说明 |
|------|----------|------|------|
| 工作台/首页 | home | 底部 Tab | 欢迎区 + 实时时钟 + 功能卡片网格 |
| 我的 | profile | 底部 Tab | 账号、主题切换、退出、关于 |
| 福利专区 | welfare | 卡片 | 帖子列表（分页/搜索）+ 详情(MD) + 羊毛雷达订阅 |
| 金融日报 | dayinfo | 卡片 | 日报列表 + 最新一期详情 + 订阅管理 |
| 模型网关 | gateway | 卡片 | API Key 管理（仅用户侧 key，不含超管 admin/*） |
| 钉钉机器人 | robot | 卡片 | 机器人 CRUD + 发送文本/文件 |
| 记忆笔记 | notes | 卡片 | 我的/公开笔记、搜索、Markdown 编辑、导出 |
| 文件共享 | share | 卡片 | 列表/下载；上传/删除仅超管 |
| Shell 脚本 | shell | 卡片 | 只读学习：主题列表 + 详情 + 存笔记 |
| Java API | javaapi | 卡片 | 类列表 + 详情 + 在线运行测试用例 |
| （登录页） | — | 启动入口 | 密码登录；无账号提示引导至注册 |
| （邮箱注册） | — | 登录页链接 | 滑块验证码 → 邮箱验证码 → 设置账号（三步向导） |
| （找回密码） | — | 登录页链接 | 滑块验证码 → 邮箱验证码 → 重置密码（三步向导） |

> 底部 Tab：因留言板/群聊被砍，固定为 **2 个 Tab（工作台 / 我的）**。其余模块全部由首页卡片进入（与 H5 架构一致）。
>
> **羊毛雷达澄清**（builder check 第 6 点）：`/radar/*` 不是独立模块，**没有自己的 menu_key**——它是 `welfare` 页内的"订阅管理"子区块（H5 同构：welfare 列表页内嵌雷达订阅列表/新增/启停/测试）。安卓端照此实现，不新增首页卡片。

---

## 4. 约束（Constraints）

1. **鉴权：** HttpSession（Cookie / JSESSIONID）。登录后持有会话 Cookie 随请求发送；401 = 会话过期需重登。
2. **请求头：** 所有请求带 `X-Requested-With: XMLHttpRequest`，否则 AuthInterceptor 对未登录返回 302 而非 401 JSON。
3. **响应契约：** 统一 `Result { errorCode, errorMsg, body }`，成功 `errorCode=="000000"`。错误按 errorCode/errorMsg 处理。
4. **请求体三类格式（关键，易错）：**
   - **多数业务 POST**：`{ "body": { ...实际字段 } }` 双层（welfare 雷达、dayinfo 订阅、gateway、note、share/delete、auth/*、radar/*）。
   - **直接字段 JSON**：`robot/add|update|delete`、`login`（虽包 Result 但内层 body 即字段）、`javaapi/.../test`、`archive/fetch`。
   - **multipart/form-data**：`robot/send`、`share/upload`。
5. **裸对象/数组接口（不包 Result）**：`shellscript/topics*`、`javaapi/classes*`（注意 `/test` 入参直接字段、返回 `ResponseEntity<CodeResult>`）。
6. **文件下载流（非 JSON）**：`share/download`、`note/export`、`note/exportAll` —— 用下载器，注意 404 表示无权/不存在。
7. **登录流程：** `POST /api/login`，请求体 `{body:{username,password}}`，返回完整 User（id/username/role）。登录后调 `GET /api/user/info` 补全 isAdmin。
8. **首页驱动：** 卡片完全来自 `GET /api/menu/list?platform=h5`（已按 sortNo 排序、已过滤权限/管理员项）；`hashUrl` 非空才出卡片，`adminOnly==1` 显示管理员角标；`home`/`profile` 固定可见。
9. **权限 UI 控制：** 据 `user/info` 的 role/isAdmin：gateway key 每日失效需每日启用、share 上传/删除仅超管、note 私有/他人只读规则（他人公开笔记仅标题可搜）。
10. **SM4：** 后端已处理加解密；安卓端**不碰 SM4**，直接消费明文 / reveal 返回值。密码以明文随 `{body:{}}` 发送（与 H5 行为一致，依赖传输层安全）。
11. **构建环境：** 当前机器 JDK 8；安卓 Kotlin/Compose 编译需 **JDK 17**，构建环境由用户外部提供（见第 8 节）。
12. **网络明文/自签（已定：支持）：** 部署环境可能是内网 HTTP 明文或自签证书。`network_security_config.xml` 允许 cleartext（`cleartextTrafficPermitted=true`）并信任用户安装的 CA（`trust-anchors` 含 `system` + `user`），Base URL 可填 `http://`。不做证书锁定（pinning）。
13. **滑块验证码（原生复刻）：** 注册/找回发码前置人机校验。契约：`GET captcha/slider` → `Result{body:{captchaId, bg(base64 png), piece(base64 png), y}}`；用户拖动拼图块到正确 x 后 `POST captcha/verify` `{body:{captchaId, x}}` → `Result{body:{ticket}}`；`ticket` 一次性，随 `register/email/send` / `password/reset/send` 提交。原生实现：Compose `Canvas` 绘制底图 + 拼图块，`pointerInput` 水平拖拽，松手即 verify；失败刷新滑块。
14. **注册/找回三步向导：** 注册 = 滑块 → `register/email/send {body:{email,ticket}}` → `register/email/verify {body:{email,code}}` 得一次性 `registerToken` → `register/email/complete {body:{registerToken,password,username?}}`（成功即登录，种会话）。找回 = 同构：`password/reset/send/verify/complete`。60 秒重发限制由服务端管，前端倒计时展示。

---

## 5. 方案（Approaches）

### 候选方案
- **A. 原生 UI + 直连后端（采纳）**：Kotlin + Jetpack Compose 重写界面，OkHttp 直连 `/api/*`。后端零改动。最贴合"原生 App 搬 H5"诉求。
- B. WebView 壳内嵌 h5.html：最快但非原生体验，被否决。
- C. 原生 + 后端移动端改造：超出"搬前端"范围，被否决。

### 推荐方案 A 的分层（构建者细化空间）
- **网络层**：OkHttp（CookieJar 持久化 JSESSIONID）+ 统一 `Result` 解析 + 三类请求体序列化封装 + `X-Requested-With` 拦截器。Retrofit 可选（手动 OkHttp 亦可，因契约不规则，手写适配更直观）。
- **UI 层**：Jetpack Compose + Material 3（Navigation Compose 管理页面栈；首页 `LazyVerticalGrid` 卡片网格；次级页统一顶栏 + 返回）。
- **状态/存储**：DataStore 存主题/登录态/服务器地址；ViewModel 承载各模块状态；Coroutine + Flow。
- **主题**：Material 3 动态取色（Android 12+）+ 跟随系统/浅色/深色开关，持久化到 DataStore（对应 H5 多主题，首版可简化为系统/浅/深三态，原 7 套主题不强求）。
- **Markdown 渲染**：引入 Compose Markdown 库（或 WebView 局部渲染）呈现 welfare/dayinfo/note 详情。
- **文件**：下载用系统下载管理器 / OkHttp 流式落盘；上传 multipart。

### 界面范式（已联网调研受阻，基于 Material 3 规范给出）
> 注：本会话环境联网抓取被网络策略拦截，无法实时核对网页。以下基于 Material Design 3 公开规范与同类 App（钉钉工作台、企业微信、支付宝首页）公认范式，属可靠设计常识，待网络放行后逐条复核。

- **导航**：固定 2 底部 Tab（工作台 / 我的），3–5 个目的地内的标准做法。次级页走单一页面栈（`NavHost` + `navigate`/`popBackStack`），顶栏含返回箭头 + 标题，不给次级功能加底部 Tab。
- **首页（工作台）**：顶部欢迎区（"你好，{username} 👋"）+ 实时时钟（`LaunchedEffect` + `delay` 每秒刷新）+ 功能卡片网格 `LazyVerticalGrid`（`GridCells.Adaptive(minSize=120.dp)`，约 2–3 列）。卡片来自 `menu/list`：图标（emoji 直接作 `Text`）、名称、`adminOnly==1` 显示"管理员"角标（小 `SuggestionChip`）。点击卡片 `navigate` 到对应模块路由。
- **次级页骨架**：统一 `Scaffold`（顶栏标题 + 返回 + 可选右侧刷新/更多）+ `LazyColumn` 列表或详情；下拉刷新（`PullToRefreshBox`，Material 3 1.2+）取代"刷新"按钮；列表项支持 `SwipeToDismissBox` 删除 + `Snackbar` 撤销；新增/编辑表单用 `ModalBottomSheet` 取代居中对话框；空状态用居中图标 + 引导文案（`Lottie`/`Compose` 轻插画可选，首版用 Icon+Text 即可）。
- **FAB**：模块需要"新增"时用 `FloatingActionButton`（对应 H5 右下角 ＋）。
- **主题**：**做全 7 套**（dark/light/eyecare/techblue/orange/pink/system，对齐服务端 `user/updateTheme` 只认的 7 值；默认 dark 对齐 H5）。每套定义 light+dark 两个 `ColorScheme`（`system` 跟随系统在两套间切换），紫罗兰 `#a78bfa` 系列为静态兜底（Android 12+ 动态取色可选叠加，`system` 态下启用）。主题选择持久化到 `DataStore` 并同步 `user/updateTheme`（与 H5 跨端一致）；`user/theme` 返回 7 值外的存储时 coerce 到 `dark`。
- **Toast / Loading**：用 Compose `SnackbarHost` 取代 H5 toast；加载用 `CircularProgressIndicator` 居中遮罩。
- **颜色 token 对齐**：尽量把 H5 的暗色玻璃拟态（`--bg-gradient` 紫蓝渐变、`--accent` 紫罗兰、卡片半透明描边）映射到 Material 3 `ColorScheme` + 适度 `surfaceVariant`/透明描边，保证视觉连续性（非像素级，首版求神似）。

---

## 6. 已决策的实现要点（Decisions）

| # | 决策 | 理由 |
|---|------|------|
| D1 | 纯原生 Compose，不 WebView | 用户明确选 A |
| D2 | 后端零改动 | 降风险，复用稳定接口 |
| D3 | 卡片数据驱动 menu/list | 与 H5 一致，运维可配显隐 |
| D4 | 底部 2 Tab | 留言板/群聊砍掉，仅剩工作台/我的 |
| D5 | 三类请求体封装 | 契约不规则，集中处理避免散落 bug |
| D6 | 首版不碰 SM4 | 后端已处理，安卓只消费 |
| D7 | 主题做全 7 套 | 对齐服务端 7 值，H5 用户跨端不丢样式（2026-10-29 用户确认） |
| D8 | 登录仅账号密码 | OAuth 需 Custom Tab + deep link，架构重，后置（2026-10-29 用户确认） |
| D9 | 注册+找回进首版 | 账号闭环完整；含原生滑块验证码复刻（2026-10-29 用户确认） |
| D10 | 支持 HTTP 明文 + 自签 | 内部部署现实；network-security-config 放行（2026-10-29 用户确认） |
| D11 | Gradle 独立工程 /android/ | 自带 settings.gradle + gradlew，clone 即可构建（builder check #4 收敛） |
| D12 | radar 为 welfare 子区块 | 菜单无独立 radar key，H5 同构（builder check #6 澄清） |

---

## 7. 实现选型结论（已定，原"构建者细化空间"收敛）

> 设计阶段已为以下开放项给出默认，构建者按此落地即可，无需再议。

- **网络层**：**手写 OkHttp + 轻封装**（不用 Retrofit）。理由：接口契约不规则（三类请求体、裸数组/裸对象、文件流），手写 `ApiClient` 集中处理 `Result` 解析 + `X-Requested-With` 拦截器 + `CookieJar` 持久化 JSESSIONID + 三类 body 序列化，比 Retrofit 注解拼凑更直观、bug 更少。
- **Markdown 渲染**：引入 Compose Markdown 库（如 `com.github.jeziellago:compose-markdown` 或 `alandev:markdowntext` 等维护中方案），在 Compose 内直接渲染 welfare / dayinfo / note 详情。**不走 WebView 局部渲染**（与"纯原生"一致）。
- **大文件分片上传**：首版**不实现** `share/uploadChunk`；普通 `multipart` 上传即可覆盖日常小文件场景。分片作为后续优化。
- **离线缓存**：首版**仅**持久化会话 Cookie（登录态）+ 主题 + 服务器地址（`DataStore`）。模块数据不本地缓存（Room 暂不引入）。
- **服务器地址**：App 内可配置 Base URL（`DataStore` 存，默认留空引导用户在"我的"里填 `/api` 的部署地址），便于连不同环境。
- **视觉细节**：颜色 token / 间距 / 图标（emoji 作图标，免引入图标库依赖）首版求"神似" H5 暗色玻璃拟态，不追求像素级。

---

## 8. 已确认的环境与位置决策

> 以下两项在 2026-10-29 设计会话中由用户拍板。

- **源码位置（已定）**：`/apps/GIT/wxyd/android/` 为一个**完全独立的 Gradle 工程**（自带 `settings.gradle.kts`、`gradle/wrapper`、`gradlew`），与 Maven 后端（仓库根 `pom.xml` → `app`）互不感知、互不影响构建。Maven 根目录不会引入 Gradle 文件；在 `android/` 目录内执行 `./gradlew assembleDebug` 即可出包。此为 builder check #4 的收敛结论（Maven 根 + Gradle 兄弟目录无法构成同一构建，必须各自独立）。
- **构建环境（已定：外部提供）**：当前开发机仅 JDK 8、无 Android SDK / Gradle / Kotlin，无法在本机 `assembleDebug` 出 APK，也无法跑模拟器。
  - 方案定为 **(a)/(c)**：本会话负责把 Kotlin 代码与 Gradle 配置**写正确**（含离线静态校验），实际编译 / 出包 / 真机运行由用户在其已装 **JDK 17 + Android SDK（Android Studio 或命令行）** 的环境或 CI 上完成。
  - 因此本文档不假设本机可构建；所有 Gradle 配置以"在合规环境 clone 后可直接 `./gradlew assembleDebug`"为标准编写。
- **模块范围（已定：仅普通用户模块）**：用户对"全量"的澄清为**只搬普通用户侧功能**。由此 `modelManage`（模型管理 / `gateway/admin/**` 超管控制台）与 `batchTask`（批次任务）维持"首版暂缓"判断，不纳入安卓端；与第 2、3 节一致，无需改动。

## 9. 残留风险

- **UI 视觉稿**：建议后续（实现前）先出原型让用户挑像素级细节；本设计基于 Material 3 规范与同类 App 范式，非实时联网核对（环境网络策略拦截）。
- **构建验证缺口**：因本机不可构建，代码正确性依赖静态检查 + 严格的接口契约对齐；首版交付后需用户在合规环境完成首次构建与真机冒烟。
- **大文件分片上传**：`share/uploadChunk` 是否首版实现见第 7 节构建者细化空间。

---

## 附录：接口速查（来自 H5 源码核对）

> 完整字段见子 agent 调研报告，要点如下。

- **登录**：`POST /api/login` `{body:{username,password}}` → User（再 `GET /api/user/info` 补 isAdmin）。
- **菜单**：`GET /api/menu/list?platform=h5` → `[{key,name,icon,hashUrl,sortNo,adminOnly,...}]`（字段名是 `key`，非 `menu_key`）。
- **welfare**：`/welfare/list`(GET 分页/搜索)、`/welfare/detail`(GET)、`/radar/*`(订阅)。
- **dayinfo**：`/dayinfo/list|latest|detail`(GET)、`/dayinfo/subscribe/*`(POST `{body:{}}`)。
- **gateway**：`/gateway/keys`(GET)、`/gateway/keys/{add,toggle,delete,reveal,model}`(POST `{body:{}}`)、`/gateway/models`(GET)。
- **robot**：`/robot/list`(GET)、`/robot/{add,update,delete}`(POST 直接字段)、`/robot/send`(multipart)。
- **notes**：`/note/list|search|get|categories|tags`(GET)、`/note/{save,delete,saveFromSource,tags/rename}`(POST `{body:{}}`)、`/note/export|exportAll`(文件流)。
- **share**：`/share/list`(GET)、`/share/upload`(multipart,超管)、`/share/download`(文件流)、`/share/delete`(POST `{body:{}}`)。
- **shell**：`/shellscript/topics`(裸数组 GET)、`/shellscript/topics/{name}`(裸对象 GET)。
- **javaapi**：`/javaapi/classes`(裸 List GET)、`/javaapi/classes/{name}`(GET)、`/javaapi/classes/{name}/test`(POST 直接字段 `{testName,code}`)。
