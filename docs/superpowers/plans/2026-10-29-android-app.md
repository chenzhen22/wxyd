# 小泽助手 Android 原生 App 实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 用 Kotlin + Jetpack Compose 把小泽助手 H5 前端搬成安卓原生 App,直连现有 `/api/*`,后端零改动。

**Architecture:** `/apps/GIT/wxyd/android/` 下一个完全独立的 Gradle 工程(与 Maven 根互不感知)。分层:手写 OkHttp `ApiClient`(CookieJar 持久化 JSESSIONID + `X-Requested-With` 拦截器 + 统一 `Result` 解析 + 三类请求体封装)→ ViewModel(Coroutine/Flow)→ Compose + Material 3 UI(Navigation Compose 单页面栈,底部 2 Tab,首页卡片数据驱动 `menu/list`)。

**Tech Stack:** Kotlin、Jetpack Compose + Material 3、Navigation Compose、OkHttp、DataStore(Preferences)、Compose Markdown 库(`com.github.jeziellago:compose-markdown`)。不用 Retrofit、不用 WebView、不用 Room。

**Spec:** `docs/superpowers/specs/2026-10-29-android-app-design.md`(接口契约以该文档附录 + H5 源码 `app/src/main/resources/static/h5.html` 为准)

## Global Constraints

