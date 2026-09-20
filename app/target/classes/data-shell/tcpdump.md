---
name: Tcpdump 抓包
category: 网络工具
order: 7
---

## 介绍

tcpdump 是命令行抓包工具，直接捕获网卡上的数据包。服务「看着活着但连不通」、请求发出没响应、TLS 握手失败这类应用层查不出的问题，抓包看原始流量是最后的定位手段。抓出的 pcap 文件还能用 Wireshark 图形化分析。

核心特点：
- **按网卡抓**：-i 指定接口，any 抓所有
- **BPF 过滤表达式**：host/port/src/dst/and/or/not 精确圈流量
- **-w 存盘分析**：抓成 pcap 丢给 Wireshark 看协议细节
- **-X 看内容**：十六进制+ASCII 展示，HTTP 明文请求肉眼可读

## 语法

### 基本抓包

```bash
tcpdump -i any
```
- 描述: 抓所有网卡流量，Ctrl+C 停止；先用 any 摸底再缩小范围

### 按主机过滤

```bash
tcpdump -i any host 192.168.1.100
```
- 描述: 只抓与该 IP 有关的包；src/dst 限定方向

### 按端口过滤

```bash
tcpdump -i any port 8080
```
- 描述: 只抓 8080 端口流量；组合 and：port 8080 and host 10.0.0.5

### 存为 pcap 文件

```bash
tcpdump -i any -w dump.pcap port 3306
```
- 描述: -w 写入文件，用 Wireshark 打开做深度分析

### 限制包数与大小

```bash
tcpdump -i any -c 100 -s 0 -w dump.pcap
```
- 描述: -c 抓满 100 个包自动停；-s 0 完整抓包不截断

### 查看内容

```bash
tcpdump -i any -X -s 0 port 80 and host 10.0.0.5
```
- 描述: -X 十六进制+ASCII 显示，明文 HTTP 能看到请求行和 Header

### 只看 TCP 握手

```bash
tcpdump -i any "tcp[tcpflags] & (tcp-syn|tcp-fin) != 0" port 8090
```
- 描述: 只抓 SYN/FIN，看连接建立与断开，排查连接风暴

## 示例

### 连接问题定位

- 描述: 「telnet 不通」时抓包看谁没回话

```bash
# 服务端上抓，同时客户端 telnet 192.168.1.100 8090
tcpdump -i any -n port 8090 and host 192.168.1.50

# 判断依据：
# 只有 SYN 没有 SYN-ACK  → 服务端没监听或防火墙拦截
# 有 SYN-ACK 但没 ACK    → 网络回包被丢
# 三次握手完整但很快 RST → 应用层主动拒绝
```

### 抓 MySQL 慢交互

- 描述: 确认 DB 请求是否发出、耗时在哪

```bash
# 抓 3306 与特定应用机的交互，存盘分析
tcpdump -i any -s 0 -w mysql.pcap host 10.0.0.20 and port 3306 -c 1000

# 用 Wireshark 打开 mysql.pcap，按协议过滤看每个 SQL 的往返耗时
# 或实时看时间戳观察间隔
tcpdump -i any -tttt -n port 3306 and host 10.0.0.20
```

### 抓 HTTP 明文请求

- 描述: 内网 HTTP 服务调试

```bash
# 抓本机 8090 的 HTTP 请求内容
tcpdump -i any -A -s 0 -n port 8090 and tcp port 8090

# -A 以 ASCII 显示，可直接看到 GET/POST 路径与 Header
# HTTPS 流量是密文，只能看到握手，需在网关侧或用 sslkeylogfile 方案
```