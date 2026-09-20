---
name: Wget 下载
category: 网络工具
order: 6
---

## 介绍

wget 是命令行下载工具，支持 HTTP/HTTPS/FTP 协议，擅长单文件下载、断点续传、镜像整站。配合定时任务可做自动化拉取；交互式补全、递归下载等更复杂场景可对比 curl 选用（wget 专注下载，curl 专注请求调试）。

核心特点：
- **断点续传**：-c 中断后接着下，大文件传输必备
- **递归镜像**：-r/-m 可整站抓取，配合参数控制深度
- **后台静默**：-b 放后台、-q 静默，适合脚本
- **重试友好**：--tries/-T 自动重试与超时，弱网环境稳健

## 语法

### 基本下载

```bash
wget https://example.com/app.tar.gz
```
- 描述: 下载到当前目录，文件名取自 URL

### 指定文件名

```bash
wget -O myapp.tar.gz https://example.com/download/v2
```
- 描述: -O 重命名保存；URL 无文件名时必用

### 断点续传

```bash
wget -c https://example.com/bigfile.iso
```
- 描述: -c 续传未完成的下载，中断重跑同一命令即可

### 后台下载

```bash
wget -b -c https://example.com/bigfile.iso
```
- 描述: -b 放后台，进度写入 wget-log；配合 nohup 更稳

### 限速下载

```bash
wget --limit-rate=1m https://example.com/big.zip
```
- 描述: 限速 1MB/s，避免占满业务带宽

### 重试与超时

```bash
wget -t 3 -T 30 --waitretry=5 URL
```
- 描述: -t 重试 3 次、-T 单次超时 30 秒、--waitretry 重试间隔递增

### 镜像整站

```bash
wget -m -p -np -k https://docs.example.com/
```
- 描述: -m 镜像、-p 含页面资源、-p 不出上级目录、-k 链接转本地

### 携带认证下载

```bash
wget --user=admin --password=xxx ftp://host/file.zip
wget --header="Authorization: Bearer token" URL
```
- 描述: FTP 账密或 HTTP 头认证下载内网资源

## 示例

### 日常下载场景

- 描述: 软件包与安装脚本

```bash
# 下载安装包并改名
wget -O jdk8.tar.gz https://example.com/jdk-8u391-linux-x64.tar.gz

# 大文件后台 + 续传下载
wget -b -c https://mirrors.example.com/centos.iso
tail -f wget-log    # 查看进度

# 从内网制品库拉包（带超时重试）
wget -t 3 -T 30 http://nexus.local:8081/repository/app-1.0.0.jar
```

### 脚本化拉取

- 描述: 健康检查与定时同步

```bash
# 静默下载 + 退出码判断（配合 crontab 拉取日报）
wget -q -O /tmp/report.csv "https://api.example.com/report?date=$(date -d -1 day +%F)"
[ $? -eq 0 ] && echo "下载成功" || echo "下载失败"

# 限速拉备份，不抢业务带宽
wget --limit-rate=2m -c http://bak-host/data/full-$(date +%Y%m%d).tar.gz
```

### 目录与整站抓取

- 描述: 批量下载与镜像

```bash
# 下载某目录下所有指定类型文件（借助 grep 圈 URL）
wget -r -np -A "*.pdf" https://docs.example.com/manual/

# 镜像文档站供离线浏览
wget -m -k -p -np https://docs.example.com/
```