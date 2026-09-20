---
name: Maven 构建
category: 构建工具
order: 1
---

## 介绍

Maven 是 Java 项目的构建与依赖管理工具，`mvn` 命令驱动从编译、测试、打包到安装部署的整条生命周期，并按 GAV 坐标自动解析、下载依赖。多模块项目里「只构建某个模块」「排查依赖冲突」「跳过测试加速构建」是日常最频繁的三个动作，本节按实际使用频率整理，命令可直接复制。

核心特点：
- **三段坐标**：groupId:artifactId:version 唯一定位一个构件，本地仓库默认在 ~/.m2/repository
- **生命周期三段**：clean / default / site，default 内含 compile→test→package→install→deploy，指定阶段会依次执行其前面的所有阶段
- **依赖自动传递**：声明一个依赖，其下游依赖自动引入；冲突按「最短路径优先，同深度先声明优先」调节
- **多模块 -pl 与 -am**：-pl 精确指定模块，-am 连带构建它依赖的模块，避免全量编译
- **两种跳过测试不同**：-DskipTests 编译测试代码但不运行，-Dmaven.test.skip=true 连编译都跳过

## 语法

### 完整构建

```bash
mvn clean install
```
- 描述: 清空 target 后编译、测试、打包并安装到本地仓库，最常用的全量构建

### 只编译

```bash
mvn clean compile
```
- 描述: 只执行到 compile 阶段，改完代码快速验证语法，比打包快得多

### 打包跳过测试

```bash
mvn clean package -DskipTests
```
- 描述: 编译测试类但不运行测试，兼顾速度与测试代码的编译校验，日常首选

### 彻底跳过测试

```bash
mvn clean package -Dmaven.test.skip=true
```
- 描述: 连测试代码都不编译，构建最快；测试类本身编译不过时用它绕过

### 多模块指定模块构建

```bash
mvn clean package -pl app -am -DskipTests
```
- 描述: -pl 指定模块（逗号分隔多个），-am 同时构建其依赖模块；-amd 则构建依赖它的模块

### 查看依赖树

```bash
mvn dependency:tree
```
- 描述: 打印完整依赖树，定位某个 jar 从哪条路径被引入的第一手段

### 过滤依赖树

```bash
mvn dependency:tree -Dincludes=org.slf4j:*
```
- 描述: 只看命中 groupId:artifactId 的子树；支持通配，排查冲突时输出更聚焦

### 查找无用与缺失依赖

```bash
mvn dependency:analyze
```
- 描述: 列出「声明了但没用到」（可删）与「用到但没声明」（靠传递依赖引入，有风险）的依赖

### 查看生效的 POM

```bash
mvn help:effective-pom
```
- 描述: 输出父 POM、profile、属性全部合并后的最终配置，排查「配置不生效」必备

### 强制更新依赖

```bash
mvn clean install -U
```
- 描述: 忽略本地缓存重新检查远程仓库，SNAPSHOT 依赖不更新时先试它

### 离线构建

```bash
mvn -o clean package
```
- 描述: 只用本地仓库，不联网；内网环境或需要确定性构建时使用，缺依赖会直接失败

### 指定 profile

```bash
mvn clean package -Pprod
```
- 描述: 激活 pom 中定义的 profile（生产配置、打包方式等）；-P!dev 可显式排除

### 运行 Spring Boot

```bash
mvn spring-boot:run -pl app
```
- 描述: 直接用插件启动应用，改代码后重启方便；需模块已引入 spring-boot-maven-plugin

### 查看版本与调试

```bash
mvn -v
```
- 描述: 显示 Maven、JDK、系统信息；排查版本不符先看这里，加 -X 输出调试日志

## 示例

### 排查依赖冲突

- 描述: Slf4j 多绑定、NoSuchMethodError 这类问题的定位流程

```bash
#!/bin/bash
# 用途：定位某个依赖的引入路径与冲突
set -euo pipefail

PKG="org.slf4j"

# 1. 看谁引入了这个依赖（含被忽略的分支）
mvn dependency:tree -Dincludes="${PKG}:*"

# 2. Maven 3.0 之前需要 -Dverbose 才显示被忽略依赖；
#    新版 maven-dependency-plugin（3.x 起）默认就会标出
#    omitted for conflict / omitted for duplicate
mvn dependency:tree -Dverbose

# 3. 全局搜索：确认是不是被其他模块间接带入
mvn dependency:tree | grep -n "${PKG}"

# 4. 看最终实际参与构建的版本
mvn help:effective-pom | grep -A2 "${PKG}"

# 判定要点：
# - 同一依赖出现两次且版本不同 → 冲突，用 <exclusions> 排除低版本
# - 路径最短者生效；同深度则 POM 中先声明者生效
```

