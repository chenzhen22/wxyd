---
name: Sort 与 Uniq
category: 文本处理
order: 4
---

## 介绍

sort 负责排序、uniq 负责去重计数，两者几乎总是成对出现，是日志统计（TOP N、访问排行、错误计数）的基础工具链。

核心特点：
- **sort 排序**：默认按字符排序，-n 数值排序、-r 反序、-k 按列
- **uniq 相邻去重**：只对相邻行生效，必须先 sort 再 uniq
- **uniq -c 计数**：每个唯一值出现次数，配合 sort -rn 出 TOP 榜
- **-t 指定分隔符**：按 CSV/冒号等特定列排序

## 语法

### 基本排序

```bash
sort file
```
- 描述: 按字符顺序（ASCII）升序排序

### 数值排序

```bash
sort -n file
```
- 描述: -n 按数值大小排，避免 10 排在 2 前面的问题

### 反序与去重

```bash
sort -r file
sort -u file
```
- 描述: -r 降序；-u 排序同时去重（等价 sort|uniq）

### 按列排序

```bash
sort -t: -k3 -n /etc/passwd
```
- 描述: -t 指定分隔符，-k3 按第 3 列，-n 数值比较

### uniq 去重计数

```bash
sort file | uniq -c
```
- 描述: -c 在行首加出现次数，先 sort 保证唯一值相邻

### 只显示重复行

```bash
sort file | uniq -d
```
- 描述: -d 只输出出现过多次的行，-u 只输出仅出现一次的行

### TOP N 排行

```bash
sort file | uniq -c | sort -rn | head -10
```
- 描述: 计数后按数值降序取前 10，日志统计万能模板

## 示例

### 访问 IP 排行

- 描述: 经典统计链：提取→计数→排序→截取

```bash
# 统计访问量 TOP 10 的 IP
awk '{print $1}' access.log | sort | uniq -c | sort -rn | head -10

# 统计被请求最多的页面 TOP 5
awk '{print $7}' access.log | sort | uniq -c | sort -rn | head -5
```

### 值分布统计

- 描述: 统计状态码分布与重复值

```bash
# 统计 HTTP 状态码分布
awk '{print $9}' access.log | sort | uniq -c | sort -rn
# 典型输出:
#   1580 200
#    120 404
#      5 500

# 找出重复出现的错误信息
grep "ERROR" app.log | sort | uniq -d
```

### 文件内容处理

- 描述: 合并去重与列排序

```bash
# 合并两个文件并去重
sort a.txt b.txt | uniq > merged.txt

# 求两个文件的交集（都有的行）
sort a.txt b.txt | uniq -d

# 按第 3 列数值降序排 CSV
sort -t, -k3 -rn data.csv
```