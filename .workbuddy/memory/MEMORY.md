# wxyd 项目长期备忘

## 技术栈与访问入口

- Spring Boot 多模块（`app` 模块为主），JDK 1.8，MySQL（远程，见 `env.properties`）。
- 访问入口 **`http://localhost:8090/api/`** —— context-path 是 `/api`，**不是根路径**；
  健康检查 `/api/actuator/health`。
- 构建：`mvn clean package -pl app -DskipTests -B`
  （mvn = `D:\cz\java\maven\apache-maven-3.8.4\bin\mvn.cmd`，本地仓库 `D:\cz\repository`），
  产物 `app/target/app-1.0.0.jar`（约 90MB）。
- 运行：`java -jar app/target/app-1.0.0.jar --server.port=8090`
  （JDK = `D:\cz\project\jdk1.8.0_181`）。

## 启动必避的两个坑

1. **必须显式 `--server.port=8090`**。`application.yml` 写的是 `port: ${PORT:8090}`，
   而宿主环境注入了 `PORT`，会导致应用去监听 60881（撞 WorkBuddy 自身端口）而启动失败；
   日志只报「Port 60881 was already in use」，完全看不出跟 8090 有关。
2. **不要用 PowerShell 直接传 `-Dfile.encoding=UTF-8`**（会被拆坏，报「找不到或无法加载主类」），
   改用 `$env:JAVA_TOOL_OPTIONS="-Dfile.encoding=UTF-8"`。
3. 重新打包前先停掉在跑的 java 进程，否则 Windows 会锁住 jar 导致 `clean` 失败。

## 接口验证套路（所有业务接口都在鉴权后）

`AuthInterceptor` 保护 `/user/** /robot/** /javaapi/** /shellscript/** /dubbo/**`。
未登录时**页面请求会 302 到 `login.html` 并返回 200 + 登录页 HTML**，直连容易误判成功。
正确做法：先 `POST /api/login`，body `{"body":{"username":"admin","password":"..."}}`
（凭据见 `env.properties` 的 `wxyd.admin.*`），带 Cookie 再请求接口，
并附 `X-Requested-With: XMLHttpRequest` + `Accept: application/json`。

## 两套只读资料库（加内容前先选对目录）

- `app/src/main/resources/data/` —— **JDK 类 API 速查**（array-list、hash-map 等），
  走 `ApiDataService`，对应「Java API」页。CLI/构建工具不要放这里。
- `app/src/main/resources/data-shell/` —— **CLI 命令工具速查**，走 `ShellScriptDataService`，
  对应「Shell 脚本学习」页。curl/wget/scp/mysql/mvn/dmidecode 都在这。
- 纯数据驱动，**加 md 无需改 Java/前端**；重启加载，或调 `POST /api/shellscript/topics/reload` 热重载。

## data-shell 主题 md 格式约定（新增必须遵守）

front-matter：`name`（唯一键，用于列表与详情路由）/ `category` / `order`。
固定三段，缺一段对应区域就为空：

- `## 介绍` — 纯文本，服务端剥掉代码块，列表页截断到 150 字符。
- `## 语法` — 每节 `### 名称`，**首个 ```bash 代码块只取第一行作为签名，其余行丢弃**；
  说明必须写成 `- 描述: ...` 单行（多行会被拼成一段）。
  想让语法节展示多条命令 → 拆成多个 `###`，别写多行代码块。
