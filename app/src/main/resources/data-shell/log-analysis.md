---
name: 实战日志分析脚本
category: 实战模板
order: 2
---

## 介绍

本节把 grep、awk、sort、uniq、date 串成一套日志分析脚本：统计当日错误率、访问 TOP、慢请求分布，输出一页式巡检报告。这些模板稍加改造即可用于 nginx/tomcat/自研服务的日志巡检。

核心特点：
- **管道流水线**：grep 过滤 → awk 取列 → sort/uniq 计数，分段可测
- **按天过滤**：grep "$(date +%F)" 圈定当天数据，避免全量扫描
- **输出即报告**：所有统计拼成段落，重定向到文件即可发群里
- **可挂 cron**：每天早上自动生成昨日报告

## 语法

### 当日错误统计

```bash
grep -c "ERROR" "${LOG}"
```
- 描述: 快速统计错误行数；配合 -F 固定串更快

### TOP N 访问统计

```bash
awk '{print $1}' access.log | sort | uniq -c | sort -rn | head -10
```
- 描述: 访问 IP 排行万能模板，取列后计数排序

### 状态码分布

```bash
awk '{print $9}' access.log | sort | uniq -c | sort -rn
```
- 描述: 看健康度：2xx 占比、4xx/5xx 数量一目了然

### 错误率计算

```bash
total=$(wc -l < access.log)
errors=$(awk '$9 >= 500' access.log | wc -l)
echo "scale=2; ${errors} * 100 / ${total}" | bc
```
- 描述: awk 条件过滤行数 + bc 算百分比，输出形如 3.25

### 慢请求提取

```bash
awk '$NF > 1 {print $7, $NF}' access.log | sort -k2 -rn | head
```
- 描述: 假设最后一列是响应秒数，筛出超 1 秒的请求按耗时排序

## 示例

### 完整巡检脚本 log-report.sh

- 描述: 每日一页报告，改造自 nginx 日志场景

```bash
#!/bin/bash
# 每日日志巡检报告：昨日错误率 + TOP 统计
set -euo pipefail

LOG="/opt/app/logs/access.log"
DAY=$(date -d "-1 day" +%d/%b/%Y)      # nginx 时间格式: 14/Sep/2026
REPORT="/var/log/report-$(date +%Y%m%d).txt"

{
echo "======== 日志巡检报告 $(date +%F) ========"
echo ""

echo "-- 1. 状态码分布 --"
grep "${DAY}" "${LOG}" | awk '{print $9}' | sort | uniq -c | sort -rn

echo ""
echo "-- 2. 5xx 错误率 --"
total=$(grep -c "${DAY}" "${LOG}" || true)
err=$(grep "${DAY}" "${LOG}" | awk '$9 >= 500' | wc -l)
if [ "${total}" -gt 0 ]; then
    printf "5xx: %s / %s (%.2f%%)\n" "${err}" "${total}" \
        "$(echo "scale=4; ${err} * 100 / ${total}" | bc)"
fi

echo ""
echo "-- 3. 访问 TOP 10 IP --"
grep "${DAY}" "${LOG}" | awk '{print $1}' | sort | uniq -c | sort -rn | head -10

echo ""
echo "-- 4. 访问 TOP 10 页面 --"
grep "${DAY}" "${LOG}" | awk '{print $7}' | sort | uniq -c | sort -rn | head -10

echo ""
echo "-- 5. ERROR 关键字条数 --"
grep -c "ERROR" /opt/app/logs/app.log || true
} > "${REPORT}"

echo "报告已生成: ${REPORT}"
```

### 部署与扩展

- 描述: 挂 cron 每日自动出报告

```bash
# 手动跑一次验证输出
chmod +x log-report.sh && ./log-report.sh && cat /var/log/report-*.txt | tail -30

# 每天早上 8 点生成昨日报告
# 0 8 * * * /opt/scripts/log-report.sh >> /var/log/report-cron.log 2>&1

# 扩展方向：报告追加钉钉/邮件推送
# curl -H "Content-Type: application/json" -d "{\"msgtype\":\"text\",\"text\":{\"content\":\"$(cat ${REPORT})\"}}" https://oapi.dingtalk.com/robot/send?access_token=xxx
```