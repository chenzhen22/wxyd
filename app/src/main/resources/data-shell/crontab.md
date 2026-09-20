---
name: Crontab 定时任务
category: 系统运维
order: 1
---

## 介绍

crontab 是 Linux 计划任务服务，按 cron 表达式周期性执行命令或脚本。日志切割、数据备份、状态巡检等周期性工作都由它驱动，是无人值守运维的基石。

核心特点：
- **五段表达式**：分 时 日 月 周，精确到分钟
- **用户级隔离**：每个用户有自己的 crontab，环境变量独立
- **输出会发邮件**：不重定向的话输出可能塞爆 /var/spool/mail，务必重定向
- **注意环境差异**：cron 环境 PATH 精简，脚本内建议用绝对路径

## 语法

### 编辑定时任务

```bash
crontab -e
```
- 描述: 编辑当前用户的任务；-l 列出；-r 清空（危险）

### 五段表达式

```bash
分 时 日 月 周
```
- 描述: * 任意值、*/n 每 n 个单位、a-b 区间、a,b,c 枚举

### 常用表达式

```bash
*/5 * * * *    # 每 5 分钟
0 2 * * *      # 每天凌晨 2 点
0 2 * * 1      # 每周一凌晨 2 点
0 0 1 * *      # 每月 1 号零点
30 8-18 * * *  # 每天 8:30 到 18:30 的每小时的 30 分
```
- 描述: 高频表达式示例，背下来覆盖 90% 场景

### 标准任务行

```bash
*/10 * * * * /opt/scripts/check.sh >> /var/log/check.log 2>&1
```
- 描述: 每 10 分钟执行一次，输出追加日志，2>&1 合并错误流

## 示例

### 典型运维任务

- 描述: 备份、清理、巡检三大件

```bash
# 每天凌晨 2 点备份数据库
0 2 * * * /opt/scripts/backup-mysql.sh >> /var/log/backup.log 2>&1

# 每周日凌晨 3 点清理 30 天前的日志
0 3 * * 0 find /opt/app/logs -name "*.log" -mtime +30 -delete

# 每 5 分钟健康检查，异常时发告警
*/5 * * * * /opt/scripts/health-check.sh >> /var/log/health.log 2>&1
```

### 避坑要点

- 描述: 环境变量与输出管理

```bash
# 错误示范：直接用相对路径与非登录 shell 命令
* * * * * java -jar app.jar

# 正确示范：绝对路径 + 显式环境
* * * * * /usr/bin/java -jar /opt/app/app.jar >> /opt/app/run.log 2>&1

# 需要特殊环境时在 crontab 顶部声明
SHELL=/bin/bash
PATH=/usr/local/bin:/usr/bin:/bin
MAILTO=""
```

### 任务排查

- 描述: 任务没跑起来的检查路径

```bash
# 确认 cron 服务在跑
systemctl status crond

# 看 cron 执行记录（确认是否被调度）
grep CRON /var/log/syslog
grep CRON /var/log/cron

# 手动以 cron 同样环境试跑验证脚本本身
/bin/bash /opt/scripts/check.sh
```