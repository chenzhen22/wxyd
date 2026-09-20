---
name: Ps 进程管理
category: 系统运维
order: 4
---

## 介绍

ps 报告当前进程快照，配合 kill 完成进程的查看与管理。找进程、看资源、发信号、管前后台，是进程故障处理（假死、僵尸、需要重启）的基本功。

核心特点：
- **两种风格**：BSD 风格（ps aux）与 System V 风格（ps -ef），都用熟
- **grep 过滤黄金组合**：ps aux | grep xxx，记得排除 grep 自身
- **信号分级**：SIGTERM(15) 优雅退出优先，SIGKILL(9) 强杀是最后手段
- **前后台控制**：& 放后台、jobs 查看、fg/bg 切换、nohup 免挂断

## 语法

### aux 风格

```bash
ps aux | grep java
```
- 描述: a 所有用户、u 详细格式、x 含无终端进程；%CPU/%MEM 看占用

### -ef 风格

```bash
ps -ef | grep wxyd
```
- 描述: e 所有进程、f 全格式（含 PPID 父进程号）

### 按资源排序

```bash
ps aux --sort=-%cpu | head -5
ps aux --sort=-%mem | head -5
```
- 描述: --sort=-%cpu 按 CPU 降序，=%mem 同理内存，head 取前几名

### 自定义输出列

```bash
ps -eo pid,ppid,user,%mem,%cpu,cmd --sort=-%mem | head
```
- 描述: -e 全部进程、-o 自定义列，精确取所需字段

### 树状展示

```bash
pstree -p | grep java
ps -ef --forest | grep -A 3 tomcat
```
- 描述: pstree 直观看父子关系，-p 显示 PID

### 发送信号

```bash
kill -15 PID
kill -9 PID
killall java
pkill -f "wxyd"
```
- 描述: 默认即 15 优雅终止；-9 强杀不清理资源；pkill 按名/命令行匹配

## 示例

### 日常进程查找

- 描述: 找到目标进程的多种姿势

```bash
# 找 java 应用进程（排除 grep 自身）
ps aux | grep "[w]xyd"

# 精确取 PID（供脚本使用）
pgrep -f "wxyd.jar"

# 看某进程的启动命令全貌
ps -p 12345 -o cmd=
```

### 进程状态处理

- 描述: 假死重启与僵尸确认

```bash
# 优雅重启应用
pid=$(pgrep -f "wxyd.jar")
kill -15 $pid
sleep 5
pgrep -f "wxyd.jar" || echo "已停止，准备启动"

# 15 无效再 9 强杀（先确认没有内存落盘需求）
kill -9 $pid

# 确认有无僵尸进程（Z 状态）
ps aux | awk '$8 ~ /^Z/'
```

### 前后台与免挂断

- 描述: 交互任务的作业控制

```bash
# 后台运行并免挂断（关终端不影响）
nohup java -jar app.jar > app.out 2>&1 &

# 查看后台任务
jobs -l

# 切回前台 / 继续后台
fg %1
bg %1
```