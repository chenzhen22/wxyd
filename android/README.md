# wxyd Android 原生客户端（Kotlin + Jetpack Compose）

小泽助手的 Android 原生客户端，**直接调用现有后端 `/api/*`**，后端无需任何改动。
与 H5 版功能对齐的首版范围见下方「首版范围」。

## 技术栈

| 项 | 版本 |
|----|------|
| Kotlin | 2.0.21（kotlinx-serialization 插件） |
| Compose | BOM 2024.09.03（Material3） |
| AGP / Gradle | 见 `gradle/` 与 `build.gradle.kts`（wrapper 首次构建时由 Android Studio 生成） |
| OkHttp | 4.12（手写网络层，不用 Retrofit） |
| DataStore | 1.1.1（服务器地址、本地偏好） |
| minSdk / targetSdk | 26 / 34（Java 8 API 模块依赖服务端 JDK，客户端无此要求） |

## 构建

需要 **JDK 17**（AGP 8.x 要求）与 **Android SDK**（compileSdk 34）。

```bash
cd android
# 首次：用 Android Studio 打开 android/ 目录（会自动生成/下载 Gradle wrapper 与 SDK 组件），或自行：
#   gradle wrapper --gradle-version 8.x
./gradlew assembleDebug
# 产物：android/app/build/outputs/apk/debug/app-debug.apk
```

> 注意：仓库**不包含** `gradlew` 与 `local.properties`（后者含本机 SDK 路径，已被 gitignore）；
> 需要 JDK 17 + Android SDK 的构建机。CI 可用 `-Dorg.gradle.jvmargs=-Xmx2g`。

## 运行与服务器地址

后端默认监听 `8090`，context-path `/api`（`http://<host>:8090/api`）。

- App 不内置默认地址（`SettingsStore.DEFAULT_BASE_URL` 为空），首次登录前需在
  **我的 → 服务器地址** 填写，例如：
  - 真机：`http://192.168.1.10:8090/api`
  - 模拟器访问宿主机：`http://10.0.2.2:8090/api`
- 地址**不要**带结尾 `/`；保存时自动去除。填错或后端不可达时，登录页会给出「服务器地址未配置或非法」或网络错误提示。
- 明文 HTTP 已放行（`res/xml/network_security_config.xml`）；生产建议上 HTTPS，地址填 `https://.../api`。

## 网络契约（客户端侧约定）

- 所有请求带 `X-Requested-With: XMLHttpRequest`（否则后端对未登录返回 302 而非 401）；
- HTTP 401 → 全局清会话、回登录页（`ApiClient.onUnauthorized`）；
- 统一响应 `Result {errorCode,errorMsg,body}`，成功 `"000000"`；
- 请求体三种格式集中在 `RequestBodyFactory`：
  - 双层 `{"body":{...}}` → `postBody`（绝大多数接口）；
  - 直接字段 → `postDirect`（`robot/add|update|delete`、`javaapi/.../test`）；
  - multipart → `postMultipart`（`robot/send`、`share/upload`）。
- 裸响应（不包 `Result`）：`shellscript/topics*`、`javaapi/classes*`、`menu/list`、`welfare/list`、`robot/list`、
  `note/search`、`share/list`、文件下载流 —— 走 `getRaw` / `ApiClient.postDirectRaw`。

## 目录结构

```
android/app/src/main/java/com/cyz/wxyd/
├── AppContainer.kt          # 手工依赖容器（ApiClient / SettingsStore / FileDownloader）
├── data/
│   ├── local/               # DataStore（服务器地址）、SessionCookieJar
│   └── remote/              # ApiClient、RequestBodyFactory、ApiResult、FileDownloader
└── ui/
    ├── nav/                 # AppNav（会话互斥双路由图）、MainScreen（工作台/我的）、ModuleRoute 分发
    ├── auth/ login/ session/# 登录、注册向导、找回密码、会话
    ├── welfare/             # 福利雷达（列表/详情 + 羊毛雷达订阅）
    ├── dayinfo/             # 金融日报（列表/详情 + 我的订阅）
    ├── gateway/             # 模型网关：我的 Key（新增/查看明文/启停/删除）
    ├── robot/               # 钉钉机器人：管理 + 发送（文本/文件）
    ├── notes/               # 记忆笔记：我的/公开、搜索、编辑、预览、导出
    ├── share/               # 文件共享：下载（全员）、上传/删除（管理员）
    ├── shell/               # Shell 脚本学习（只读 + 存笔记）
    ├── javaapi/             # Java 8 API 学习（方法 + 在线运行 + 存笔记）
    └── common/              # 工厂、错误文案、Markdown 渲染
```

## 首版范围与限制

**已实现**：登录 / 注册向导 / 找回密码；工作台（`menu/list?platform=h5` 驱动的卡片）与「我的」；
福利雷达、金融日报、模型网关（用户侧 Key）、钉钉机器人（管理与发送）、记忆笔记、文件共享、
Shell 脚本学习、Java API 学习（只读 + 在线运行）。

**未实现（进入显示"建设中"占位，无死链）**：留言板、群聊、巡检日报、证据整理、归档、`admin` 系列（用户/菜单/日志管理）等。

其他限制：

- 下载文件保存到公共目录 `Download/xiaozhe/`，需系统存储权限或用户授权（Android 10+ 走分区存储适配）；
- 分享/导出通过 FileProvider（authority `${applicationId}.fileprovider`）打开系统应用查看；
- 在线运行 Java 代码依赖服务端 `javax.tools.JavaCompiler`（故服务端必须用 JDK 运行，不能用 JRE）；
- 首版不包含后台推送，消息类内容需打开 App 主动刷新。
