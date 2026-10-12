---
name: Redis 命令行
category: 数据库
order: 3
---

## 介绍

redis-cli 是 Redis 运维排障的基本功：连接、键扫描、读写诊断、大 key 定位、内存分析、慢查询。图形工具（RedisInsight、AnotherRedisDesktopManager）连不上内网时它是唯一入口，线上告警时也是最快确认现场的手段。

核心特点：
- **三种调用**：交互式登录、-e 单条执行、--pipe 管道批量导入
- **密码安全**：-a 会进 ps 历史和 shell 历史，优先用 REDISCLI_AUTH 环境变量
- **迭代扫描**：线上严禁 KEYS（阻塞主线程），一律 SCAN / --scan 游标遍历
- **四大排障面**：大 key、热 key、慢日志、内存碎片，一个命令各管一块

## 语法

### 连接登录

```bash
redis-cli -h 127.0.0.1 -p 6379 -a YourPass -n 0
```
- 描述: -h 主机 -p 端口 -n 库编号（0-15），-a 后直接跟密码；本机可只写 redis-cli

### 密码安全传参

```bash
REDISCLI_AUTH=YourPass redis-cli -h 10.0.0.8 -p 6379
```
- 描述: 用环境变量传密码，不进 ps 输出；-a 明文会被同机其他用户从 ps -ef 看到

### 单条命令执行

```bash
redis-cli -h 10.0.0.8 -p 6379 -n 0 GET user:1001
```
- 描述: 不进交互界面直接拿结果，脚本里这么用；返回 (nil) 表示键不存在

### 从 stdin 读值

```bash
echo '{"name":"zhangsan"}' | redis-cli -x SET user:1001
```
- 描述: -x 把 stdin 内容作为最后一个参数的值，适合存带特殊字符的大段内容

### 管道批量导入

```bash
cat dump.txt | redis-cli --pipe
```
- 描述: 文件需为 RESP 协议格式（\*3\r\n\$3\r\nSET...），比逐条 -e 快几个数量级

### 迭代扫描键

```bash
redis-cli --scan --pattern 'user:*' | head -50
```
- 描述: 基于 SCAN 游标遍历不阻塞服务端；--count 100 控制每批数量，大库务必加

### 查看键详情

```bash
redis-cli TYPE user:1001
redis-cli TTL user:1001
redis-cli OBJECT ENCODING user:1001
```
- 描述: TYPE 看类型、TTL 看剩余过期秒、OBJECT ENCODING 看底层编码（ziplist/listpack/hashtable）

### 大 key 排查

```bash
redis-cli MEMORY USAGE user:1001
redis-cli STRLEN big:str
redis-cli --bigkeys
```
- 描述: MEMORY USAGE 查单键字节（4.0+）；--bigkeys 自动遍历给出各类最大键，-i 控制间隔

### 服务端信息

```bash
redis-cli INFO stats
redis-cli INFO memory
redis-cli INFO replication
```
- 描述: INFO 分节输出；stats 看命中率与 QPS，memory 看内存与碎片，replication 看主从延迟

### 慢查询与延迟

```bash
redis-cli SLOWLOG GET 10
redis-cli --latency-history -i 5
```
- 描述: SLOWLOG 记录超过阈值的命令（默认 10ms，可 CONFIG SET 调整）；--latency-history 看延迟波动

### 客户端连接排查

```bash
redis-cli CLIENT LIST
redis-cli CONFIG GET maxmemory
```
- 描述: CLIENT LIST 看谁占着连接（含 idle 时长与最近命令）；CONFIG GET 查内存上限与淘汰策略

## 示例

### 线上 QPS 突增/命中率下跌排查

- 描述: 报警「缓存命中率下降」或「Redis CPU 高」时的标准流程

```bash
# 1. 命中率与 QPS：keyspace_hits / (hits + misses)
redis-cli INFO stats | grep -E 'keyspace_hits|keyspace_misses|instantaneous_ops_per_sec|expired_keys'

# 2. 慢命令：超过 10ms 的命令都在这，看是否有 KEYS/FLUSHDB/HGETALL 大键
redis-cli SLOWLOG GET 20

# 3. 谁在连、在连什么：addr 与 cmd 两列定位来源 IP 与操作类型
redis-cli CLIENT LIST | awk '{print $2, $5, $9}' | sort | uniq -c | sort -rn | head

# 4. 延迟与内存是否吃紧
redis-cli --latency -i 1 | head -5
redis-cli INFO memory | grep -E 'used_memory_human|maxmemory_human|mem_fragmentation_ratio'
```

### 大 key 扫描脚本

- 描述: 找出超过阈值的热点大 key，防止阻塞与内存倾斜（生产级，可定期跑）

