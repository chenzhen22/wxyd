# 小泽助手（wxyd）

基于 Spring Boot 2.7.18 的轻量运维管理平台，提供用户管理、留言板、群聊、钉钉机器人消息推送、用户级 Markdown 记忆笔记、菜单管理、Java 8 API 交互式学习等功能。前后端一体（桌面版 + H5），单 jar 部署。

## ✨ 功能模块

| 模块 | 说明 |
|------|------|
| 🏠 首页 | 欢迎面板 + 实时时钟 |
| 👤 用户管理 | 注册审批制（超级管理员审批），SM4 加密密码，HTTP Session 鉴权 |
| 💬 留言板 | 按用户 ID 鉴权（管理员删任意 / 用户删自己的），最多保留 50 条，昵称显示名回退用户名 |
| 👥 群聊 | 桌面/H5 双端；每用户限建 5 群；模糊搜索群名申请加入；群主审批/踢人/直接拉人（用户名/昵称模糊搜索）；消息支持文字/表情/图片；消息记录存浏览器 localStorage，服务器只做中转（拉取即删，7 天兜底清理）；已读回执（仅发送者可见「已读 N/M」，点击查看已读人明细） |
| 📝 记忆笔记 | 用户级 Markdown 笔记（可新增/编辑/删除），md 存 `/apps/shareFile/{用户id}/`；分公共/私有（frontmatter visibility），公共笔记可被其他用户按标题模糊搜索；旧笔记迁移至 admin 用户下 |
| 🧩 菜单管理 | 仅管理员；四态配置（全部可见/全部隐藏/白名单可见/黑名单不可见）控制各菜单对普通用户的显隐，桌面菜单与 H5 卡片/tab 同步生效；管理员始终可见全部 |
| 🤖 钉钉机器人 | 每用户独立机器人 CRUD，发送文本/文件消息（文件→zip→base64，>20KB 分块 ≤18KB），文件限制 200KB |
| ☕ Java8-API | 384 个 Java API 文档 + 在线编辑运行测试代码（javax.tools.JavaCompiler + JUnit 4，10 秒超时，安全黑名单） |
| 🗄️ SQL 查询 | 仅超级管理员可见的在线 SQL 工具，直连 wxyd 自有 MySQL 数据源；仅允许 SELECT/SHOW/DESC/EXPLAIN/WITH 单语句，分页查询，支持 CSV/Excel 导出 |
| 📑 证据材料整理 | 上传多个 PDF / 图片 / Word / Excel，或其 ZIP / 7z 压缩包（服务端自动解压展开），按文件名/文件夹页码标注合并为单一 A4 竖向带页码 PDF（与 v5 输出逐像素一致）：横向内容自动旋转成竖向、矢量保真不裁剪、发票 2 页并排、Excel 截图两两并排、Word 宋体/黑体渲染、标注重叠自动填空洞；失败返回 JSON 错误（仅登录可见，菜单管理可配显隐） |
| 📁 文件共享 / 🐚 Shell脚本 / 🔌 Dubbo调用 / 📦 归档下载 | 辅助工具模块 |
| 🎨 主题 | 7 套主题（含跟随系统），持久化到用户 |
| 📱 H5 | 独立移动端页面（h5.html），桌面版功能基本对齐，群聊入口在底部 tab；次级模块（含证据材料整理）由首页卡片网格进入 |

## 🛠 技术栈

| 层级 | 技术 |
|------|------|
| 语言 / 运行时 | **Java 8**（JDK，非 JRE — Java8-API 模块需 `javax.tools.JavaCompiler`） |
| 框架 | Spring Boot **2.7.18** |
| ORM | MyBatis（mybatis-spring-boot-starter **2.3.2**，`mapUnderscoreToCamelCase`） |
| 数据库 | MySQL 8.0.30 + HikariCP 连接池 |
| 安全 | 国密 SM4（CBC + PKCS7Padding + 随机 IV）加密密码与机器人 SECRET，BouncyCastle |
| HTTP 客户端 | OkHttp 4.9.3 |
| 前端 | jQuery 3.7.1 + 暗色玻璃拟态（glassmorphism）主题（多主题可切换） |
| 构建 | Maven 3.8.x（`D:\cz\java\maven\apache-maven-3.8.4`） |
| 本地仓库 | `D:\cz\repository` |

## 🚀 快速开始

### 前置条件

