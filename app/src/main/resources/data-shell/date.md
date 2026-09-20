---
name: Date 时间处理
category: Shell 进阶
order: 1
---

## 介绍

date 显示与设置系统时间，其格式化与日期运算能力是脚本里生成时间戳、构造备份文件名、计算「N 天前」的核心工具。日志归档、定时备份命名、报表周期都依赖它。

核心特点：
- **+格式符**：+%Y%m%d 等组合出任意时间格式
- **日期运算**：-d "-1 day" 便捷计算昨天、上周、下月
- **时间戳互转**：+%s 秒级时间戳与日期自由转换
- **脚本命名利器**：$(date +%Y%m%d) 拼进文件名，天然按日归档

## 语法

### 常用格式符

```bash
date +%Y-%m-%d
```
- 描述: %Y 年 %m 月 %d 日 %H 时 %M 分 %S 秒；%F 等于 %Y-%m-%d，%T 等于 %H:%M:%S

### 完整时间输出

```bash
date "+%F %T"
```
- 描述: 输出 2026-09-15 14:30:00 形式，日志前缀最常用

### 日期加减运算

```bash
date -d "-1 day" +%F
```
- 描述: -d 支持偏移：-1 day / last month / +2 hour / next Monday

### 时间戳转换

```bash
date +%s
date -d @1726400000 "+%F %T"
```
- 描述: +%s 取当前秒级时间戳；@秒数 反解为可读时间

### 毫秒与纳秒

```bash
date +%s%3N
```
- 描述: %N 纳秒，%3N 截前 3 位即毫秒，做 traceId 前缀很方便

### 指定时区

```bash
TZ="Asia/Shanghai" date "+%F %T"
```
- 描述: TZ 环境变量临时切时区输出，排查跨时区问题有用

## 示例

### 备份文件命名

- 描述: 带日期的归档名，天然按天分文件

```bash
# 生成形如 logs-20260915.tar.gz 的文件名
ts=$(date +%Y%m%d)
tar -czvf logs-${ts}.tar.gz /opt/app/logs/

# 昨天的日志目录（配合 find 处理昨日数据）
yesterday=$(date -d "-1 day" +%Y-%m-%d)
grep "$yesterday" /opt/app/logs/app.log
```

### 日志前缀与耗时统计

- 描述: 脚本里打点计时

```bash
# 每条输出带时间前缀
echo "[$(date '+%F %T')] 任务开始"

# 计算任务耗时（秒）
start=$(date +%s)
sleep 2
end=$(date +%s)
echo "耗时 $((end - start)) 秒"
```

### 周期判断与区间计算

- 描述: 按月/周分支、批量生成日期序列

```bash
# 判断是否月末（明天是 1 号则今天是月末）
[ "$(date -d tomorrow +%d)" = "01" ] && echo "今天是月末，跑月度汇总"

# 列出最近 7 天的日期
for i in $(seq 0 6); do date -d "-$i day" +%F; done

# 上个月字符串（月度表名/归档目录）
date -d "last month" +%Y%m
```