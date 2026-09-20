---
name: 实战日志备份脚本
category: 实战模板
order: 1
---

## 介绍

本节把前面学的 find、tar、date、crontab、scp 串成一个生产可用的日志定时备份脚本：按天打包日志、保留最近 N 天本地归档、超期清理、可选异地传输。逐段讲解设计思路，可直接改造使用。

核心特点：
- **参数化配置**：路径、保留天数集中定义在头部，改配置不改逻辑
- **幂等安全**：set -euo pipefail + 严格变量，出错即停
- **本地保留策略**：备份包只留 N 天，自动滚动清理
- **可挂 cron**：一行 crontab 即可无人值守

## 语法

### 脚本骨架

```bash
#!/bin/bash
set -euo pipefail
trap 'echo "[ERROR] 失败于第 $LINENO 行" >&2' ERR
```
- 描述: 严格模式 + 错误行号捕获，生产脚本标配开头

### 配置区

```bash
LOG_DIR="/opt/app/logs"
BACKUP_DIR="/data/backup/logs"
KEEP_DAYS=7
TODAY=$(date +%Y%m%d)
```
- 描述: 所有可变项集中在顶部，部署时只需改这里

### 打包动作

```bash
tar -czf "${BACKUP_DIR}/logs-${TODAY}.tar.gz" "${LOG_DIR}"
```
- 描述: 把日志目录打成带日期的压缩包；-z 压缩省空间

### 过期清理

```bash
find "${BACKUP_DIR}" -name "logs-*.tar.gz" -mtime +${KEEP_DAYS} -delete
```
- 描述: 按保留天数滚动删除旧备份，防止备份本身撑爆磁盘

### 异地传输（可选）

```bash
scp "${BACKUP_DIR}/logs-${TODAY}.tar.gz" backup@bak-host:/data/logs/
```
- 描述: 配合 SSH 免密把备份传到备份机，本地异地双份

## 示例

### 完整脚本 backup-logs.sh

- 描述: 可直接落地的完整版本

```bash
#!/bin/bash
# 日志每日备份脚本：打包 -> 清理过期 -> 可选异地
set -euo pipefail
trap 'echo "[ERROR] 失败于第 $LINENO 行" >&2' ERR

LOG_DIR="/opt/app/logs"
BACKUP_DIR="/data/backup/logs"
KEEP_DAYS=7
REMOTE=""                          # 形如 user@host:/path，留空则不传
TODAY=$(date +%Y%m%d)
FILE="${BACKUP_DIR}/logs-${TODAY}.tar.gz"

mkdir -p "${BACKUP_DIR}"

# 1. 打包（只收当天的 .log，避免重复打包旧包）
find "${LOG_DIR}" -name "*.log" -mtime -1 -type f \
  | tar -czf "${FILE}" -T -

# 2. 校验包可用
tar -tzf "${FILE}" > /dev/null && echo "备份成功: ${FILE}"

# 3. 清理过期备份
find "${BACKUP_DIR}" -name "logs-*.tar.gz" -mtime +${KEEP_DAYS} -delete

# 4. 异地传输（REMOTE 非空时）
if [ -n "${REMOTE}" ]; then
    scp "${FILE}" "${REMOTE}/"
    echo "已异地传输至 ${REMOTE}"
fi
```

### 部署上线

- 描述: 赋权 + 挂 crontab 两步走

```bash
# 加执行权限并手动跑一次验证
chmod +x /opt/scripts/backup-logs.sh
/opt/scripts/backup-logs.sh

# 写入 crontab：每天凌晨 2:30 执行，输出进日志
crontab -e
# 30 2 * * * /opt/scripts/backup-logs.sh >> /var/log/backup-logs.log 2>&1
```