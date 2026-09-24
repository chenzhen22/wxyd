---
name: Jenkins 安装
category: 环境搭建
---

# 下载 Java 11

Java 11 下载地址：[https://repo.huaweicloud.com:8443/artifactory/java-local/jdk/11.0.2+9/](https://repo.huaweicloud.com:8443/artifactory/java-local/jdk/11.0.2+9/)

将 `jdk-11.0.2_linux-x64_bin.tar.gz` 包放入 `/usr/src/java11/` 目录下，执行解压：

```bash
tar -xvf jdk-11.0.2_linux-x64_bin.tar.gz
```

1. 用文本编辑器打开 JDK 安装目录下的文件：`/usr/src/java11/jdk-11.0.2/conf/security/java.security`。
2. 搜索关键字 `jdk.tls.disabledAlgorithms`，在这个变量定义的最前面加上：`TLSv1.3, `（注意有英文逗号和空格）。

# 安装 Jenkins

① 执行命令导入仓库并导入密钥：

```bash
wget -O /etc/yum.repos.d/jenkins.repo http://pkg.jenkins-ci.org/redhat/jenkins.repo --no-check-certificate
rpm --import https://jenkins-ci.org/redhat/jenkins-ci.org.key
```

② 安装完成后，修改仓库配置：

```bash
vi /etc/yum.repos.d/jenkins.repo
# gpgcheck=0   # 修改为 0 不进行检测
```

③ Yum 安装 Jenkins：

```bash
yum install jenkins-2.459
```

④ 修改 Jenkins 中的 Java 路径：

```bash
vim /usr/lib/systemd/system/jenkins.service
```

- `Environment="JENKINS_JAVA_CMD=/usr/src/java11/jdk-11.0.2/bin/java"` 取消这一行的注释，并将 Java 路径写入
- `Environment="JENKINS_PORT=8000"` 修改端口

⑤ 启动 Jenkins：

```bash
systemctl start jenkins     # 启动
systemctl stop jenkins      # 停止
systemctl restart jenkins   # 重启
systemctl status jenkins    # 查看状态
```

# 访问 Jenkins

打开浏览器访问：[http://172.2.17.238:8000/](http://172.2.17.238:8000/)

# 获取管理员密码

```bash
cat /var/lib/jenkins/secrets/initialAdminPassword
```

安装推荐的插件即可，使用 `admin` 继续操作。
