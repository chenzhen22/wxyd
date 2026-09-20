---
name: Df 与 Du 磁盘空间
category: 系统运维
order: 3
---

## 介绍

df 看文件系统整体使用率，du 看目录实际占用，两者配合完成磁盘空间的「总览 → 定位 → 清理」三步曲。磁盘写满是线上最常见的故障之一，这套组合拳必须熟练。

核心特点：
- **df 总览**：-h 人类可读单位，关注 Use% 超 85% 预警
- **du 定位**：-sh 汇总、--max-depth 分层，逐层下钻找大目录
- **inode 也会满**：小文件海量时 df 正常但写不进，df -i 排查
- **删除不等于释放**：进程占用句柄时需配合 lsof 处理

## 语法

### 查看磁盘使用率

```bash
df -h
```
- 描述: -h 自动换算 G/M，关注 Use% 列与挂载点

### 查看 inode

```bash
df -i
```
- 描述: 小文件爆 inode 时 Use% 不高但无法写入，用它确认

### 目录总占用

```bash
du -sh /opt/app
```
- 描述: -s 只给总数、-h 可读单位，快速看某目录多大

### 分层下钻

```bash
du -h --max-depth=1 /opt | sort -rh
```
- 描述: 只看第一层子目录大小并降序，逐层下钻找大目录

### 找大文件

```bash
du -ah /opt | sort -rh | head -20
```
- 描述: -a 含文件级，配合 sort 取最大的 20 个

### 排除挂载点

```bash
du -shx /opt/*
```
- 描述: -x 不跨文件系统，避免把 NFS 挂载也算进来

## 示例

### 空间排查三步曲

- 描述: 从告警到定位的完整流程

```bash
# 1. df -h 找到满的挂载点，比如 /data 用了 95%
df -h

# 2. du 逐层下钻定位大目录
du -h --max-depth=1 /data | sort -rh
du -h --max-depth=1 /data/app/logs | sort -rh

# 3. 圈定大文件后归档或删除
find /data/app/logs -name "*.log" -size +100M -mtime +7 -exec gzip {} \;
```

### 常见陷阱处理

- 描述: df 与 du 数字对不上的两种情况

```bash
# 情况一：文件已删除但进程还持有 → lsof 找元凶
lsof | grep deleted | sort -k7 -rn | head

# 情况二：inode 耗尽 → 找海量小文件目录
df -i
find /tmp -type f | wc -l

# 挂载点被覆盖：先 umount 再看被盖住的旧数据
umount /mnt/backup && du -sh /mnt/backup
```

### 定时容量巡检

- 描述: 脚本化监控，超阈值告警

```bash
# 磁盘超 85% 就输出告警行（可接钉钉/邮件通知）
df -h | awk -v OFS=" " '$5+0 > 85 {print "磁盘告警:", $6, "已用", $5}'
```