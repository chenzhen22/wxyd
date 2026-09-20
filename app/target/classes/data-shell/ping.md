---
name: Ping 网络诊断
category: 网络工具
order: 5
---

## 介绍

网络故障排查遵循「由近及远」：先 ping 本机与网关，再 ping 公网，分段定位断点。ping 测连通与延迟，telnet/nc 测端口通断，traceroute 看路径，nslookup/dig 查 DNS。四件套组合解决 90% 的「连不上」问题。

核心特点：
- **分层排查**：网卡 → 网关 → 公网 → 目标端口，逐段验证
- **ping 测连通**：ICMP 回包看延迟与丢包
- **telnet/nc 测端口**：ping 通但服务不通时的下一步
- **traceroute 看路径**：定位在哪一跳断掉

## 语法

### 基本连通测试

```bash
ping -c 4 192.168.1.100
```
- 描述: -c 指定次数（不加会一直 ping），看丢包率与延迟

### 指定间隔与超时

```bash
ping -i 0.5 -W 2 -c 10 8.8.8.8
```
- 描述: -i 间隔 0.5 秒、-W 等待超时 2 秒，快速探测

### 测端口连通

```bash
telnet 192.168.1.100 8080
```
- 描述: 测 TCP 端口是否可达；Ctrl+] 退出；nc -zv 是更现代的写法

### nc 测端口

```bash
nc -zv 192.168.1.100 8080
```
- 描述: -z 只扫描不发送数据、-v 显示结果，脚本判断退出码即可

### 路由跟踪

```bash
traceroute 8.8.8.8
```
- 描述: 显示到目标经过的每一跳，定位断点在第几跳

### DNS 查询

```bash
nslookup example.com
dig +short example.com
```
- 描述: 域名解析排查；dig +short 只输出 IP 结果更干净

## 示例

### 分段定位网络故障

- 描述: 由近及远的标准排查流程

```bash
# 1. 先 ping 网关，通则本机与内网正常
ping -c 3 192.168.1.1

# 2. 再 ping 公网 IP，通则出口正常（不经过 DNS）
ping -c 3 223.5.5.5

# 3. 再 ping 域名，不通则是 DNS 问题
ping -c 3 www.baidu.com
```

### 服务端口排查

- 描述: ping 得通但服务连不上时

```bash
# 测远程服务端口是否开放
nc -zv 192.168.1.100 8090
# 输出: Connection to 192.168.1.100 8090 port [tcp/*] succeeded!

# 脚本里用退出码判断
nc -z -w 2 192.168.1.100 8090 && echo "端口通" || echo "端口不通"

# telnet 手工验证 HTTP 服务
telnet 192.168.1.100 80
# 连上后输入 GET / HTTP/1.0 回车两次能看到响应
```

### DNS 问题排查

- 描述: 域名不通但 IP 通时查解析

```bash
# 查看域名解析出的 IP
dig +short api.example.com

# 指定 DNS 服务器查询对比
dig @223.5.5.5 +short api.example.com

# 配置文件里写死 IP 绕过 DNS 验证
echo "1.2.3.4 api.example.com" >> /etc/hosts
```