- **本机无构建环境**(仅 JDK 8,无 Android SDK/Gradle/Kotlin,见 spec 第 8 节):所有任务的验证步骤为**静态校验**(grep 比对 H5 源码契约、XML/kts 语法自查),不是可运行测试。编译出包由用户在 JDK 17 + Android SDK 环境执行 `./gradlew assembleDebug` 完成。**不要尝试在本机安装/运行 Gradle 或下载 Android SDK。**
- 代码写在 `/apps/GIT/wxyd/android/`,一个独立 Gradle 工程:自有 `settings.gradle.kts`、`gradle/wrapper`、`gradlew`;仓库根 Maven `pom.xml` 不改、不引用 android 目录。
- 包名 `com.cyz.wxyd`;minSdk 26,targetSdk/compileSdk 34;Kotlin 2.0.x + Compose BOM。
- 所有请求带 `X-Requested-With: XMLHttpRequest`;会话 Cookie 持久化;401 → 清会话回登录页。
- 统一响应 `Result { errorCode, errorMsg, body }`,成功 `errorCode=="000000"`。三类请求体见 spec 约束 4,**逐接口对齐,不得混用**。
- 请求体双层包裹 `{ "body": { ... } }` 的接口:welfare 雷达、dayinfo 订阅、gateway、note、share/delete、auth/*、radar/*、login;直接字段:robot/add|update|delete、javaapi/.../test;multipart:robot/send、share/upload。裸对象/数组(不包 Result):shellscript/topics*、javaapi/classes*。
- 不碰 SM4;密码明文随 `{body:{}}` 发送。
- 明文 HTTP + 自签:`network_security_config.xml` 允许 cleartext、信任 system+user CA;Base URL 用户可配。
- 主题 7 值:`dark/light/eyecare/techblue/orange/pink/system`,默认 `dark`,持久化 DataStore 并同步 `user/updateTheme`;`user/theme` 返回 7 值外 coerce 到 `dark`。
- 卡片数据驱动 `GET /api/menu/list?platform=h5`,`hashUrl` 非空才出卡片,`adminOnly==1` 显示管理员角标;`home`/`profile` 固定可见,底部固定 2 Tab。
- radar 是 welfare 页内子区块,不新增首页卡片。
- 图标用 emoji 直接 `Text` 渲染,不引入图标库。
- 首版不做:留言板/群聊/巡检/证据/归档、admin 模块、OAuth、分片上传、Room 离线缓存。

## Review Focus

1. **三类请求体格式混用**(spec 约束 4,最易错):robot 接口若误包 `{body:{}}` 会全挂。每任务验证步骤包含对照 H5 源码 grep 该模块接口的实际 payload 封装;Task 2 的统一封装测试列出逐接口格式表。
2. **会话过期路径**:任何请求返回 401 时必须清 Cookie 并跳登录,而不是弹错误 Toast。Task 2 的 ApiClient 集中拦截;Task 4 登录态 Flow 驱动全局导航。
3. **menu/list 字段名是 `key` 不是 `menu_key`**(spec 附录明确),且 `hashUrl` 空值过滤。Task 7 验证步骤 grep H5 的菜单渲染代码核对字段名与过滤条件。
4. **滑块验证码 ticket 一次性**:verify 成功后 ticket 只能用一次,send 失败必须重新走滑块而非复用 ticket。Task 5 的状态机处理 verify→send 的生命周期。
5. **文件下载 404 语义**(share/download、note/export):404 = 无权/不存在,要给用户可读提示而非崩溃/空文件。Task 12 下载器统一处理。

---

### Task 1: Gradle 工程骨架

**Files:**
- Create: `android/settings.gradle.kts`、`android/build.gradle.kts`、`android/gradle.properties`、`android/gradle/libs.versions.toml`
- Create: `android/app/build.gradle.kts`、`android/app/src/main/AndroidManifest.xml`
- Create: `android/app/src/main/res/xml/network_security_config.xml`
- Create: `android/app/src/main/java/com/cyz/wxyd/WxydApp.kt`(Application 类,空壳)
- Create: `android/gradle/wrapper/gradle-wrapper.properties`(wrapper jar 由用户环境 `gradle wrapper` 生成或提交标准 jar)

**Interfaces:**
- Produces: 可被后续任务填充的工程骨架;`WxydApp` Application 类(manifest 注册);依赖版本目录 `libs.versions.toml`(compose-bom、okhttp、datastore、navigation-compose、compose-markdown、coil 可选)。

- [ ] **Step 1: 写 settings/根 build/版本目录**

`settings.gradle.kts` 仅含 `include(":app")` + `pluginManagement`(google/mavenCentral/`maven("https://jitpack.io")`,compose-markdown 在 jitpack)。`libs.versions.toml` 钉版本:AGP 8.5.x、Kotlin 2.0.x、compose-bom 2024.xx、okhttp 4.12、datastore 1.1.x、navigation-compose 2.7.x、jeziellago compose-markdown 0.5.x。

- [ ] **Step 2: 写 app/build.gradle.kts 与 manifest**

`com.android.application` + `kotlin("android")` + `kotlin("plugin.compose")`;`minSdk=26`、`compileSdk=34`、`targetSdk=34`。manifest:`INTERNET` 权限、`android:networkSecurityConfig="@xml/network_security_config"`、`android:name=".WxydApp"`、启动 Activity 占位 `MainActivity`(Task 4 实装)。

- [ ] **Step 3: 写 network_security_config.xml**

```xml
<network-security-config>
    <base-config cleartextTrafficPermitted="true">
        <trust-anchors>
            <certificates src="system" />
            <certificates src="user" />
        </trust-anchors>
    </base-config>
</network-security-config>
```

- [ ] **Step 4: 静态校验**

Run: `grep -c "cleartextTrafficPermitted" android/app/src/main/res/xml/network_security_config.xml && grep -c "WxydApp" android/app/src/main/AndroidManifest.xml && grep -c "jitpack" android/settings.gradle.kts`
Expected: 3 行,各 ≥1。人工通读 kts 文件确认无语法错误(括号/引号闭合)。

- [ ] **Step 5: Commit**

```bash
cd /apps/GIT/wxyd && git add android/ && git commit -m "feat(android): 独立 Gradle 工程骨架"
```

---

### Task 2: 网络层 ApiClient + 本地存储

**Files:**
- Create: `android/app/src/main/java/com/cyz/wxyd/data/remote/ApiClient.kt`
- Create: `android/app/src/main/java/com/cyz/wxyd/data/remote/Result.kt`
- Create: `android/app/src/main/java/com/cyz/wxyd/data/remote/RequestBodyFactory.kt`
- Create: `android/app/src/main/java/com/cyz/wxyd/data/local/SettingsStore.kt`
- Create: `android/app/src/main/java/com/cyz/wxyd/data/local/SessionCookieJar.kt`

**Interfaces:**
- Produces:
  - `class ApiClient(baseUrl: String, cookieJar: SessionCookieJar)`,方法:
    - `suspend fun getResult(path: String, query: Map<String,String> = emptyMap()): JsonObject` — GET,解析 `Result`,errorCode != "000000" 时抛 `ApiException(errorCode, errorMsg)`;401 抛 `SessionExpiredException`
    - `suspend fun postBody(path: String, bodyFields: JsonObject): JsonObject` — POST `{"body": {...}}`
    - `suspend fun postDirect(path: String, fields: JsonObject): JsonObject` — POST 直接字段 JSON
    - `suspend fun postMultipart(path: String, fields: Map<String,String>, files: Map<String,File>): JsonObject`
    - `suspend fun getRaw(path: String, query: Map<String,String> = emptyMap()): Response` — 裸对象/数组接口与文件流用
  - `class ApiException(val errorCode: String, val errorMsg: String): Exception`;`class SessionExpiredException: Exception`
  - `class SettingsStore(context)`:`baseUrlFlow: Flow<String>`、`suspend fun setBaseUrl(v: String)`、`themeFlow: Flow<String>`、`suspend fun setTheme(v: String)`、`suspend fun clearSession()`(清 Cookie)
  - `class SessionCookieJar: CookieJar` — 内存 + 文件持久化 JSESSIONID
- Consumes: Task 1 的依赖版本与 `WxydApp`。

- [ ] **Step 1: 写 Result.kt(响应模型 + 异常)**

`Result(errorCode, errorMsg, body: JsonObject?)`,JSON 解析用 `kotlinx.serialization` 或 `org.json`(与 Compose 工程共存选 kotlinx-serialization-json,在 Task 1 的 toml 中已有则直接用,否则此处补依赖声明)。

- [ ] **Step 2: 写 SessionCookieJar + SettingsStore(DataStore)**

CookieJar:响应头 `Set-Cookie` 中的 `JSESSIONID` 存内存 Map + Preferences DataStore;`loadForRequest` 回填。SettingsStore 按 Interfaces 签名实现。

- [ ] **Step 3: 写 RequestBodyFactory(三类请求体)**

按 Global Constraints 的三类格式逐接口写注释表(哪些 path 用双层/直接/multipart),`postBody`/`postDirect`/`postMultipart` 是唯一入口,业务层不得自己拼 JSON body。

- [ ] **Step 4: 写 ApiClient**

OkHttp 单例拦截器:①加 `X-Requested-With: XMLHttpRequest`;②Base URL 从 `SettingsStore` 读取拼接;③响应 401 → 抛 `SessionExpiredException`;④`application/json; charset=utf-8`。

- [ ] **Step 5: 静态校验**

Run: `grep -rn "X-Requested-With" android/app/src/main/java/com/cyz/wxyd/data/remote/ && grep -rn "000000" android/app/src/main/java/com/cyz/wxyd/data/remote/Result.kt && grep -rn "SessionExpiredException" android/app/src/main/java/com/cyz/wxyd/data/remote/ApiClient.kt`
Expected: 三处命中。人工通读 `RequestBodyFactory` 注释表与 spec 约束 4 逐行比对(双层/直接/multipart 三列)。

- [ ] **Step 6: Commit**

```bash
cd /apps/GIT/wxyd && git add android/ && git commit -m "feat(android): OkHttp 网络层 + 三类请求体封装 + DataStore"
```

---

### Task 3: 主题系统(7 套)

**Files:**
- Create: `android/app/src/main/java/com/cyz/wxyd/ui/theme/Themes.kt`
- Create: `android/app/src/main/java/com/cyz/wxyd/ui/theme/ThemeController.kt`

**Interfaces:**
- Produces:
  - `val ALL_THEMES = listOf("dark","light","eyecare","techblue","orange","pink","system")`
  - `fun colorSchemeFor(theme: String, darkSystem: Boolean): ColorScheme` — 7 值外/`system` 按 `darkSystem` 落到 dark 或 light;每套基于紫罗兰 `#a78bfa` 系列 + H5 暗色玻璃拟态映射(见 spec 5 节"主题")
  - `class ThemeController(settings: SettingsStore)`: `themeFlow: Flow<String>`、`suspend fun setTheme(v: String)`(写 DataStore + 调 `user/updateTheme` postBody `{theme}`,失败不阻塞本地生效)
- Consumes: Task 2 `SettingsStore`、`ApiClient`。

- [ ] **Step 1: 写 Themes.kt(7 套 ColorScheme)**

每套 light+dark 两个 `ColorScheme`(Material3 `lightColorScheme`/`darkColorScheme` 参数化主色),默认 dark 主色 `#a78bfa`。共 14 个 scheme + `colorSchemeFor` coerce 逻辑。

- [ ] **Step 2: 写 ThemeController**

按 Interfaces 签名;`setTheme` 先写本地,再 fire-and-forget 调 `POST /api/user/updateTheme` `{body:{theme}}`(ApiClient.postBody)。

- [ ] **Step 3: 静态校验**

Run: `grep -c "ColorScheme" android/app/src/main/java/com/cyz/wxyd/ui/theme/Themes.kt && grep -rn "updateTheme" android/app/src/main/java/com/cyz/wxyd/ui/theme/ThemeController.kt && grep -rn '"dark"' android/app/src/main/java/com/cyz/wxyd/ui/theme/`
Expected: 命中;确认 `coerce` 兜底默认 `"dark"`。

- [ ] **Step 4: Commit**

```bash
cd /apps/GIT/wxyd && git add android/ && git commit -m "feat(android): 7 套主题 ColorScheme + ThemeController"
```

---

### Task 4: 导航骨架 + 登录 + 会话状态

**Files:**
- Create: `android/app/src/main/java/com/cyz/wxyd/MainActivity.kt`
- Create: `android/app/src/main/java/com/cyz/wxyd/ui/nav/AppNav.kt`
- Create: `android/app/src/main/java/com/cyz/wxyd/ui/login/LoginScreen.kt` + `LoginViewModel.kt`
- Create: `android/app/src/main/java/com/cyz/wxyd/ui/common/ErrorHandling.kt`(ApiException→Snackbar 文案、SessionExpired→回登录)

**Interfaces:**
- Produces:
  - 路由约定(后续任务全部复用):`login`、`main`(含 2 Tab)、`home`、`profile`、以及各模块 `welfare/dayinfo/gateway/robot/notes/share/shell/javaapi`;注册/找回路由 Task 6 加。
  - `class UserSession(val id: Long, val username: String, val isAdmin: Boolean)`;`class SessionViewModel`: `session: StateFlow<UserSession?>`,登录后从 `POST /api/login` + `GET /api/user/info` 组装;`SessionExpiredException` 全局收集置空 session → 导航回 `login`。
  - `LoginViewModel.login(username, password)`: `postBody("/login", {username,password})` → `user/info` 补全 → 存 session。
- Consumes: Task 2 ApiClient/SettingsStore、Task 3 ThemeController。

- [ ] **Step 1: 写 SessionViewModel + UserSession**

登录流程按 spec 约束 7:`/login` 返回 User(id/username/role),再 `user/info` 补 isAdmin。

- [ ] **Step 2: 写 LoginScreen**

账号/密码输入 + 登录按钮 + Loading 遮罩 + 错误 Snackbar;"没有账号?去注册"、"忘记密码?"文字链接占位(路由 Task 6 接)。Base URL 为空时先显示"服务器地址"输入引导(spec 7 节)。

- [ ] **Step 3: 写 AppNav + MainActivity**

`NavHost` 单栈;`session == null` 时起始路由 `login`,否则 `main`。`MainActivity` 组装 ThemeController + SessionViewModel + AppNav。

- [ ] **Step 4: 写 ErrorHandling.kt**

`fun Throwable.userMessage(): String`(ApiException→errorMsg,SessionExpired→"登录已过期",其他→"网络错误,请重试")。

- [ ] **Step 5: 静态校验**

Run: `grep -rn "postBody(\"/login\"" android/app/src/main/java/com/cyz/wxyd/ui/login/ && grep -rn "user/info" android/app/src/main/java/com/cyz/wxyd/ui/login/ && grep -rn "SessionExpiredException" android/app/src/main/java/com/cyz/wxyd/ui/common/ErrorHandling.kt`
Expected: 命中。人工核对 H5 `login.html`/`h5.html` 中登录请求的封装格式确认双层 `{body:{username,password}}`。

- [ ] **Step 6: Commit**

```bash
cd /apps/GIT/wxyd && git add android/ && git commit -m "feat(android): 导航骨架 + 登录 + 会话状态"
```

---

### Task 5: 滑块验证码组件

**Files:**
- Create: `android/app/src/main/java/com/cyz/wxyd/ui/captcha/SliderCaptcha.kt`
- Create: `android/app/src/main/java/com/cyz/wxyd/ui/captcha/CaptchaViewModel.kt`

**Interfaces:**
- Produces:
  - `SliderCaptcha(onVerified: (ticket: String) -> Unit, onFailed: () -> Unit)` — Composable:加载 `GET /captcha/slider` → base64 png 解码绘制底图与拼图块 → `pointerInput` 水平拖拽 → 松手 `POST /captcha/verify {body:{captchaId, x}}` → 成功回调 ticket,失败刷新滑块并提示。
  - `CaptchaViewModel`: `load()`, `verify(x: Float)`, `state: StateFlow<CaptchaState>`;CaptchaState = Loading/Ready(bg,piece,y)/Verifying/Success(ticket)/Failed(msg)。
- Consumes: Task 2 ApiClient(`getResult`/`postBody`,captcha 接口契约见 spec 约束 13)。

- [ ] **Step 1: 写 CaptaViewModel 状态机**

`GET captcha/slider` 解析 `{captchaId, bg, piece, y}`;base64 → `ImageBitmap`(`BitmapFactory` + `asImageBitmap`)。

- [ ] **Step 2: 写 SliderCaptcha Composable**

`Canvas` 画底图,拼图块画在 `(dragX, y)`;`pointerInput { detectDragGestures }` 只允许水平;松手以拼图块中心 x 提交 verify;Success → `onVerified(ticket)` 并禁用交互;Failed → 重置 dragX + 重新 `load()`。

- [ ] **Step 3: 静态校验**

Run: `grep -rn "captcha/slider\|captcha/verify" android/app/src/main/java/com/cyz/wxyd/ui/captcha/ && grep -rn "onVerified" android/app/src/main/java/com/cyz/wxyd/ui/captcha/SliderCaptcha.kt`
Expected: 命中;人工确认 verify 用 `postBody`(双层 `{body:{captchaId,x}}`)、失败路径会重新 `load()`(ticket 一次性,Review Focus #4)。

- [ ] **Step 4: Commit**

```bash
cd /apps/GIT/wxyd && git add android/ && git commit -m "feat(android): 原生滑块验证码组件"
```

---

### Task 6: 注册 + 找回密码三步向导

**Files:**
- Create: `android/app/src/main/java/com/cyz/wxyd/ui/auth/RegisterScreen.kt` + `ForgotPasswordScreen.kt`
- Create: `android/app/src/main/java/com/cyz/wxyd/ui/auth/AuthWizardViewModel.kt`
- Modify: `android/app/src/main/java/com/cyz/wxyd/ui/nav/AppNav.kt`(加路由 `register`、`forgot`)
- Modify: `android/app/src/main/java/com/cyz/wxyd/ui/login/LoginScreen.kt`(链接接到新路由)

**Interfaces:**
- Produces:
  - 共用 `AuthWizardViewModel(mode: REGISTER|RESET)`:步骤状态机 滑块 → 发送邮箱码(60s 倒计时)→ 验证 → 完成。
  - 接口(spec 约束 14):`register/email/send {body:{email,ticket}}` → `register/email/verify {body:{email,code}}` → `register/email/complete {body:{registerToken,password,username?}}`;找回用 `password/reset/send|verify|complete`,字段同构。
- Consumes: Task 5 SliderCaptcha、Task 2 ApiClient、Task 4 路由/会话。

- [ ] **Step 1: 写 AuthWizardViewModel**

状态机 4 步;`ticket` 在 `send` 成功后置空(一次性);`send` 失败回到滑块步骤;60s 倒计时用 `countDownFlow`。

- [ ] **Step 2: 写两个 Screen(共用向导布局)**

复用 `SliderCaptcha`;注册 complete 成功 → 登录态生效(spec:成功即登录),导航 `main`;找回 complete 成功 → 回 `login` 并提示用新密码登录。

- [ ] **Step 3: 接路由与登录页链接**

AppNav 加 `register`/`forgot`;LoginScreen 两个链接 `navigate` 过去。

- [ ] **Step 4: 静态校验**

Run: `grep -rn "register/email\|password/reset" android/app/src/main/java/com/cyz/wxyd/ui/auth/ && grep -rn "registerToken" android/app/src/main/java/com/cyz/wxyd/ui/auth/`
Expected: 三个接口路径与 `registerToken` 传递命中;人工对照 H5 `register.html`/`forgot.html` 核对字段名。

- [ ] **Step 5: Commit**

```bash
cd /apps/GIT/wxyd && git add android/ && git commit -m "feat(android): 邮箱注册与找回密码三步向导"
```

---

### Task 7: 首页工作台 + 我的 Tab

**Files:**
- Create: `android/app/src/main/java/com/cyz/wxyd/ui/home/HomeScreen.kt` + `HomeViewModel.kt`
- Create: `android/app/src/main/java/com/cyz/wxyd/ui/profile/ProfileScreen.kt`
- Modify: `android/app/src/main/java/com/cyz/wxyd/ui/nav/AppNav.kt`(`main` 拆为 2 Tab + 各模块路由占位)

**Interfaces:**
- Produces:
  - `HomeViewModel.menus: StateFlow<List<MenuItem>>`;`MenuItem(key, name, icon, hashUrl, sortNo, adminOnly)`(`hashUrl` 非空才入列表;按 sortNo 已序)。`GET /api/menu/list?platform=h5`。
  - 卡片点击 → `navigate("module/{key}")`,路由表按 key 分发到各模块 Screen(Task 8–13 逐个实装,未实装 key 显示"建设中")。
  - ProfileScreen:显示 username/isAdmin、主题选择器(7 值,写 ThemeController)、服务器地址编辑(写 SettingsStore)、退出登录(清会话回 login)、关于。
- Consumes: Task 2/3/4。

- [ ] **Step 1: 写 HomeViewModel + MenuItem**

字段名是 `key`(不是 menu_key,Review Focus #3);本地固定追加 `home`/`profile` 可见逻辑由 Tab 承担,网格仅渲染接口返回项。

- [ ] **Step 2: 写 HomeScreen**

欢迎区"你好,{username} 👋" + 实时时钟(`LaunchedEffect` 每 1s)+ `LazyVerticalGrid(GridCells.Adaptive(120.dp))`;卡片 = emoji Text + 名称;`adminOnly==1` 加"管理员"角标。

- [ ] **Step 3: 写 ProfileScreen + 退出登录**

按 Interfaces 描述实现;主题选择器为 7 项单选(对话框或列表)。

- [ ] **Step 4: AppNav 接 main 双 Tab**

`NavigationBar` 2 项(工作台/我的),各模块路由占位 `placeholder`。

- [ ] **Step 5: 静态校验**

Run: `grep -rn "platform=h5" android/app/src/main/java/com/cyz/wxyd/ui/home/ && grep -rn "hashUrl" android/app/src/main/java/com/cyz/wxyd/ui/home/ && grep -rn '"key"' android/app/src/main/java/com/cyz/wxyd/ui/home/HomeViewModel.kt`
Expected: 命中。人工对照 H5 `h5.html` 菜单渲染段核对字段名与 `hashUrl` 过滤。

- [ ] **Step 6: Commit**

```bash
cd /apps/GIT/wxyd && git add android/ && git commit -m "feat(android): 工作台卡片网格 + 我的页 + 双 Tab 骨架"
```

---

### Task 8: 福利专区(welfare + 羊毛雷达)

**Files:**
- Create: `android/app/src/main/java/com/cyz/wxyd/ui/welfare/WelfareScreen.kt` + `WelfareDetailScreen.kt` + `WelfareViewModel.kt`
- Create: `android/app/src/main/java/com/cyz/wxyd/ui/welfare/RadarSection.kt`

**Interfaces:**
- Produces: welfare 列表(分页/搜索,GET `/welfare/list`)、详情(Markdown 渲染,GET `/welfare/detail`)、RadarSection(welfare 页内订阅管理:列表/新增/启停/测试,`/radar/*`,postBody 双层)。
- Consumes: Task 2 ApiClient、Task 4 路由、compose-markdown 依赖。

- [ ] **Step 1: 写 WelfareViewModel**

列表分页(`LazyColumn` 触底加载)+ 关键字搜索;详情拉取。

- [ ] **Step 2: 写 WelfareScreen + DetailScreen**

列表项标题 + 摘要;详情用 `MarkdownText`(compose-markdown);下拉刷新 `PullToRefreshBox`。

- [ ] **Step 3: 写 RadarSection**

welfare 页内区块(非独立卡片,spec D12):订阅列表 + 新增 `ModalBottomSheet` + 启停/测试按钮;`/radar/*` 全部 `postBody` 双层。

- [ ] **Step 4: 静态校验**

Run: `grep -rn "welfare/list\|welfare/detail" android/app/src/main/java/com/cyz/wxyd/ui/welfare/ && grep -rn "postBody(\"/radar" android/app/src/main/java/com/cyz/wxyd/ui/welfare/`
Expected: 命中;人工对照 H5 welfare 段核对 radar 子接口路径与字段。

- [ ] **Step 5: Commit**

```bash
cd /apps/GIT/wxyd && git add android/ && git commit -m "feat(android): 福利专区 + 羊毛雷达订阅"
```

---

### Task 9: 金融日报(dayinfo)

**Files:**
- Create: `android/app/src/main/java/com/cyz/wxyd/ui/dayinfo/DayInfoScreen.kt` + `DayInfoViewModel.kt`

**Interfaces:**
- Produces: 日报列表(`GET /dayinfo/list`)、"最新一期"入口(`/dayinfo/latest`)、详情(`/dayinfo/detail`,Markdown)、订阅管理(`/dayinfo/subscribe/*`,postBody 双层)。
- Consumes: Task 2、Task 4、compose-markdown。

- [ ] **Step 1: 写 DayInfoViewModel**

list/latest/detail + subscribe 状态(已订阅开关、启停)。

- [ ] **Step 2: 写 DayInfoScreen**

默认展示最新一期 + 列表切换;订阅管理 `ModalBottomSheet`。

- [ ] **Step 3: 静态校验**

Run: `grep -rn "dayinfo/list\|dayinfo/latest\|dayinfo/detail\|dayinfo/subscribe" android/app/src/main/java/com/cyz/wxyd/ui/dayinfo/`
Expected: 四类命中;人工对照 H5 dayinfo 段核对 subscribe 子路径与字段。

- [ ] **Step 4: Commit**

```bash
cd /apps/GIT/wxyd && git add android/ && git commit -m "feat(android): 金融日报"
```

---

### Task 10: 模型网关 gateway(用户侧)

**Files:**
- Create: `android/app/src/main/java/com/cyz/wxyd/ui/gateway/GatewayScreen.kt` + `GatewayViewModel.kt`

**Interfaces:**
- Produces: key 列表(`GET /gateway/keys`)、新增/启停/删除/查看明文(`POST /gateway/keys/{add,toggle,delete,reveal}`,postBody 双层)、模型列表(`GET /gateway/models`)。**不含** `gateway/admin/*`。
- Consumes: Task 2、Task 4。

- [ ] **Step 1: 写 GatewayViewModel**

keys/models/reveal 状态;每日失效 key 需"启用"操作入口(spec 约束 9)。

- [ ] **Step 2: 写 GatewayScreen**

key 卡片列表;reveal 结果弹窗展示(可复制);新增 `ModalBottomSheet`;删除二次确认。

- [ ] **Step 3: 静态校验**

Run: `grep -rn "gateway/keys" android/app/src/main/java/com/cyz/wxyd/ui/gateway/ && grep -rn "gateway/admin" android/app/src/main/java/com/cyz/wxyd/ui/gateway/ || true`
Expected: 第一条命中;第二条无命中(确认未引入 admin 接口)。

- [ ] **Step 4: Commit**

```bash
cd /apps/GIT/wxyd && git add android/ && git commit -m "feat(android): 模型网关用户侧 key 管理"
```

---

### Task 11: 钉钉机器人 robot

**Files:**
- Create: `android/app/src/main/java/com/cyz/wxyd/ui/robot/RobotScreen.kt` + `RobotViewModel.kt`

**Interfaces:**
- Produces: 机器人列表(`GET /robot/list`)、CRUD(`POST /robot/{add,update,delete}` — **直接字段 JSON**,postDirect)、发送(`POST /robot/send` — **multipart**,文本或文件)。
- Consumes: Task 2 的 `postDirect`/`postMultipart`(Review Focus #1 主战场)。

- [ ] **Step 1: 写 RobotViewModel**

list + CRUD(注意 add/update/delete 用 `postDirect`,不是 postBody)+ send(文本消息直传;文件消息用 multipart)。

- [ ] **Step 2: 写 RobotScreen**

列表 + `SwipeToDismissBox` 删除 + Snackbar 撤销;新增/编辑 `ModalBottomSheet`;发送页选机器人 + 文本/文件切换 + 发送结果反馈。

- [ ] **Step 3: 静态校验**

Run: `grep -rn "postDirect(\"/robot" android/app/src/main/java/com/cyz/wxyd/ui/robot/ && grep -rn "postMultipart(\"/robot/send\"" android/app/src/main/java/com/cyz/wxyd/ui/robot/ && grep -rn "postBody(\"/robot" android/app/src/main/java/com/cyz/wxyd/ui/robot/ || true`
Expected: 前两条命中;第三条无命中(误用双层格式即违规)。

- [ ] **Step 4: Commit**

```bash
cd /apps/GIT/wxyd && git add android/ && git commit -m "feat(android): 钉钉机器人管理与发送"
```

---

### Task 12: 记忆笔记 notes + 文件共享 share

**Files:**
- Create: `android/app/src/main/java/com/cyz/wxyd/ui/notes/NotesScreen.kt` + `NoteEditScreen.kt` + `NotesViewModel.kt`
- Create: `android/app/src/main/java/com/cyz/wxyd/ui/share/ShareScreen.kt` + `ShareViewModel.kt`
- Create: `android/app/src/main/java/com/cyz/wxyd/data/remote/FileDownloader.kt`

**Interfaces:**
- Produces:
  - notes:我的/公开列表 + 搜索(`GET /note/list|search`,他人公开笔记仅标题可搜)、详情/编辑(Markdown)、分类/标签(`GET /note/categories|tags`)、保存/删除(`POST /note/save|delete`,postBody)、导出(`note/export|exportAll` 文件流)。
  - share:列表(`GET /share/list`)、下载(FileDownloader)、上传/删除仅超管可见(`share/upload` multipart、`share/delete` postBody)。
  - `class FileDownloader(client: ApiClient)`: `suspend fun download(path: String, query: Map<String,String>, target: File): Result<File>` — 流式落盘;HTTP 404 → `ApiException("404","文件不存在或无权访问")`(Review Focus #5);成功后 `MediaScanner`/通知可选,首版落 `Downloads/` 并 Toast 路径。
- Consumes: Task 2 `getRaw`、Task 4 session.isAdmin。

- [ ] **Step 1: 写 FileDownloader**

OkHttp 流式 copy 到临时文件再 rename;404/非 200 映射为用户可读异常。

- [ ] **Step 2: 写 NotesScreen/NoteEditScreen/NotesViewModel**

双列表(我的/公开)Tab 或分段;搜索框;编辑页 Markdown 输入 + 实时预览切换;导出按钮调 FileDownloader。

- [ ] **Step 3: 写 ShareScreen/ShareViewModel**

列表 + 下载(进度 Toast);`isAdmin` 为 true 才显示上传按钮与删除操作。

- [ ] **Step 4: 静态校验**

Run: `grep -rn "note/export\|share/download" android/app/src/main/java/com/cyz/wxyd/ && grep -rn "share/upload\|share/delete" android/app/src/main/java/com/cyz/wxyd/ui/share/ && grep -rn "isAdmin" android/app/src/main/java/com/cyz/wxyd/ui/share/ShareViewModel.kt`
Expected: 命中;人工确认上传/删除入口受 isAdmin 控制。

- [ ] **Step 5: Commit**

```bash
cd /apps/GIT/wxyd && git add android/ && git commit -m "feat(android): 记忆笔记 + 文件共享"
```

---

### Task 13: Shell 脚本 + Java API(只读学习模块)

**Files:**
- Create: `android/app/src/main/java/com/cyz/wxyd/ui/shell/ShellScreen.kt` + `ShellViewModel.kt`
- Create: `android/app/src/main/java/com/cyz/wxyd/ui/javaapi/JavaApiScreen.kt` + `JavaApiViewModel.kt`

**Interfaces:**
- Produces:
  - shell:主题列表 `GET /shellscript/topics`(**裸数组**,getRaw)、详情 `GET /shellscript/topics/{name}`(裸对象);"存笔记"按钮调 Task 12 的 `note/saveFromSource`(postBody)。
  - javaapi:类列表 `GET /javaapi/classes`(裸 List,getRaw)、详情 `GET /javaapi/classes/{name}`、在线运行 `POST /javaapi/classes/{name}/test`(**直接字段** `{testName,code}`,返回 `ResponseEntity<CodeResult>` 也按裸处理)。
- Consumes: Task 2 `getRaw`/`postDirect`,Task 12 note 保存。

- [ ] **Step 1: 写 ShellViewModel + ShellScreen**

列表 → 详情(代码块等宽字体展示);存笔记确认后提示成功。

- [ ] **Step 2: 写 JavaApiViewModel + JavaApiScreen**

类列表 → 详情(方法列表)+ "运行测试用例"输入 `testName`/`code` → 展示运行结果。

- [ ] **Step 3: 静态校验**

Run: `grep -rn "getRaw(\"/shellscript\|getRaw(\"/javaapi" android/app/src/main/java/com/cyz/wxyd/ && grep -rn "postDirect" android/app/src/main/java/com/cyz/wxyd/ui/javaapi/`
Expected: 命中;确认两模块未用 `getResult`/`postBody`(裸契约,Review Focus #1 延伸)。

- [ ] **Step 4: Commit**

```bash
cd /apps/GIT/wxyd && git add android/ && git commit -m "feat(android): Shell 脚本与 Java API 学习模块"
```

---

### Task 14: 收尾——契约全面核对 + 构建指引

**Files:**
- Create: `android/README.md`
- Modify: 各模块 Screen 中 Task 7 留下的 `placeholder` 路由(确保无死链)

**Interfaces:**
- Consumes: 全部前序任务。

- [ ] **Step 1: 全量契约 grep 核对**

Run: `grep -rhoE '"/(login|menu/list|user/info|user/updateTheme|welfare|radar|dayinfo|gateway|robot|note|share|shellscript|javaapi|captcha|register|password)[^"]*"' android/app/src/main/java | sort -u`
对照 spec 附录接口速查逐行核对:每个出现的 path 在附录中存在且请求体格式匹配三层分类;附录中首版范围内的接口在代码中都有对应调用。

- [ ] **Step 2: 路由死链检查**

Run: `grep -rn "navigate(" android/app/src/main/java/com/cyz/wxyd/ui/ | grep -v "popBackStack"`
人工核对每个 navigate 目标路由都在 AppNav 中定义;placeholder 已全部替换或移除。

- [ ] **Step 3: 写 README**

内容:环境要求(JDK 17 + Android SDK)、`./gradlew assembleDebug` 构建步骤、Base URL 配置说明(我的页可配,支持 http 明文)、首版功能范围与已知限制(引用 spec)。

- [ ] **Step 4: Commit**

```bash
cd /apps/GIT/wxyd && git add android/ && git commit -m "docs(android): 构建指引 + 全量契约核对收尾"
```
