---
name: NTP 时间同步
category: 系统运维
order: 6
---

## 介绍

多台服务器时间不一致会引发_token 校验失败、日志时序错乱、数据库主从不一致等诡异问题。NTP 负责把机器时钟对齐到统一源，CentOS 7 用 chrony（推荐）或 ntpd，一次性配好长期受益。

核心特点：
- **chrony 为主流**：CentOS 7+/Rocky 默认自带，比老 ntpd 收敛更快
- **时钟分层**：Stratum 层级，内网自建 NTP 源是最佳实践
- **渐变修正**：大偏差默认平滑追赶（slew），硬跳用 makestep
- **先查后改**：timedatectl 看状态与时区，再谈同步

## 语法

### 查看时间状态

```bash
timedatectl
```
- 描述: 看当前时间、时区、NTP 是否开启；Local time 与 UTC 的关系一目了然

### 设置时区

```bash
timedatectl set-timezone Asia/Shanghai
```
- 描述: 时区不对会差 8 小时，先对时区再对时间

### chrony 服务管理

```bash
systemctl status chronyd
systemctl enable --now chronyd
```
- 描述: 确认服务在跑且开机自启；--now 启动并设自启

### 配置时间源

```bash
# /etc/chrony.conf
server ntp.aliyun.com iburst
server 10.0.0.1 iburst
```
- 描述: 多源冗余，内网源优先；iburst 加快首次同步；改完 systemctl restart chronyd

### 查看同步状态

```bash
chronyc sources -v
chronyc tracking
```
- 描述: sources 看各源状态（^* 为当前使用源）；tracking 看本机偏差

### 手动强制对时

```bash
chronyc makestep
```
- 描述: 立即跳变对齐（不做平滑追赶），偏差很大时用一次

### 旧版 ntpdate

```bash
ntpdate ntp.aliyun.com
```
- 描述: 一次性硬对时（会使时间跳变）；业务运行中慎用，仅初始化时用

## 示例

### 新机器初始化对时

- 描述: 三步完成时区与时间同步

```bash
# 1. 时区设为上海
timedatectl set-timezone Asia/Shanghai

# 2. 配置 NTP 源（阿里云公网源 + 内网源）
sed -i 's/^server/#server/' /etc/chrony.conf
echo -e "server ntp.aliyun.com iburst\nserver 10.0.0.1 iburst" >> /etc/chrony.conf
systemctl enable --now chronyd

# 3. 验证
chronyc sources -v | head -8
timedatectl | grep -E "Time zone|synchronized"
```

### 内网自建 NTP 服务端

- 描述: 不能出公网的环境，拿一台当源

```bash
# 服务端（能出公网或有时钟源）：/etc/chrony.conf 加
# allow 192.168.1.0/24          # 允许内网同步
# local stratum 10              # 断外网时兜底
systemctl restart chronyd

# 客户端全部指向它
# server 192.168.1.1 iburst
```

### 时间不一致引发问题的排查

- 描述: 典型症状与处理

```bash
# 症状：签名校验失败/日志乱序 → 先比时区再比时间
for h in prod1 prod2 prod3; do
    echo "$h: $(ssh $h 'date "+%F %T %Z"')"
done

# 偏差几分钟内：chronyc makestep 立即对齐
# 持续漂移：确认 chronyd 在跑、源可达（firewall 放行 123/udp）
```