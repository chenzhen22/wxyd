---
name: Ulimit 资源限制
category: 系统运维
order: 8
---

## 介绍

ulimit 限制用户/进程可占用的系统资源，其中文件句柄数（open files）最常出问题：Java 应用连接数、日志文件、网络套接字都占句柄，默认 1024 太小，报 Too many open files 时就得调它。调优要分清「当前会话临时生效」与「limits.conf 永久生效」两条路。

核心特点：
- **-n 文件句柄**：Too many open files 的主角，Java 应用建议 65536+
- **临时 vs 永久**：ulimit -n 只改当前 shell；limits.conf + pam 永久生效
- **systemd 服务要单独配**：systemd 启动的服务不受 limits.conf 管，要写 LimitNOFILE
- **查看进程实际值**：cat /proc/PID/limits 看到的才是进程真实生效的

## 语法

### 查看当前限制

```bash
ulimit -n
ulimit -a
```
- 描述: -n 看句柄数；-a 看全部限制（核心文件、栈大小、进程数等）

### 临时调整

```bash
ulimit -n 65536
```
- 描述: 只对当前 shell 及其子进程生效，重新登录失效；上限受 hard 限制约束

### 永久配置

```bash
# /etc/security/limits.conf
*  soft  nofile  65536
*  hard  nofile  131072
```
- 描述: soft 警告线/hard 硬上限；* 所有用户；重新登录生效

### systemd 服务配置

```bash
# .service 的 [Service] 段
LimitNOFILE=65536
LimitNPROC=65536
```
- 描述: systemctl 管的服务必须这样配，改完 daemon-reload + restart

### 查看进程实际句柄限制

```bash
cat /proc/12345/limits | grep "open files"
```
- 描述: 排查必看，确认配置真的作用到了目标进程

### 查看进程句柄占用

```bash
ls /proc/12345/fd | wc -l
lsof -p 12345 | wc -l
```
- 描述: 当前打开了多少句柄，逼近上限就是泄漏前兆

### 其他常用项

```bash
ulimit -c unlimited
ulimit -u 65535
```
- 描述: -c 核心转储大小（排查崩溃开）；-u 最大进程数

## 示例

### Too many open files 处理

- 描述: 报错时的标准处理流

```bash
# 1. 找到报错进程，看当前限制与占用
pid=$(pgrep -f wxyd.jar)
cat /proc/$pid/limits | grep "open files"
ls /proc/$pid/fd | wc -l

# 2. 看句柄都被什么占了（泄漏定位）
lsof -p $pid | awk '{print $4, $5}' | sort | uniq -c | sort -rn | head
# 大量 IPv4 socket → 连接泄漏；大量普通文件 → 文件流没关

# 3. 按服务类型永久修复（见下方两套路子）
```

### Java 应用三层配置

- 描述: 手动启动、systemd、全用户兜底

```bash
# 1. 全局兜底（重新登录生效）
cat >> /etc/security/limits.conf <<EOF
*  soft  nofile  65536
*  hard  nofile  131072
EOF

# 2. systemd 服务（覆盖 limits.conf，以这里为准）
# [Service] 段加 LimitNOFILE=65536

# 3. nohup 手动启动的场景：启动脚本里先提额
ulimit -n 65536
nohup java -jar app.jar &

# 4. 验证
cat /proc/$(pgrep -f wxyd.jar)/limits | grep "open files"
```

### 配置不生效排查

- 描述: 最常见的两个坑

```bash
# 坑一：改了 limits.conf 但没重新登录/重启服务 → 重新登录或 systemctl restart
# 坑二：pam 未加载限制模块 → 确认 sshd 配置
grep UsePAM /etc/ssh/sshd_config        # 应为 yes
grep pam_limits /etc/pam.d/sshd         # 应存在 session required pam_limits.so

# 坑三：只改了 soft 没改 hard，或写成了 6553 （位数手误）
ulimit -Hn    # hard 上限确认
```