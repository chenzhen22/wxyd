---
name: Free 内存查看
category: 系统运维
order: 5
---

## 介绍

free 显示物理内存与交换分区的使用情况。判断内存是否紧张要看 available 而不是 free，这是新手最常见的误区；buffer/cache 是内核缓存可随时回收，不算「被占用」。

核心特点：
- **-h 可读单位**：G/M 自动换算，看数不累
- **available 才是真可用**：free 列小不代表内存紧张
- **buff/cache 可回收**：被缓存占用的内存在需要时会自动让出
- **swap 使用需警惕**：swap 持续增长说明物理内存真不够了

## 语法

### 基本查看

```bash
free -h
```
- 描述: -h 人类可读；默认单位 KB（-m 兆、-g G）

### 周期刷新

```bash
free -h -s 3 -c 5
```
- 描述: -s 每 3 秒刷新、-c 共刷 5 次，观察内存变化趋势

### 看 swap 明细

```bash
swapon --show
cat /proc/swaps
```
- 描述: 列出交换分区/交换文件及其使用量

### 进程级内存

```bash
ps aux --sort=-%mem | head -5
smem -rk | head
```
- 描述: 找吃内存的进程；RSS 为实际驻留内存

## 示例

### 读懂输出

- 描述: 一行一行解释 free -h 结果

```bash
#               total   used   free   shared  buff/cache  available
# Mem:           15Gi   8.2Gi   1.1Gi    209Mi       6.1Gi      6.8Gi
# Swap:          2.0Gi      0B  2.0Gi

# 内存是否紧张看 available（6.8Gi），不是 free（1.1Gi）
# buff/cache 6.1Gi 是磁盘缓存，需要时会自动释放
# Swap used 持续增长且不回落 = 物理内存确实不足
```

### 内存排查实战

- 描述: 内存高时的定位流程

```bash
# 1. 确认真的紧张（available 低 + swap 在涨）
free -h; sleep 30; free -h

# 2. 找出内存大户
ps aux --sort=-%mem | head -10

# 3. java 进程可进一步看 JVM 各区占用
ps -o pid,rss,vsz,cmd -p $(pgrep -f wxyd)

# 4. 释放页缓存（应急手段，先 sync）
sync && echo 3 > /proc/sys/vm/drop_caches
```

### 采集与告警

- 描述: 脚本化内存巡检

```bash
# available 低于 1G 输出告警（可接通知）
free -m | awk 'NR==2 && $7 < 1024 {print "内存告警: 可用", $7, "MB"}'

# 每 5 分钟记录内存趋势（配合 crontab）
*/5 * * * * (date; free -h | head -2) >> /var/log/mem.log
```