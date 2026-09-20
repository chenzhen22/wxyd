---
name: SCP 远程拷贝
category: 文件传输
order: 1
---

## 介绍

scp（secure copy）是基于 SSH 协议的加密文件传输命令，用于在本地与远程主机之间、或两台远程主机之间复制文件和目录。数据全程加密，认证方式与 ssh 一致（密码或密钥），是 Linux 运维中最常用的文件传输工具。

核心特点：
- **基于 SSH**：默认走 22 端口，复用 ssh 的认证与加密通道
- **双向传输**：本地→远程、远程→本地、远程→远程均可
- **目录递归**：-r 参数复制整个目录
- **两种认证**：密码认证（交互输入）或密钥认证（-i 指定私钥）

## 语法

### 本地复制到远程

```bash
scp local_file user@host:/remote/path/
```
- 描述: 将本地文件复制到远程主机指定路径，需输入远程用户密码

### 远程复制到本地

```bash
scp user@host:/remote/file /local/path/
```
- 描述: 将远程文件下载到本地指定路径

### 递归复制目录

```bash
scp -r local_dir user@host:/remote/path/
```
- 描述: -r 递归复制整个目录及其子目录，目录传输必加

### 指定远程端口

```bash
scp -P 2222 local_file user@host:/remote/path/
```
- 描述: -P 大写指定远程 sshd 端口（区别于 ssh 的小写 -p）

### 保留文件属性

```bash
scp -p local_file user@host:/remote/path/
```
- 描述: -p 小写保留修改时间、访问时间和权限模式

### 压缩传输

```bash
scp -C local_file user@host:/remote/path/
```
- 描述: -C 开启压缩，适合大文件或文本类文件，节省带宽

### 指定私钥认证

```bash
scp -i ~/.ssh/id_rsa local_file user@host:/remote/path/
```
- 描述: -i 指定私钥文件，免密传输，常用于脚本自动化

### 限制传输带宽

```bash
scp -l 5000 local_file user@host:/remote/path/
```
- 描述: -l 限制带宽单位为 Kbit/s，5000 约 625KB/s，避免占满带宽

## 示例

### 日常上传下载

- 描述: 最常用的上传与下载场景

```bash
# 上传本地 jar 包到远程服务器
scp app.jar root@192.168.1.100:/opt/app/

# 上传并改名
scp app.jar root@192.168.1.100:/opt/app/app-v2.jar

# 下载远程日志到本地当前目录
scp root@192.168.1.100:/var/log/app.log ./

# 下载后指定本地路径
scp root@192.168.1.100:/etc/nginx/nginx.conf ./backup/
```

### 批量传输文件

- 描述: 一次传多个文件、通配符、目录

```bash
# 多文件上传，目的地必须是目录
scp file1.log file2.log root@192.168.1.100:/tmp/logs/

# 通配符批量上传
scp *.jar root@192.168.1.100:/opt/app/lib/

# 递归上传整个目录
scp -r ./config root@192.168.1.100:/opt/app/

# 递归下载整个目录
scp -r root@192.168.1.100:/opt/app/logs ./logs-backup/
```

### 非默认端口与密钥认证

- 描述: 组合参数实现脚本化免密传输

```bash
# 远程 sshd 端口为 2222，用私钥免密传输
scp -i ~/.ssh/id_rsa -P 2222 app.jar root@192.168.1.100:/opt/app/

# 保留属性 + 压缩 + 密钥，适合定时备份脚本
scp -i ~/.ssh/id_rsa -C -p backup.sql root@192.168.1.100:/backup/mysql/

# 常用组合建议写进 shell 别名或脚本变量
REMOTE="root@192.168.1.100"
PORT=2222
KEY=~/.ssh/id_rsa
scp -i $KEY -P $PORT data.tar.gz $REMOTE:/data/
```

### 远程到远程拷贝与带宽限制

- 描述: 两台远程主机间中转拷贝、限制带宽

```bash
# 从远程 A 拷贝到远程 B（本机作为中转，默认走本机）
scp root@192.168.1.100:/data/file.tar.gz root@192.168.1.101:/data/

# 限制带宽 5000 Kbit/s，避免影响业务流量
scp -l 5000 bigfile.iso root@192.168.1.100:/opt/

# -3 参数：两台远程主机间拷贝经本机中转（默认行为即 -3）
scp -3 root@192.168.1.100:/a.txt root@192.168.1.101:/b.txt
```