- `## 示例` — 每节 `### 名称` + `- 描述: ...` + 一个完整 ```bash 块（**保留全部行**，可多行）。

现有分类与数量：系统运维 13 / 网络工具 9 / 文件操作 7 / Shell 进阶 6 / 文本处理 5 /
文件传输 3 / 实战模板 2 / 数据库 2 / 构建工具 1，合计 48。
`order` 为分类内序号，新增时取该分类最大 order + 1。

## 新增业务模块的通用套路（照 Dubbo 模块抄）

1. 新包 `com.cyz.<模块>`（controller / service / model）。
2. `config/WebMvcConfig` 把 `/<模块>/**` 加进 AuthInterceptor 的 `addPathPatterns`。
3. 前端 `static/api.html` 加左侧 `menu-item`(data-target) 与对应 `module-box`。
4. 新建 `static/Assets/js/<模块>.js`，暴露 `{init: init}`，在 `index.js` 菜单 click 里加懒加载钩子。
5. CSS 优先复用 `cert-card` / `cert-form-row` / `btn-main` 等既有类。

要点：
- **控制器不要实现 `CommController`**，否则被 `MockAspect`(@Order(1)) 与
  `ControllerAspect`(pointcut `com.cyz.controller.CommController+`) 兜底拦截。
- 二进制下载用**原生 `fetch`**，别用 jQuery 1.8.3（blob 不便，且其全局 ajaxError 会抢 401 跳转）。
- 静态资源带 context-path：`http://localhost:8090/api/api.html`（写成 `/api.html` 会 404）。
- 可用依赖：jsoup 1.17.2、okhttp 4.9.3、hutool-all 5.8.34 均已在 pom 中，加功能一般无需引新包。

## TLS：内网金融站点证书问题（2026-09-20）

- 目标站由 **CFCA** 签发时，JDK 1.8.0_181 的 cacerts **不含任何 CFCA 根**（共 105 条），
  会报 `PKIX path building failed`；而 curl/浏览器正常（走系统信任库）。
- 解法：根证书 `.pem` 放进 `app/src/main/resources/certs/`，
  `com.cyz.archive.util.ExtraTrustStore` 自动加载并与 JVM 默认信任库合并，
  只追加可信锚点，主机名/有效期/链校验保持开启。**加证书无需改代码。**
- 现有 `certs/cfca-ev-root.pem`（CFCA EV ROOT，有效至 2029-12-31）。
- **不要用「关闭证书校验」当默认解法**（页面上留了勾选项，但默认关闭，仅自签证书场景用）。

## 本机环境限制与可用工具

- **2026-09-20 更新：Bash 工具已恢复可用**（`ls`/`tail`/`grep`/`curl`/`rm` 均正常），
  优先用它——比 PowerShell 干净得多，中文和管道都正常。
  之前「PortableGit 缺 ls/grep/cat」的记录已过期，不必再绕道 PowerShell。
- **PowerShell 不回显 stdout**：需要看输出时，`$r | Set-Content 文件` 再用 Read 读，
  或直接改用 Bash。PowerShell 里 `Get-Process -Name java | Stop-Process -Force`
  这类批量杀进程有时会让命令本身被中断（exit -1）。
- **Git Bash 的 `/tmp` 与 Windows Python 的路径不一致**：curl `-o /tmp/x.json` 存的文件，
  Windows Python 打不开。要落盘就用项目内相对路径，别用 `/tmp`。
- 用 Bash 跑 Windows Python 时写 `"C:/Users/爱亲/.workbuddy/binaries/python/versions/3.13.12/python.exe"`。
- 文件必须 UTF-8 **无 BOM** + LF 换行（Write 工具默认符合）。
- **`.workbuddy/app.log` 被运行中的应用占用**，`rm` 会因 trash 失败而报 safe-delete 错误，
  属正常现象，不要去删它。
- **后台启动的应用活不过本轮对话**：用 `run_in_background` 起的 java 进程，
  约 1~2 分钟后会被宿主回收（日志无异常、无 shutdown，退出码 1）；
  `Start-Process` 脱离进程树也一样（redirect 日志 0 字节）。实测三次无一存活。
  → 想长期跑请让用户自己执行（IDEA 或终端），我这边只负责「启动 → 验证 → 告知」。
  - 手动启动命令：
    `cd /d D:\GIT\wxyd\app\target`
    `set JAVA_TOOL_OPTIONS=-Dfile.encoding=UTF-8`
    `D:\cz\project\jdk1.8.0_181\bin\java.exe -jar app-1.0.0.jar --server.port=8090`
- 校验脚本一律写成 `.py` 文件再执行，**不要用 bash heredoc 传 Python**（易被解析破坏）。
