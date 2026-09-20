---
name: Watch 周期观察
category: 系统运维
order: 11
---

## 介绍

watch 让任意命令每隔 N 秒自动重跑并全屏刷新结果，把「一次输出」变成「实时仪表盘」。盯连接数变化、盯日志增长、盯目录大小，不用反复敲回车，Ctrl+C 退出。

核心特点：
- **-n 间隔**：-n 1 每秒刷新（最小 0.1 秒）
- **-d 高亮差异**：两次输出有变化的部分高亮显示，一眼看到动了什么
- **-g 变化即退**：输出有变化时退出并返回 0，可做触发器
- **引号陷阱**：命令带管道时整体要加引号，否则管道只作用于 watch

## 语法

### 基本用法

```bash
watch -n 1 date
```
- 描述: 每秒刷新显示时间，最简示例

### 高亮变化

```bash
watch -d -n 1 'netstat -an | grep ESTABLISHED | wc -l'
```
- 描述: -d 高亮数值变化，盯连接数涨跌

### 间隔精确

```bash
watch -n 0.5 'ls -lh /opt/app/logs/app.log'
```
- 描述: 0.5 秒间隔，观察文件增长速率

### 变化即退出

```bash
watch -g 'ls /tmp/flag.done 2>/dev/null'
```
- 描述: -g 检测到输出变化（如文件出现）就退出，配合 echo $? 做等待触发

### 标题定制

```bash
watch -n 2 -t -d 'df -h /data'
```
- 描述: -t 去标题栏；顶部默认显示命令与时间间隔

## 示例

### 实时仪表盘组合

- 描述: 高频盯盘场景

```bash
# 盯端口连接数变化
watch -d -n 1 'ss -tn | grep :8090 | wc -l'

# 盯日志文件增长速度
watch -n 2 'ls -lh /opt/app/logs/app.log | awk "{print \$5}"'

# 盯磁盘剩余
watch -d -n 5 'df -h /data | tail -1'

# 盯某个服务状态
watch -n 3 'systemctl status wxyd --no-pager | head -5'
```

### 等待某个条件发生

- 描述: -g 做轮询触发器

```bash
# 等待文件生成，出现后立即执行后续动作
watch -g 'ls /data/incoming/*.csv 2>/dev/null' && echo "文件已到达" && ./import.sh

# 等服务端口起来再继续
watch -g 'ss -tln | grep :8090' && echo "服务已监听"
```

### 与 tail -f 的取舍

- 描述: 两种「实时」的差异

```bash
# 追加型内容（日志）→ tail -F 更自然，历史上下文保留
tail -F app.log

# 数值/状态型快照（连接数、磁盘、进程）→ watch 更直观
watch -d -n 1 'ss -s | head -3'
```