- **JDK 8**（必须 JDK，非 JRE）
- **Maven 3.8+**
- **MySQL 8.0+**

### 1. 克隆 & 配置

```bash
git clone <repo-url> wxyd
cd wxyd
```

复制环境配置模板并填入真实值：

```bash
cp app/src/main/resources/env.properties.example app/src/main/resources/env.properties
```

`env.properties` 内容（**已 gitignore，永不提交**）：

```properties
# 数据源
datasource.url=jdbc:mysql://<host>:<port>/<db>?useSSL=false&characterEncoding=utf8&serverTimezone=Asia/Shanghai
datasource.username=<db_user>
datasource.password=<db_password>

# 国密 SM4 密钥（16 字节 / 32 位 hex）— 加密用户密码与机器人 SECRET
# 生成方式：运行 com.cyz.util.SMUtil.generateSM4Key()
wxyd.sm4.key=<32_hex_chars>

# 超级管理员初始账号（首次启动且 users 表为空时自动播种）
wxyd.admin.username=admin
wxyd.admin.password=<initial_admin_password>
```

### 2. 初始化数据库

```bash
# 基础表（message、action、requestlog 等）
mysql -u root -p < wxyd.sql

# 迁移脚本（按序号执行）
mysql -u root -p < app/src/main/resources/sql/V2__user_robot.sql      # users + ding_robot
mysql -u root -p < app/src/main/resources/sql/V4__group_chat.sql      # 群聊四表（group_chat/group_member/group_join_apply/group_msg_transit）
mysql -u root -p < app/src/main/resources/sql/V5__group_msg_read.sql  # 群聊已读回执（group_msg/group_msg_read + transit.msg_id）
mysql -u root -p < app/src/main/resources/sql/V6__menu_config.sql     # 菜单管理（menu_config）
```

### 3. 构建 & 运行

```bash
# 构建（JDK 8）
mvn -pl app -am clean package -DskipTests

# 运行
java -Dwxyd.mock.enabled=true -jar app/target/app-1.0.0.jar
```

打开浏览器访问 **http://localhost:8090/api/login.html**

> **Mock 模式**：`-Dwxyd.mock.enabled=true` 时，外部 `tbphx.do` 网关返回 mock 数据，不影响新增的管理端点（用户/机器人/留言/javaapi 走真实逻辑）。生产环境设为 `false`。

## 📂 项目结构

```
wxyd/
├── app/                              # 唯一可执行模块（单 jar 部署）
│   ├── src/main/java/com/cyz/
│   │   ├── WxydApplication.java      # 启动入口（@PropertySource env.properties）
│   │   ├── config/                   # WebMvcConfig, DataInitializer, mybatisMysqlConfig
│   │   ├── controller/               # AuthController, UserController, MessageController,
│   │   │                             #   RobotController, ApiController(tbphx.do),
│   │   │                             #   SqlQueryController(仅管理员 SQL 工具)
│   │   ├── pdf/                      # PdfMergeService + PdfMergeController（证据材料整理，PDFBox 2.0.24）
│   │   ├── javaapi/                  # Java8 API 学习平台（独立子包）
│   │   │   ├── model/                #   ApiClass, ApiMethod, TestCase, CodeResult
│   │   │   ├── service/              #   ApiDataService, CodeExecutionService
│   │   │   └── controller/           #   JavaApiController
│   │   ├── service/                  # AuthService, UserService, MessageService, RobotService,
│   │   │                             #   SqlQueryService(直连 mysqlDataSource)
│   │   ├── filter/                   # RepeatReadFilter（multipart 免包装）
│   │   ├── mock/                     # MockAspect（mock 模式兜底 tbphx.do）
│   │   └── aspect/                   # ControllerAspect（traceId/clientIp MDC）
│   ├── src/main/resources/
│   │   ├── application.yml           # port 8090, context-path /api
│   │   ├── env.properties            # 真实密钥/数据库（gitignore）
│   │   ├── env.properties.example    # 配置模板
│   │   ├── data/                     # 384 个 Java API 文档（Markdown）
│   │   ├── data-note/                # 旧版只读笔记（已迁移至 admin 用户目录，仅作迁移源保留）
│   │   ├── mapper/MysqlMapper.xml    # MyBatis SQL
│   │   ├── sql/V2..V6__*.sql         # 迁移脚本（用户/机器人、群聊、已读回执、菜单配置）
│   │   └── static/                   # 前端页面 + CSS + JS
│   │       ├── login.html            # 登录/注册（暗色玻璃 + 流动渐变动画）
│   │       ├── api.html              # 管理后台（桌面版单页）
│   │       ├── h5.html               # 移动端单页（底部 tab + 模块卡片）
│   │       └── Assets/
│   │           ├── css/admin.css     # 主题样式（多主题变量 + 群聊/笔记/菜单管理样式）
│   │           └── js/
│   │               ├── index.js      # 桌面版主逻辑（菜单门控 + 各模块懒加载）
│   │               ├── h5.js         # H5 主逻辑
│   │               ├── group-chat.js # 群聊（双端共用：轮询/缓存/已读回执）
│   │               ├── menu-manage.js# 菜单管理（双端共用，仅管理员）
│   │               ├── note.js       # 记忆笔记（桌面版）
│   │               ├── javaapi.js    # Java API 学习平台逻辑
│   │               ├── sqltool.js    # SQL 查询工具逻辑（仅管理员加载）
│   │               ├── pdftool.js    # 证据材料整理逻辑（桌面版：上传/排序/标注/合并下载）
│   │               └── h5-pdftool.js # 证据材料整理逻辑（H5 版，registerH5Module 注册）
│   └── pom.xml
├── wxyd.sql                          # 基础数据库建表脚本
└── pom.xml                           # 父 POM（Spring Boot 2.7.18）
```

