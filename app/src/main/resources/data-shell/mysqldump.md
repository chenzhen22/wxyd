---
name: Mysqldump 备份
category: 数据库
order: 2
---

## 介绍

mysqldump 是 MySQL 逻辑备份标准工具，导出可读的 SQL 文件，跨版本恢复兼容性最好。单库/单表备份、变更前快照、定时全量备份都靠它；恢复用 mysql 客户端导入即可，配合 crontab 与日期命名就是完整的备份方案。

核心特点：
- **逻辑备份**：导出 SQL 语句，跨平台跨版本，小中型库首选
- **粒度灵活**：全实例 → 单库 → 单表 → 仅表结构/仅数据
- **一致性**：--single-transaction 对 InnoDB 做快照备份不锁表
- **压缩省盘**：管道 gzip 输出，几十 G 的库也能轻松存放

## 语法

### 备份单库

```bash
mysqldump -u app -p --single-transaction wxyd > wxyd.sql
```
- 描述: --single-transaction 一致性快照不锁 InnoDB 表，生产必备参数

### 备份并压缩

```bash
mysqldump -u app -p --single-transaction wxyd | gzip > wxyd-$(date +%F).sql.gz
```
- 描述: 管道压缩一步到位，文件名带日期便于按天管理

### 备份单表

```bash
mysqldump -u app -p wxyd t_order > t_order.sql
```
- 描述: 库名后跟表名即只备该表；多表空格隔开

### 仅表结构 / 仅数据

```bash
mysqldump -u app -p -d wxyd t_order > schema.sql
mysqldump -u app -p -t wxyd t_order > data.sql
```
- 描述: -d 只导结构（建表语句）；-t 只导数据；迁移表结构用 -d

### 按条件导出

```bash
mysqldump -u app -p wxyd t_order --where="create_time >= '2026-09-01'" > sep.sql
```
- 描述: --where 按条件导部分数据，配合备份归档分区表很实用

### 恢复导入

```bash
gzip -d < wxyd-2026-09-15.sql.gz | mysql -u app -p wxyd
mysql -u app -p wxyd < t_order.sql
```
- 描述: 解压管道导入或直接导入；恢复前建议先确认目标库状态

### 全实例备份

```bash
mysqldump -u root -p --single-transaction --all-databases --routines --events | gzip > all.sql.gz
```
- 描述: --all-databases 全库；--routines/--events 带上存储过程与事件，完整迁移必加

## 示例

### 变更前快照

- 描述: 任何 UPDATE/DELETE 前先拍快照

```bash
# 批量改数据前的安全带
mysqldump -u app -p --single-transaction wxyd t_user > /tmp/t_user-bak-$(date +%H%M).sql

# 执行变更
mysql -u app -p wxyd -e "UPDATE t_user SET status=1 WHERE id IN (...);"

# 改错了秒回滚单表
mysql -u app -p wxyd_new < /tmp/t_user-bak-1042.sql
```

### 定时备份脚本 backup-mysql.sh

- 描述: 配合 crontab 的完整方案

```bash
#!/bin/bash
set -euo pipefail
BAK_DIR="/data/backup/mysql"
KEEP_DAYS=7
DB="wxyd"
TS=$(date +%Y%m%d-%H%M)

mkdir -p "$BAK_DIR"

# 全库一致性备份 + 压缩
mysqldump -u backup -p"xxxx" --single-transaction --routines "$DB" \
  | gzip > "${BAK_DIR}/${DB}-${TS}.sql.gz"

# 校验备份文件可读（gzip -t 完整性）
gzip -t "${BAK_DIR}/${DB}-${TS}.sql.gz"

# 清理过期备份
find "$BAK_DIR" -name "${DB}-*.sql.gz" -mtime +"$KEEP_DAYS" -delete

# crontab: 0 2 * * * /opt/scripts/backup-mysql.sh >> /var/log/backup-mysql.log 2>&1
```

### 恢复演练与异机迁移

- 描述: 备份没恢复过等于没备份

```bash
# 1. 建演练库导入验证
mysql -u root -p -e "CREATE DATABASE wxyd_test;"
gunzip < wxyd-20260915-0200.sql.gz | mysql -u root -p wxyd_test
mysql -u root -p wxyd_test -e "SELECT COUNT(*) FROM t_order;"

# 2. 迁移到新机器（传输 + 导入）
scp wxyd-20260915-0200.sql.gz backup@new-host:/tmp/
ssh backup@new-host "gunzip < /tmp/wxyd-*.sql.gz | mysql -u root -p wxyd"

# 3. 每季度演练一次，记录恢复耗时作为 RTO 依据
```