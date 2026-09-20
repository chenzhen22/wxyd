---
name: SSH 免密登录
category: 网络工具
order: 3
---

## 介绍

SSH 是远程登录的标准协议，免密登录基于非对称密钥对：公钥放服务器，私钥留本地。配置一次后 scp、rsync、git、ansible 等全部免密，自动化脚本的前提条件。

核心特点：
- **非对称认证**：私钥签字、公钥验证，比密码更安全
- **一次配置终身受益**：ssh/scp/sftp/rsync/git 全部免密
- **config 简化连接**：给主机起别名，免敲长命令
- **跳板机支持**：-J 一跳直达内网机器

## 语法

### 生成密钥对

```bash
ssh-keygen -t rsa -b 4096 -C "comment"
```
- 描述: -t 算法（推荐 ed25519 或 rsa）、-b 长度、-C 备注；回车三次用默认路径

### 分发公钥

```bash
ssh-copy-id user@host
```
- 描述: 把本机公钥写入远程 ~/.ssh/authorized_keys，之后免密登录

### 指定端口生成与分发

```bash
ssh-copy-id -p 2222 user@host
```
- 描述: 远程 sshd 非默认端口时用 -p

### config 别名配置

```bash
Host myserver
    HostName 192.168.1.100
    User root
    Port 2222
    IdentityFile ~/.ssh/id_rsa
```
- 描述: 写入 ~/.ssh/config 后直接 ssh myserver 即可连接

### 跳板机连接

```bash
ssh -J jumpuser@jump-host targetuser@target-host
```
- 描述: -J 指定跳板机，先登录跳板再连目标，内网穿透常用

### 远程执行命令

```bash
ssh user@host "df -h | grep /data"
```
- 描述: 非交互执行远程命令，脚本批量巡检的基础

## 示例

### 完整免密配置流程

- 描述: 三步实现 A 机器免密登录 B 机器

```bash
# 1. 在本机生成密钥对（已有则跳过）
ssh-keygen -t ed25519 -C "ops@local"

# 2. 把公钥分发到目标机器（输一次密码）
ssh-copy-id root@192.168.1.100

# 3. 验证免密生效
ssh root@192.168.1.100 "hostname"
# 直接输出主机名不再要密码，即成功
```

### 多服务器批量巡检

- 描述: config + for 循环批量执行命令

```bash
# ~/.ssh/config 里配好 prod1 prod2 prod3 三个别名
for h in prod1 prod2 prod3; do
    echo "=== $h ==="
    ssh $h "uptime && df -h /data | tail -1"
done
```

### 文件传输与端口转发

- 描述: 免密后的 scp 直传、本地端口转发

```bash
# 免密 scp 直传
scp app.jar prod1:/opt/app/

# 本地端口转发：访问本机 3307 即等于访问远程内网 MySQL
ssh -L 3307:127.0.0.1:3306 user@db-host -N
mysql -h 127.0.0.1 -P 3307 -u app -p
```