## 🔌 API 接口

Context-path：`/api`，所有路径前缀 `/api/`。除登录/注册外均需 Session 鉴权（未登录返回 401）。

### 认证

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `register` | 注册（待审批），SM4 加密密码 |
| POST | `login` | 登录，创建 HTTP Session |
| POST | `logout` | 注销，销毁 Session |

### 用户管理

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `user/list` | 用户列表（管理员看全部，普通用户只看自己） |
| POST | `approveUser` | 审批通过（仅管理员） |
| POST | `rejectUser` | 审批拒绝（仅管理员） |
| POST | `deleteUser` | 删除用户（仅管理员） |
| POST | `user/updateDisplayName` | 设置昵称 |

### 留言板

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `queryMessage` | 查询留言（flag=1 只看自己的） |
| POST | `addMessage` | 新增留言（自动保留最新 50 条） |
| POST | `delMessage` | 删除留言（管理员删任意 / 用户删自己的） |

### 钉钉机器人

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `robot/list` | 机器人列表 |
| POST | `robot/add` | 新增机器人 |
| POST | `robot/update` | 更新机器人 |
| POST | `robot/delete` | 删除机器人 |
| POST | `robot/send` | 发送消息（text / file，文件 ≤200KB） |

### Java8 API 学习

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `javaapi/classes` | API 列表（384 个，按字母排序） |
| GET | `javaapi/classes/{name}` | API 详情（方法 + 测试用例） |
| POST | `javaapi/classes/{name}/test` | 编译执行测试代码 |
| POST | `javaapi/classes/reload` | 重新加载 API 数据 |

### 群聊

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `group/my` | 我加入的群列表 |
| POST | `group/create` | 创建群聊（每人最多 5 个） |
| GET | `group/search?keyword=` | 群名模糊搜索（含是否已加入/已申请标记） |
| POST | `group/apply` | 申请入群 |
| GET | `group/applies` | 我作为群主的待审批列表 |
| POST | `group/handle` | 审批入群申请（通过/拒绝） |
| POST | `group/invite` | 直接拉用户入群（仅群主） |
| GET | `group/users?groupId=&keyword=` | 邀请场景：按用户名/昵称模糊搜索用户（仅群主） |
| POST | `group/kick` | 踢出成员（仅群主） |
| GET | `group/members?groupId=` | 成员列表 |
| POST | `group/img` | 上传聊天图片（≤5MB，jpg/png/gif/webp），返回相对 URL |
| POST | `group/send` | 发送消息（text/emoji/image），返回消息登记 id（msgId） |
| GET | `group/pull` | 拉取我的中转消息（拉走即删，同时记为已读） |
| GET | `group/reads?groupId=` | 我发出消息的已读计数（仅发送者） |
| GET | `group/readers?msgId=` | 某条消息的已读人明细（仅该消息发送者） |

### 记忆笔记（用户级）

