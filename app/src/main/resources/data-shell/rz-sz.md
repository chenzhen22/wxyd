---
name: Rz 与 Sz 传输
category: 文件传输
order: 1
---

## 介绍

rz/sz 是基于 ZMODEM 协议的终端文件传输工具（lrzsz 包），必须配合 Xshell、SecureCRT、MobaXterm 等支持 ZMODEM 的终端使用。rz 从本机上传到服务器，sz 从服务器下载到本机，小文件交互式传输最顺手；大文件与自动化场景请用 scp/sftp。

核心特点：
- **终端内直达**：不用另开 sftp 工具，在会话窗口里直接拖拽/敲命令
- **依赖终端支持**：走的是终端仿真层的 ZMODEM，ssh 原生终端（如 linux 下 ssh 命令）不支持
- **rz 上传 sz 下载**：记法 r=receive（服务器接收）、s=send（服务器发送）
- **适合小批量**：配置文件、小日志、临时包最方便，大文件稳定性不如 scp

## 语法

### 安装

```bash
yum install -y lrzsz
```
- 描述: CentOS 安装；Ubuntu 为 apt install lrzsz

### 上传文件

```bash
rz
```
- 描述: 弹出文件选择框（或直接把文件拖进 Xshell 窗口），上传到当前目录

### 上传覆盖

```bash
rz -y
```
- 描述: -y 遇到同名文件直接覆盖，不加则自动改名跳过

### 下载文件

```bash
sz app.log
```
- 描述: 把服务器上的 app.log 下载到终端设置的本机下载目录

### 下载多个文件

```bash
sz a.log b.log /opt/app/conf/*.yml
```
- 描述: 支持多参数与通配符，一次性批量下载

### 二进制模式

```bash
sz -b archive.tar.gz
```
- 描述: -b 强制二进制传输（默认自动判断），图片/压缩包建议显式指定

## 示例

### 日常上传下载

- 描述: 配置文件的来回搬运

```bash
# 上传本地 nginx.conf 到服务器当前目录（拖拽进终端等效）
rz -y

# 下载服务器日志到本机（落在本机下载目录）
sz /var/log/app.log

# 批量下载配置文件
sz /opt/app/config/*.yml
```

### 搭配 tar 打包传输目录

- 描述: 目录先打包再 sz，规避 ZMODEM 逐文件慢的问题

```bash
# 目录不能直接 sz，先打包成一个文件
tar -czf config-$(date +%Y%m%d).tar.gz /opt/app/config/

# 单文件传输，稳定快速
sz config-20260915.tar.gz

# 本机解包后如需回传：rz -y 上传回来再 tar -xzvf 解压
```

### 适用边界与替代方案

- 描述: 什么时候该换 scp

```bash
# 场景 1：大文件（>500M）或服务器间传输 → 用 scp
scp -C bigdata.tar.gz root@192.168.1.100:/data/

# 场景 2：自动化脚本（无人值守）→ 用 scp/rsync
rsync -avz /opt/app/ backup@host:/backup/app/

# 场景 3：Xshell 里临时拉一个小日志看一眼 → sz 最快
sz /opt/app/logs/error.log
```