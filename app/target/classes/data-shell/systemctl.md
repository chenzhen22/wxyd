---
name: Systemctl 服务管理
category: 系统运维
order: 7
---

## 介绍

systemctl 是 systemd 的统一服务管理入口：启动停止、开机自启、状态查看、日志查询（journalctl）一站式完成。Java 应用上线写一个 service 单元文件，就能获得开机自启、崩溃自动拉起、标准日志收集等能力，替代手写 nohup 脚本。

核心特点：
- **动作四件套**：start/stop/restart/status，reload 平滑重载配置
- **自启管理**：enable/disable 开机自启，--now 边设边启
- **单元文件**：/etc/systemd/system/*.service 定义服务
- **journalctl 日志**：-u 按服务过滤，-f 实时追，等价 tail -f

## 语法

### 服务操作

```bash
systemctl start wxyd
systemctl stop wxyd
systemctl restart wxyd
systemctl status wxyd
```
- 描述: .service 后缀可省略；status 看 Active 状态与最近日志

### 开机自启

```bash
systemctl enable wxyd
systemctl disable wxyd
systemctl enable --now wxyd
```
- 描述: enable 设自启；--now 设自启同时立即启动，新服务上线一步到位

### 平滑重载

```bash
systemctl reload wxyd
```
- 描述: 仅当单元文件配置了 Reload 信号时有效；改配置优先 reload 避免断连

### 修改配置后刷新

```bash
systemctl daemon-reload
```
- 描述: 改过 .service 文件后必须执行，否则改动不生效

### 查看服务列表

```bash
systemctl list-units --type=service --state=running
systemctl list-unit-files | grep enabled
```
- 描述: 前者看运行中的服务，后者看设置了自启的服务

### journalctl 查日志

```bash
journalctl -u wxyd -f
journalctl -u wxyd --since "10 min ago"
journalctl -u wxyd --since today --no-pager | tail -100
```
- 描述: -f 实时追；--since 时间过滤；--no-pager 输出不进分页器

### 查看服务崩溃原因

```bash
systemctl status wxyd -l
journalctl -u wxyd -p err -n 50
```
- 描述: -l 不截断；-p err 只看错误级别；OOM/端口冲突通常都能看到

## 示例

### Java 应用注册为系统服务

- 描述: 一次配置，自启+拉起+日志全有

```bash
# 创建 /etc/systemd/system/wxyd.service
cat > /etc/systemd/system/wxyd.service <<EOF
[Unit]
Description=Wxyd Application
After=network.target mysql.service

[Service]
Type=simple
User=appuser
WorkingDirectory=/opt/app
ExecStart=/usr/bin/java -jar /opt/app/wxyd.jar
Restart=on-failure
RestartSec=10
LimitNOFILE=65536

[Install]
WantedBy=multi-user.target
EOF

# 生效并启动
systemctl daemon-reload
systemctl enable --now wxyd
systemctl status wxyd
```

### 服务起不来的排查

- 描述: status → journalctl 两板斧

```bash
# 1. 看状态里的失败原因摘要
systemctl status wxyd -l

# 2. 看完整日志定位
journalctl -u wxyd -n 100 --no-pager

# 常见原因：端口被占（ss -tlnp 查）、JDK 路径不对（绝对路径！）、
# 权限不足（User= 指定的用户对目录无写权限）、内存不足被 OOM
```

### 改配置与重启的正确姿势

- 描述: 改动生效的完整链路

```bash
# 改 JVM 参数后
vim /etc/systemd/system/wxyd.service
systemctl daemon-reload
systemctl restart wxyd

# 确认新参数生效
ps -ef | grep wxyd | grep -o '\-Xmx[0-9]*[gGmM]'
```