md 文件存于 `{wxyd.share.dir}/{用户id}/{标题}.md`，元数据内嵌 YAML frontmatter（name/category/visibility，缺省 public）。

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `note/list` | 我的笔记列表（含预览，按更新时间倒序） |
| GET | `note/search?keyword=` | 按标题模糊搜索公共笔记 |
| GET | `note/get?ownerId=&name=` | 读单篇（自己的任意笔记 / 他人的公共笔记） |
| POST | `note/save` | 新增/更新/改名自己的笔记（正文 ≤500KB） |
| POST | `note/delete` | 删除自己的笔记 |
| POST | `note/migrateOld` | 旧只读笔记迁移至 admin 目录（仅超管，幂等） |

### 菜单管理（仅管理员）

四态模型：`visible=1 且无名单` 全部可见；`visible=0 且无名单` 全部隐藏；`visible=1 且有名单` 白名单可见；`visible=0 且有名单` 黑名单不可见。管理员始终可见全部；首页/我的固定可见。

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `menu/all` | 全部菜单及配置 |
| GET | `menu/visible` | 当前用户可见的菜单 key 列表 |
| POST | `menu/save` | 保存某菜单配置 `{menuKey, visible, userIds}` |

### 文件共享

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `share/list` | 共享文件列表（存于 `wxyd.share.dir`，默认 /apps/shareFile/） |
| POST | `share/upload` / `share/uploadChunk` / `share/merge` | 上传（管理员，支持分片续传） |
| GET | `share/download?name=` | 下载 |
| POST | `share/delete` | 删除（管理员） |

### 用户会话信息

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `user/info` | 返回当前登录用户的 `username` / `role` / `isAdmin`（前端据此决定 SQL 查询菜单是否可见） |

### SQL 查询（仅超级管理员）

直连 wxyd 自有 MySQL 数据源（`mysqlDataSource`），**仅 `role=0` 超级管理员**可用；`/sql/**` 同时受 `AuthInterceptor` 登录拦截。语句经校验仅允许 `SELECT` / `SHOW` / `DESC` / `DESCRIBE` / `EXPLAIN` / `WITH` 单语句，禁止多语句拼接。

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `sql/tables` | 列出当前库所有表名 |
| POST | `sql/query` | 执行查询，body：`{"sql":"...","page":1,"size":20}`；返回列/行/总数/总页数/耗时 |
| GET | `sql/export?type=csv\|xlsx&sql=...` | 导出查询结果（CSV 带 UTF-8 BOM / Excel 由 Apache POI `SXSSFWorkbook` 生成） |

### 证据材料整理

上传多个 PDF / 图片 / Word / Excel，或其 ZIP / 7z 压缩包（可多级嵌套，服务端自动解压展开），合并为**与离线验证过的 v5 输出逐像素一致**的单一 A4 竖向带页码 PDF（104 页真实证据 zip 实测：页脚序列 100% 一致、无 ≥0.5% 差异页）。仅登录可见，菜单管理可配显隐。核心规则（v5）：

- **页码标注解析**：文件名「页码：X-Y」/「页码：X」/「N-M」前缀 → 父文件夹「页码：X」兜底；无标注按顺序补齐。
- **冲突空洞分配**：标注重叠的条目（如命名笔误「21-32」实占 31-32）按实际页数填入第一个空闲页码空洞，无需硬编码。
- **PDF 页矢量嵌入**（`LayerUtility.importPageAsForm`，不栅格化、不裁剪、文字可选中）；横向页旋转 90°（顶边朝左）；多页 PDF 标注 1 页（发票类）自动左右并排压缩。
- **xlsx**：A 列备注文字 + 截图逐条一页，剩余截图按验证过的分配算法两两并排（竖图对 gap=20 不旋转、横向旋转对 gap=14）；**docx**：纯文字页宋体/黑体渲染（bold ≥16pt 居中），含 ≥3 图的考勤类渲染为说明文字 + 图注 4 列网格 1 页。
- **页脚**：`HELVETICA 11pt` 灰色 0.25，底部居中（`FOOTER_Y = A4H - 24`）。

