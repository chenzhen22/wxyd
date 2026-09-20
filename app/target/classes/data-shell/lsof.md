---
name: Lsof 文件查看
category: 网络工具
order: 4
---

## 介绍

lsof（list open files）列出进程打开的所有资源：普通文件、目录、网络套接字、管道。在 Linux「一切皆文件」的设计下，它是端口占用排查、删除文件回收、进程行为分析的多面手，与 netstat 互为补充。

核心特点：
- **端口反查进程**：lsof -i:8080 直接看谁占着端口
- **恢复删除文件线索**：进程还持有的已删除文件能被找到
- **进程视角**：看某进程打开的全部文件与网络连接
- **需 root 权限**：看其他用户的进程信息需要 sudo

## 语法

### 按端口查

```bash
lsof -i:8080
```
- 描述: 查看占用 8080 端口的进程，最常用

### 按协议与端口范围

```bash
lsof -i TCP:1-1024
```
- 描述: 查 TCP 1-1024 端口范围的占用；-i UDP 同理

### 按进程查

```bash
lsof -p 1234
```
- 描述: 列出 PID 1234 进程打开的所有文件与套接字

### 按文件查

```bash
lsof /var/log/app.log
```
- 描述: 查看哪些进程正在使用某文件

### 按用户查

```bash
lsof -u appuser
```
- 描述: 列出某用户所有进程打开的文件

### 已删除未释放文件

```bash
lsof | grep deleted
```
- 描述: 找出「已删除但仍被进程占用」的文件，磁盘空间不释放的元凶

## 示例

### 端口占用排查

- 描述: 启动报「Address already in use」时的标准动作

```bash
# 查 8090 端口被谁占用
lsof -i:8090
# 输出: java  12345  root  42u  IPv6 ...  TCP *:8090 (LISTEN)

# 杀掉占用进程
kill $(lsof -t -i:8090)

# -t 只输出 PID，方便脚本化
```

### 磁盘空间不释放

- 描述: rm 了大文件但 df 不变的原因与解法

```bash
# 找到已删除但被进程持有的文件
lsof | grep deleted | sort -k7 -rn

# 解决：重启对应进程，或清空文件内容而非删除
echo "" > /var/log/huge.log
# systemctl restart app
```

### 进程行为分析

- 描述: 看进程打开的文件与连接

```bash
# java 进程打开了哪些日志与网络连接
lsof -p $(pgrep -f "wxyd" | head -1) | grep -E "log|TCP"

# 看某目录正被哪些进程写入
lsof +D /opt/app/logs/
```