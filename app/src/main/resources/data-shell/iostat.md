---
name: Iostat 磁盘 IO
category: 系统运维
order: 10
---

## 介绍

iostat 报告 CPU 统计与磁盘 IO 统计。当 top 里 %wa（等待 IO）偏高、应用响应变慢但 CPU/内存正常时，就该看磁盘了：util 饱和、await 变长、读写量异常都能在这找到答案。iotop 则从进程维度定位谁在狂写盘。

核心特点：
- **-x 扩展指标**：util 利用率、await 单次 IO 平均耗时是两个核心指标
- **%wa 是入口**：top 里 wa 高 → iostat 确认哪块盘饱和 → iotop 找进程
- **-k/-m 单位**：KB/s、MB/s 可读输出
- **间隔采样**：iostat -x 2 5 每 2 秒采一次共 5 次，观察趋势

## 语法

### 基础采样

```bash
iostat -x 2 5
```
- 描述: 每 2 秒采一次共 5 次；第一组是开机以来的均值，看后面几组

### 核心指标解读

```bash
iostat -x -m 1 3
```
- 描述: -m 用 MB；重点看 %util（接近 100% 即饱和）、await（毫秒，>20ms 机械盘偏慢）

### 只看设备

```bash
iostat -x -d sda
```
- 描述: -d 只显示磁盘统计（去掉 CPU 部分），聚焦指定盘

### 吞吐与 IOPS

```bash
iostat -x 1
```
- 描述: r/s+w/s 是 IOPS，rkB/s+wkB/s 是吞吐，对照磁盘规格判断瓶颈类型

### 进程级 IO

```bash
iotop -o
```
- 描述: -o 只显示有 IO 的进程，实时看谁在写盘（需 root，yum install iotop）

### 按进程累计

```bash
pidstat -d 2 5
```
- 描述: 每 2 秒采各进程读写量（sysstat 包），iotop 的非交互替代

## 示例

### 「系统变慢」排查链

- 描述: 从 top 到 iostat 到 iotop 的定位路径

```bash
# 1. top 看 CPU 构成，发现 wa 高
top
# %Cpu(s): 10.0 us, 5.0 sy, 45.0 wa   ← wa 高，IO 等待严重

# 2. iostat 确认哪块盘饱和
iostat -x -m 2 3
# sda: %util=99  await=85ms  ← sda 打满且单次 IO 耗时高

# 3. iotop 找出元凶进程
iotop -o
# 发现 mysqld 写入 80MB/s → 结合业务判断是慢查询/备份/日志刷写
```

### 日志狂写盘问题

- 描述: 定位异常刷盘的应用

```bash
# 谁在写
iotop -o | grep -v idle

# 写的是什么（配合 lsof 看文件句柄）
lsof -p $(pgrep -f wxyd) | grep log
du -h --max-depth=1 /opt/app/logs/ | sort -rh

# 处理：调日志级别 / 加轮转 / 迁移到独立盘
```

### 建立基线对比

- 描述: 心里有数才能发现异常

```bash
# 业务低峰期采样留存基线
iostat -x -m 2 10 > /tmp/io-baseline.txt

# 高峰期/故障时再采一组对比
iostat -x -m 2 10 > /tmp/io-now.txt
diff <(grep sda /tmp/io-baseline.txt) <(grep sda /tmp/io-now.txt)
```