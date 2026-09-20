---
name: MySQL 命令行
category: 数据库
order: 1
---

## 介绍

mysql 客户端是数据库操作的基本功：连接、查询、批量执行 SQL、结果导出。运维排障时经常需要在服务器上直接查库（连接池耗尽、锁等待、数据修正），图形工具连不上的内网环境里它是唯一入口。

核心特点：
- **三种调用**：交互式登录、-e 单条执行、批量脚本执行
- **格式化输出**：-t 表格、-B 制表符、--xml/--batch 供程序消费
- **安全连接**：密码走 my.cnf 配置或 login-path，避免命令行明文
- **常用运维 SQL**：看连接、看锁、看变量，三招定位 80% 问题

## 语法

### 连接登录

```bash
mysql -h 127.0.0.1 -P 3306 -u app -p wxyd
```
- 描述: -h 主机 -P 端口 -u 用户 -p 交互输密码，最后的 wxyd 是库名；本机可省 -h

### 单条命令执行

```bash
mysql -u app -p wxyd -e "SELECT count(*) FROM user;"
```
- 描述: -e 直接执行不进交互界面，脚本里这么用；密码可配在 my.cnf 免输

### 密码免输入

```bash
# ~/.my.cnf（chmod 600）
[client]
user=app
password=YourPass
host=127.0.0.1
```
- 描述: 配置后直接 mysql wxyd 即可；权限必须 600，否则 mysql 会拒绝读取

### 格式化输出

```bash
mysql -t wxyd -e "SELECT id,username FROM user LIMIT 3;"
mysql -B wxyd -e "..." | awk -F'\t' '{print $1}'
```
- 描述: -t 表格模式（交互默认）；-B 制表符分隔，方便管道取列

### 导出结果

```bash
mysql -B wxyd -e "SELECT ..." > /tmp/result.tsv
```
- 描述: 重定向保存；INTO OUTFILE 需要服务端 FILE 权限，客户端导出更通用

### 执行 SQL 文件

```bash
mysql wxyd < init.sql
```
- 描述: 批量执行脚本；加 -f 遇错继续，--verbose 显示执行过程

### 常用运维 SQL

```sql
SHOW PROCESSLIST;                 -- 当前连接与正在执行的 SQL
SHOW FULL PROCESSLIST;            -- 完整 SQL 文本
SHOW VARIABLES LIKE 'max_connections';
SHOW STATUS LIKE 'Threads_connected';
SELECT * FROM information_schema.innodb_trx\G   -- 当前事务与锁等待
```
- 描述: 连接堆积、慢 SQL、锁等待三板斧；\G 竖排显示更易读

## 示例

### 连接池耗尽排查

- 描述: 应用报 connection is closed / 拿不到连接时

```bash
# 1. 看当前连接数与上限
mysql -e "SHOW STATUS LIKE 'Threads_connected'; SHOW VARIABLES LIKE 'max_connections';"

# 2. 看谁占着连接、在干什么
mysql -e "SHOW FULL PROCESSLIST;" | awk '{print $3, $4, $5, $6, $7}' | head -20

# 3. 杀掉超时空闲连接（Time 状态且 > 600 秒）
mysql -e "SELECT concat('KILL ', id, ';') FROM information_schema.processlist WHERE command='Sleep' AND time>600;" | mysql
```

### 数据快速核对与导出

- 描述: 运维对账常用

```bash
# 统计各状态订单数
mysql -B wxyd -e "SELECT status, count(*) FROM t_order GROUP BY status;"

# 导出昨天数据给业务方（制表符分隔可直接开 Excel）
mysql -B wxyd -e "SELECT * FROM t_trans WHERE create_time >= CURDATE() - INTERVAL 1 DAY" > /tmp/yesterday.tsv

# 交互式竖排看单条记录
mysql wxyd -e "SELECT * FROM t_user WHERE id=1001\G"
```

### 安全规范

- 描述: 命令行用库的三条红线

```bash
# 1. 不在命令行明文带密码（ps 历史可见）：用 ~/.my.cnf 或交互输入
# 2. 生产只读账号优先：CREATE USER 'ro'@'%' IDENTIFIED BY 'x'; GRANT SELECT ON wxyd.* TO 'ro'@'%';
# 3. 变更先备份再执行：mysqldump 单表后跑 UPDATE/DELETE
```