### 精准构建多模块

- 描述: 大项目里只构建改动模块，把十几分钟压到一分钟

```bash
#!/bin/bash
# 用途：只构建改动模块及其依赖，避免全量编译
set -euo pipefail

MODULE="app"

# 只编译该模块及其上游依赖（-am = also make）
mvn clean install -pl "$MODULE" -am -DskipTests

# 只编译该模块本身，要求其依赖已在本地仓库（-N 只处理根项目同理）
mvn clean package -pl "$MODULE" -o -DskipTests

# 同时构建依赖它的下游模块（-amd = also make dependents）
mvn clean package -pl "$MODULE" -amd -DskipTests

# 多模块一起：逗号分隔
mvn clean package -pl common,app -am -DskipTests

# 跳过指定模块（! 前缀排除）
mvn clean package -pl '!dataDict' -am -DskipTests
```

### 配置国内镜像加速

- 描述: 首次构建下载慢或卡在 central 时的标准解法

```bash
#!/bin/bash
# 用途：为 Maven 配置阿里云镜像，显著提升依赖下载速度
set -euo pipefail

MAVEN_HOME="${MAVEN_HOME:-/opt/maven}"
SETTINGS="${MAVEN_HOME}/conf/settings.xml"

# 备份原配置
cp "$SETTINGS" "${SETTINGS}.bak.$(date +%Y%m%d)" 2>/dev/null || true

# 在 <mirrors> 内加入镜像（用 python 精确插入更稳妥，此处示意配置内容）
cat > /tmp/mirror-snippet.xml <<'EOF'
<mirror>
  <id>aliyunmaven</id>
  <mirrorOf>*</mirrorOf>
  <name>阿里云公共仓库</name>
  <url>https://maven.aliyun.com/repository/public</url>
</mirror>
EOF

echo "请将 /tmp/mirror-snippet.xml 内容插入 $SETTINGS 的 <mirrors> 节点内"

# 若依赖已损坏导致构建失败，清掉 lastUpdated 标记后重试
find ~/.m2/repository -name "*.lastUpdated" -delete
mvn clean install -U -DskipTests
```

### CI 环境构建脚本

- 描述: 无交互、可重复、失败即停的构建脚本

```bash
#!/bin/bash
# 用途：CI 流水线中的构建与归档
set -euo pipefail
trap 'echo "[ERROR] 构建失败于第 $LINENO 行" >&2' ERR

APP_MODULE="app"
BUILD_DIR="/opt/ci/workspace"
ARTIFACT_DIR="/opt/ci/artifacts"
TAG="$(date +%Y%m%d%H%M%S)"

echo "==> 1. 环境确认"
mvn -v

echo "==> 2. 清理并构建（跳过测试，测试由独立阶段执行）"
mvn -B clean package -pl "$APP_MODULE" -am -DskipTests

echo "==> 3. 校验产物"
JAR="${BUILD_DIR}/${APP_MODULE}/target/${APP_MODULE}-1.0.0.jar"
if [ ! -f "$JAR" ]; then
    echo "[ERROR] 未找到产物 $JAR" >&2
    exit 1
fi
ls -lh "$JAR"

echo "==> 4. 归档带时间戳的副本"
mkdir -p "$ARTIFACT_DIR"
cp "$JAR" "${ARTIFACT_DIR}/${APP_MODULE}-${TAG}.jar"
echo "==> 归档完成: ${ARTIFACT_DIR}/${APP_MODULE}-${TAG}.jar"
```

### 本项目 wxyd 构建命令

- 描述: 按 CLAUDE.md 约定整理，可直接复制使用

```bash
#!/bin/bash
# 用途：wxyd 项目常用构建与启动命令
set -euo pipefail

# 全量构建（跳过测试）
mvn clean install -DskipTests

# 仅编译 common（其他模块依赖它，改了 common 先装它）
mvn clean install -pl common -DskipTests

# 构建单个模块（-am 连带构建其依赖）
mvn clean package -pl batch -am -DskipTests
mvn clean package -pl api -am -DskipTests
mvn clean package -pl tbpApply -am -DskipTests

# 打包 app 模块（本项目唯一模块，等价于全量）
mvn clean package -pl app -DskipTests

# 运行测试
mvn test -pl common

# 通过 Spring Boot 插件启动
mvn spring-boot:run -pl app

# 提示：application.yml 中端口写的是 ${PORT:8090}，
# 若环境中存在 PORT 变量会被截走，启动时显式覆盖更稳妥：
# java -jar app/target/app-1.0.0.jar --server.port=8090
```
