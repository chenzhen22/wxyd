---
name: Netstat 网络连接
category: 网络工具
order: 3
---

## 介绍

netstat（network statistics）用于查看网络连接、路由表、接口统计等网络状态信息。排查端口占用、确认服务监听、分析连接状态是它的三大典型场景。较新的系统推荐 ss 命令（更快），但 netstat 仍是使用最广泛的通用工具。

核心特点：
- **查看监听**：-l 列出本机正在监听的端口
- **查看连接**：-a 列出所有连接（含监听、已建立、TIME_WAIT 等）
- **进程关联**：-p 显示占用端口的进程号与进程名（需 root）
- **数字显示**：-n 不做主机名/端口名反解，输出纯 IP 和端口号

## 语法

### 查看所有连接

```bash
netstat -a
```
- 描述: 列出所有套接字，包括监听中和已建立的连接

### 查看监听端口

```bash
netstat -l
```
- 描述: 只显示处于 LISTEN 状态的监听端口

### 数字格式输出

```bash
netstat -an
```
- 描述: -n 直接显示 IP 和端口号，跳过 DNS 反解，速度最快

### 显示进程信息

```bash
netstat -tlnp
```
- 描述: -p 显示 PID/进程名，需 root 权限，排查端口占用必备

### 只看 TCP 连接

```bash
netstat -tn
```
- 描述: -t 只显示 TCP（-u 为 UDP），-n 数字格式

### 查看路由表

```bash
netstat -rn
```
- 描述: -r 显示内核路由表，-n 数字格式，看默认网关

### 统计连接状态

```bash
netstat -an | awk '/^tcp/ {print $6}' | sort | uniq -c
```
- 描述: 按状态汇总连接数，快速发现大量 TIME_WAIT 等异常

## 示例

### 排查端口占用

- 描述: 确认某端口被哪个进程占用

```bash
# 查 8080 端口占用（最常用组合）
netstat -tlnp | grep 8080
# 输出: tcp 0 0 0.0.0.0:8080 0.0.0.0:* LISTEN 12345/java

# 确认 8090 是否已在监听
netstat -an | grep 8090 | grep LISTEN

# 查所有 java 进程监听的端口
netstat -tlnp | grep java
```

### 确认服务监听范围

- 描述: 区分监听所有网卡还是仅本机

```bash
# 监听 0.0.0.0:8080 表示所有网卡可访问
# 监听 127.0.0.1:8080 表示仅本机可访问
netstat -tlnp | grep -E "0.0.0.0|:::"

# 只看 IPv4 的 TCP 监听
netstat -tln4
```

### 连接状态分析

- 描述: 统计各状态连接数，判断负载与健康度

```bash
# 汇总 TCP 各状态连接数
netstat -an | awk '/^tcp/ {print $6}' | sort | uniq -c
# 典型输出:
#    120 ESTABLISHED
#     85 TIME_WAIT
#      8 LISTEN

# 只看已建立的连接
netstat -an | grep ESTABLISHED

# 统计每个客户端 IP 的连接数（防刷排查）
netstat -an | grep ESTABLISHED | awk '{print $5}' | cut -d: -f1 | sort | uniq -c | sort -rn | head
```

### 路由与网卡信息

- 描述: 查看默认网关和接口流量

```bash
# 查看路由表，找默认网关
netstat -rn | head -10

# 查看各网卡收发数据包统计（errors/dropped 排查）
netstat -i
```