> 关键实现注意：pymupdf 用左上原点而 PDFBox 内容流用底部原点，所有 y 需 `A4H - y_top` 转换；PDF 图片 XObject 占据**单位正方形 [0,1]²**（矩阵系数须用目标 pt 尺寸，非像素比），Form 用点空间 BBox。

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `pdf/merge` | 多文件合并。`files[]` 为 `MultipartFile[]`（≤500 个、≤500MB；支持 PDF / PNG / JPG / docx / xlsx / ZIP / 7Z，压缩包在服务端解压展开）；`meta` 为 JSON：`{"mode":"auto"\|"label","labels":["101",...]}`（labels 仅对直接上传的纯数字标注生效，压缩包内按文件名/文件夹解析）。成功返回 `application/pdf` 流（`Content-Disposition` 含 UTF-8 文件名 `证据材料（A4竖版·带页码）.pdf`）；失败返回 `application/json` `{"errorCode":...,"errorMsg":...}`（HTTP 200，前端按 `content-type` 区分），业务错误码 `000003`、通用错误码 `999999` |

> 注意：`pom.xml` 中 pdfbox 固定为 **2.0.24**（本地仓库已缓存 2.0.24，2.0.32 仅有 `.lastUpdated` 桩未下载）。ZIP / 7z 解压依赖 **commons-compress 1.21 + xz 1.9**；docx/xlsx 解析依赖 **poi-ooxml 3.17**（均本地仓库已缓存）。中文字体渲染使用系统 `C:/Windows/Fonts/simsun.ttc`(宋体) + `simhei.ttf`(黑体)，服务器需为 Windows 或自带字体。`Matrix` 用 6 参数构造、`PDFormXObject.setMatrix` 直传 `AffineTransform`、`setNonStrokingColor` 用浮点版，确保 Java 8 / PDFBox 2.0.24 零告警编译。

### 外部网关（遗留）

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `tbphx.do` | 外部 transData 入口（base64 + URLdecode + 防重放） |

## 🗄 数据库

| 表 | 说明 |
|----|------|
| `users` | 用户（id, username, password SM4, display_name, role 0=管理员/1=普通, status 0=正常/1=待审/2=拒绝） |
| `ding_robot` | 钉钉机器人（user_id, name, access_token SM4, secret SM4） |
| `message` | 留言（user_id, ip, info, time） |
| `group_chat` / `group_member` / `group_join_apply` | 群聊 / 群成员 / 入群申请（V4） |
| `group_msg_transit` | 群聊中转消息（拉取即删，expire_time 7 天兜底；V4，msg_id 见 V5） |
| `group_msg` / `group_msg_read` | 消息登记（含应达人数）/ 已读记录（V5） |
| `menu_config` | 菜单可见性配置（menu_key 唯一，四态模型；V6） |
| `note` / `operinfo` / `action` / `requestlog` / `doc` / `documentFile` | 遗留业务表（用户笔记已改为文件存储，见上） |

## 🔒 安全

- **密码加密**：SM4 CBC + 随机 IV（非确定性），登录时解密比对
- **机器人 SECRET**：SM4 加密存储
- **鉴权**：HTTP Session + AuthInterceptor（按路径白名单）
- **代码沙箱**：Java8-API 执行黑名单（禁止 File/网络/反射/进程/SQL），10 秒超时
- **SQL 查询工具**：仅 `role=0` 超级管理员可用（前端 `user/info` 门控 + 控制器 `role==0` 校验 + `AuthInterceptor` 登录拦截三重防护）；语句白名单仅允许只读查询，禁止 DML/DDL 与多语句
- **密钥**：`env.properties` 已 gitignore，永不提交；SM4 密钥由 `SMUtil.generateSM4Key()` 生成

## 🎨 前端主题

暗色玻璃拟态（glassmorphism）：
- 背景：`linear-gradient(135deg, #0f0c29, #302b63, #24243e)` + 15s 流动动画
- 浮动光晕：`#667eea→#764ba2` / `#f093fb→#f5576c` / `#4facfe→#00f2fe`
- 玻璃面板：`rgba(255,255,255,.08)` + `backdrop-filter: blur(22px)`
- 强调色：`#a78bfa`
- 按钮：`linear-gradient(135deg, #667eea, #764ba2)`

## 📝 备注

- 运行时必须使用 JDK（非 JRE），因为 Java8-API 模块的 `javax.tools.JavaCompiler` 需要 JDK 的 `tools.jar`
- 从 fat jar 运行时，`CodeExecutionService` 自动从 `BOOT-INF/lib/` 提取 JUnit/Hamcrest jar 到临时文件，解决嵌套 jar classpath 不可达问题
- `tbphx.do` 外部网关保留供遗留系统集成，前端已改为直接调用各管理端点
