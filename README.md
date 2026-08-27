# 小泽助手（wxyd）

基于 Spring Boot 2.2.5 的轻量运维管理平台，提供用户管理、留言板、钉钉机器人消息推送、Java 8 API 交互式学习等功能。前后端一体，单 jar 部署。

## ✨ 功能模块

| 模块 | 说明 |
|------|------|
| 🏠 首页 | 欢迎面板 + 实时时钟 |
| 👤 用户管理 | 注册审批制（超级管理员审批），SM4 加密密码，HTTP Session 鉴权 |
| 💬 留言板 | 按用户 ID 鉴权（管理员删任意 / 用户删自己的），最多保留 50 条，昵称显示名回退用户名 |
| 🤖 钉钉机器人 | 每用户独立机器人 CRUD，发送文本/文件消息（文件→zip→base64，>20KB 分块 ≤18KB），文件限制 200KB |
| ☕ Java8-API | 384 个 Java API 文档 + 在线编辑运行测试代码（javax.tools.JavaCompiler + JUnit 4，10 秒超时，安全黑名单） |

## 🛠 技术栈

| 层级 | 技术 |
|------|------|
| 语言 / 运行时 | **Java 8**（JDK，非 JRE — Java8-API 模块需 `javax.tools.JavaCompiler`） |
| 框架 | Spring Boot 2.2.5.RELEASE |
| ORM | MyBatis 3.0.0（手动 SqlSessionFactoryBean，`mapUnderscoreToCamelCase`） |
| 数据库 | MySQL 8.0.30 + HikariCP 连接池 |
| 安全 | 国密 SM4（CBC + PKCS7Padding + 随机 IV）加密密码与机器人 SECRET，BouncyCastle |
| HTTP 客户端 | OkHttp 4.9.3 |
| 前端 | jQuery 1.8.3 + 暗色玻璃拟态（glassmorphism）主题 |
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

# 用户 & 钉钉机器人表
mysql -u root -p < app/src/main/resources/sql/V2__user_robot.sql
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
│   │   │                             #   RobotController, ApiController(tbphx.do)
│   │   ├── javaapi/                  # Java8 API 学习平台（独立子包）
│   │   │   ├── model/                #   ApiClass, ApiMethod, TestCase, CodeResult
│   │   │   ├── service/              #   ApiDataService, CodeExecutionService
│   │   │   └── controller/           #   JavaApiController
│   │   ├── service/                  # AuthService, UserService, MessageService, RobotService
│   │   ├── filter/                   # RepeatReadFilter（multipart 免包装）
│   │   ├── mock/                     # MockAspect（mock 模式兜底 tbphx.do）
│   │   └── aspect/                   # ControllerAspect（traceId/clientIp MDC）
│   ├── src/main/resources/
│   │   ├── application.yml           # port 8090, context-path /api
│   │   ├── env.properties            # 真实密钥/数据库（gitignore）
│   │   ├── env.properties.example    # 配置模板
│   │   ├── data/                     # 384 个 Java API 文档（Markdown）
│   │   ├── mapper/MysqlMapper.xml    # MyBatis SQL
│   │   ├── sql/V2__user_robot.sql    # users + ding_robot 建表
│   │   └── static/                   # 前端页面 + CSS + JS
│   │       ├── login.html            # 登录/注册（暗色玻璃 + 流动渐变动画）
│   │       ├── api.html              # 管理后台（5 大模块）
│   │       └── Assets/
│   │           ├── css/admin.css     # 暗色玻璃主题
│   │           └── js/
│   │               ├── index.js      # 用户/机器人/留言板逻辑
│   │               └── javaapi.js    # Java API 学习平台逻辑
│   └── pom.xml
├── wxyd.sql                          # 基础数据库建表脚本
└── pom.xml                           # 父 POM
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
| `note` / `operinfo` / `action` / `requestlog` / `doc` / `documentFile` | 遗留业务表 |

## 🔒 安全

- **密码加密**：SM4 CBC + 随机 IV（非确定性），登录时解密比对
- **机器人 SECRET**：SM4 加密存储
- **鉴权**：HTTP Session + AuthInterceptor（按路径白名单）
- **代码沙箱**：Java8-API 执行黑名单（禁止 File/网络/反射/进程/SQL），10 秒超时
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