```bash
#!/bin/bash
# bigkey_scan.sh —— 扫描超过 100KB 的 key 并输出 Top 20
set -euo pipefail

HOST="${1:-127.0.0.1}"
PORT="${2:-6379}"
export REDISCLI_AUTH="${REDISCLI_AUTH:-}"
LIMIT_BYTES=102400          # 100KB
PATTERN="${3:-*}"

echo "[$(date '+%F %T')] 扫描 $HOST:$PORT pattern=$PATTERN 阈值=$LIMIT_BYTES 字节"

redis-cli -h "$HOST" -p "$PORT" --scan --pattern "$PATTERN" --count 200 \
| while read -r key; do
    bytes=$(redis-cli -h "$HOST" -p "$PORT" MEMORY USAGE "$key" 2>/dev/null)
    [ -z "$bytes" ] && continue
    if [ "$bytes" -gt "$LIMIT_BYTES" ]; then
        type=$(redis-cli -h "$HOST" -p "$PORT" TYPE "$key")
        echo -e "$bytes\t$type\t$key"
    fi
done | sort -rn | head -20
```

### 数据迁移与备份

- 描述: 实例间搬数据、做临时备份的两种方式

```bash
# 方式一：导出 RDB 快照到本地（需要服务端允许 bgsave，不阻塞）
redis-cli -h 10.0.0.8 --rdb /backup/redis-dump-$(date +%F).rdb

# 方式二：按 key 迁移（先 DUMP 再 RESTORE，保留原 TTL）
for key in $(redis-cli -h 10.0.0.8 --scan --pattern 'session:*'); do
    ttl=$(redis-cli -h 10.0.0.8 TTL "$key")
    [ "$ttl" -le 0 ] && ttl=0
    redis-cli -h 10.0.0.8 DUMP "$key" | \
      redis-cli -x -h 10.0.0.9 RESTORE "$key" "$ttl"
done

# 方式三：协议文件管道导入（最快，适合离线数据灌回）
cat dump.txt | redis-cli -h 10.0.0.9 --pipe
```

### 内存持续增长定位

- 描述: 内存只涨不降时，区分「数据真多」和「碎片/未回收」

```bash
# 1. 关键字段：used_memory 数据量、used_memory_rss 实际占用、碎片率 1.5 以上要注意
redis-cli INFO memory | grep -E 'used_memory_human|used_memory_rss_human|mem_fragmentation_ratio|maxmemory_human'

# 2. 各类型 key 数量分布，看是哪类数据在涨
redis-cli INFO keyspace

# 3. 抽样看平均键大小，判断是「大 key 多」还是「小 key 海量」
redis-cli --scan --pattern '*' --count 500 | head -5000 \
  | xargs -I{} redis-cli MEMORY USAGE {} | awk '{s+=$1; n++} END {printf "平均 %d 字节 / 共 %d 键\n", s/n, n}'

# 4. 过期未删与淘汰情况：evicted_keys > 0 说明 maxmemory 已触顶
redis-cli INFO stats | grep -E 'evicted_keys|expired_keys'
```

### 热 key 与实时命令监控

- 描述: 定位单 key 被打爆的场景；MONITOR 只允许短时小流量使用

```bash
# MONITOR 实时打印所有命令：会显著拖慢实例，生产上最多看 3-5 秒就 Ctrl+C
timeout 5 redis-cli MONITOR | awk '{print $4}' | sort | uniq -c | sort -rn | head -10

# 没有 MONITOR 权限时，用 CLIENT LIST 的最近命令做近似判断
redis-cli CLIENT LIST | tr ' ' '\n' | grep cmd= | sort | uniq -c | sort -rn | head
```

### 生产安全红线

- 描述: 线上用 redis-cli 的四条军规，条条都是事故换来的

```bash
# 1. 不要 KEYS *：O(N) 且阻塞主线程，万级 key 就能卡死实例；一律 SCAN/--scan
#    反例：redis-cli KEYS 'user:*'
#    正例：redis-cli --scan --pattern 'user:*'

# 2. 不要 MONITOR 长时间挂着：等于给每个命令加一次全量广播，QPS 高时直接打挂

# 3. FLUSHALL / FLUSHDB / DEBUG 类命令要确认库编号（-n），误清库没有后悔药
#    删键前先确认类型与内容：redis-cli TYPE key && redis-cli GET key

# 4. 密码不要用 -a 明文（ps -ef 可见），用 REDISCLI_AUTH 环境变量或 ACL 文件；
#    服务端建议 rename-command FLUSHALL "" 和 rename-command KEYS "" 直接禁用
redis-cli CONFIG GET maxmemory-policy
```
