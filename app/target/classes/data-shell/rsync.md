---
name: Rsync 增量同步
category: 文件传输
order: 2
---

## 介绍

rsync 是增量文件同步工具，只传输有变化的部分，比 scp 全量拷贝快得多。本地备份、异地镜像、代码发布、断点续传都优先用它；--delete 参数还能让目标目录与源完全一致（危险但强大）。

核心特点：
- **增量算法**：对比差异只传变化块，大文件小改动传输量极小
- **保留属性**：-a 归档模式保留权限/时间/软链等
- **--delete 镜像**：删除目标端多余文件，实现严格同步
- **支持断点续传与压缩**：-z 压缩传输，中断重跑自动续

## 语法

### 本地同步

```bash
rsync -av /src/dir/ /dst/dir/
```
- 描述: -a 归档（递归+属性）、-v 显示过程；注意尾斜杠：/src/ 同步内容，/src 同步目录本身

### 同步到远程

```bash
rsync -avz /opt/app/ root@192.168.1.100:/opt/app/
```
- 描述: -z 压缩传输，走 ssh 通道（默认），免密后全自动

### 远程拉取

```bash
rsync -avz root@host:/var/log/app/ ./applog/
```
- 描述: 源在远程即拉取方向，方向由源/目的位置决定

### 镜像同步

```bash
rsync -avz --delete /src/ root@host:/dst/
```
- 描述: --delete 删除目标端源里已不存在的文件，严格镜像；先 --dry-run 预览

### 排除文件

```bash
rsync -av --exclude="*.log" --exclude="target/" /src/ /dst/
```
- 描述: 排除日志与构建产物；多个 --exclude 可叠加，或用 --exclude-from=文件

### 限速与指定端口

```bash
rsync -avz --bwlimit=5000 -e "ssh -p 2222" /src/ user@host:/dst/
```
- 描述: --bwlimit 限速 KB/s；-e 指定非标准 ssh 端口

### 列差异不传输

```bash
rsync -avnc /src/ /dst/
```
- 描述: -n 干跑预览、-c 校验内容，先看会发生什么再真跑

## 示例

### 应用发布

- 描述: 增量发布比 scp 全量快一个量级

```bash
# 增量发布新版本（只传变化的文件）
rsync -avz --exclude="logs/" /opt/app/build/ root@192.168.1.100:/opt/app/

# 发布前预演，确认文件清单
rsync -avnc --exclude="logs/" /opt/app/build/ root@192.168.1.100:/opt/app/ | head -20
```

### 定时备份

- 描述: crontab + rsync + 日期目录

```bash
# 每晚 1 点增量备份到备份机，保留按天的目录
# 0 1 * * * rsync -az --delete /opt/app/ backup@bak:/backup/app/$(date +\%F)/

# 备份机端清理 7 天前的备份目录
# 30 2 * * * ssh backup@bak "find /backup/app -maxdepth 1 -mtime +7 -exec rm -rf {} \;"
```

### 日志归集

- 描述: 多台服务器日志集中到一台

```bash
# 各业务机把日志推到日志中心
rsync -az /opt/app/logs/ logcenter:/data/logs/$(hostname)/

# 日志中心统一 --delete 镜像，保证与源一致
rsync -az --delete /opt/app/logs/ logcenter:/data/logs/$(hostname)/
```