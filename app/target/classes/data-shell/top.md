---
name: Top 性能监控
category: 系统运维
order: 2
---

## 介绍

top 实时展示 CPU、内存、负载与进程资源占用，是性能排查的第一入口。看懂 load average 三数值、区分 us/sy/wa 的 CPU 构成、按 P/M 排序找资源大户，就能快速定位性能瓶颈。

核心特点：
- **实时刷新**：默认 3 秒刷新，动态查看进程资源
- **交互排序**：大写 P 按 CPU 排、M 按内存排、1 展开每核
- **负载三数值**：load average 的 1/5/15 分钟均值，经验阈值 ≈ CPU 核数
- **CPU 构成**：us 用户态、sy 内核态、wa 等待 IO，wa 高即磁盘瓶颈

## 语法

### 启动与退出

```bash
top
```
- 描述: q 退出；常用交互键：P/M/1/k（杀进程）

### 批处理模式

```bash
top -b -n 1 > top-snapshot.txt
```
- 描述: -b 批处理输出、-n 1 只刷新一次，可存档或进脚本

### 指定刷新间隔

```bash
top -d 1
```
- 描述: -d 1 每 1 秒刷新，观察瞬时变化

### 监控指定进程

```bash
top -p $(pgrep -d',' -f "java")
```
- 描述: -p 指定 PID 列表，只看目标进程的资源变化

### htop 增强版

```bash
htop
```
- 描述: 彩色界面、鼠标操作、树状视图（需安装），体验优于 top

## 示例

### 读懂数值

- 描述: 顶部各区与进程列表的关键指标

```bash
# load average: 4.20, 3.80, 3.10
# 8 核机器阈值参考 8，持续超过说明 CPU 饱和

# %Cpu(s): 45.0 us, 10.0 sy, 0.0 ni, 40.0 id, 5.0 wa
# us 高=应用吃 CPU；sy 高=系统调用频繁；wa 高=磁盘 IO 瓶颈

# KiB Mem: 16384 total, 2048 free, 8192 used, 6144 buff/cache
# 可用内存看 available 列，不是 free 列（cache 可回收）
```

### 定位资源大户

- 描述: 快速找到吃 CPU / 吃内存的进程

```bash
# top 内按大写 P：按 CPU 排序，第一行即 CPU 大户
# top 内按大写 M：按内存排序，第一行即内存大户

# 非交互方式取 CPU 前 5
top -b -n 1 | head -15 | tail -8

# 只看 java 进程的实时资源
top -p $(pgrep -d',' -f "wxyd")
```

### 配合脚本采集

- 描述: 定时抓取性能快照留档

```bash
# 每分钟记录一次负载与内存到文件（配合 crontab）
* * * * * (uptime; free -h) >> /var/log/perf-$(date +\%Y\%m\%d).log
```