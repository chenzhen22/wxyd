---
name: Firewalld 防火墙
category: 网络工具
order: 8
---

## 介绍

firewalld 是 CentOS 7+ 默认防火墙管理工具，基于 zone 管理规则。新装的 Redis/MySQL 远程连不上、服务监听正常但外部访问不了，十有八九是防火墙没放行。掌握端口开放与规则查询即可解决绝大多数场景。

核心特点：
- **zone 分区**：public 是默认区，日常只在 public 里加规则
- **运行时与永久**：不加 --permanent 只对本次生效，加了要 --reload 才生效
- **服务 vs 端口**：可按服务名（ssh/http）或端口号放行
- **排查顺序**：先查监听、再查防火墙、最后查云安全组

## 语法

### 服务状态

```bash
systemctl status firewalld
firewall-cmd --state
```
- 描述: 确认防火墙是否在运行；测试环境可临时停：systemctl stop firewalld

### 查看已放行端口

```bash
firewall-cmd --list-ports
firewall-cmd --list-all
```
- 描述: --list-ports 看端口；--list-all 看当前区全部规则（服务+端口）

### 开放端口

```bash
firewall-cmd --permanent --add-port=8090/tcp
firewall-cmd --reload
```
- 描述: --permanent 永久生效，必须跟 --reload 才加载；不加 --permanent 立即生效但重启丢失

### 开放端口范围

```bash
firewall-cmd --permanent --add-port=8000-8100/tcp
```
- 描述: 按范围开放，微服务端口段常用

### 移除端口

```bash
firewall-cmd --permanent --remove-port=8090/tcp
firewall-cmd --reload
```
- 描述: 下线服务时收口；--list-ports 确认

### 放行来源 IP

```bash
firewall-cmd --permanent --add-rich-rule='rule family="ipv4" source address="10.0.0.0/24" port port="3306" protocol="tcp" accept'
```
- 描述: rich rule 限定只有内网网段能访问 3306，比全网开放安全

### 旧版 iptables

```bash
iptables -L -n
iptables -I INPUT -p tcp --dport 8090 -j ACCEPT
```
- 描述: CentOS 6 或未用 firewalld 的机器用 iptables，-I 插入规则立即生效

## 示例

### 新服务上线放行

- 描述: 部署完成后三步放行

```bash
# 1. 确认服务在监听（先排除应用问题）
ss -tlnp | grep 8090

# 2. 防火墙放行
firewall-cmd --permanent --add-port=8090/tcp
firewall-cmd --reload

# 3. 验证
firewall-cmd --list-ports
# 外部 telnet 端口验证
```

### MySQL/Redis 只对内网开放

- 描述: 数据库端口绝不对全网开放

```bash
# 只允许应用网段访问 3306
firewall-cmd --permanent --add-rich-rule='rule family="ipv4" source address="192.168.1.0/24" port port="3306" protocol="tcp" accept'
firewall-cmd --reload

# 云服务器还要检查控制台的安全组规则（另一层防火墙）
```

### 排查「连不上」

- 描述: 标准排查链路

```bash
# 1. 服务端确认监听
ss -tlnp | grep 8090

# 2. 确认防火墙
firewall-cmd --list-ports | grep 8090

# 3. 确认 SELinux（centos 偶发）
getenforce

# 4. 云机确认安全组；用另一台机器 nc -zv 验证
nc -zv 192.168.1.100 8090
```