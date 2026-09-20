---
name: Awk 列处理
category: 文本处理
order: 2
---

## 介绍

awk 是强大的文本分析工具，按列（字段）处理文本，内置完整的编程能力：变量、条件、循环、函数。名字来自三位作者的首字母（Aho、Weinberger、Kernighan）。日志统计、数据提取、报表生成是它的主战场。

核心特点：
- **按列处理**：默认以空白分隔字段，$1 第 1 列、$2 第 2 列、$0 整行
- **模式-动作**：`模式 {动作}`，匹配行才执行动作
- **BEGIN/END**：处理前初始化、处理完后汇总输出
- **内置变量**：NR 行号、NF 列数、FS 分隔符、OFS 输出分隔符

## 语法

### 按列提取

```bash
awk '{print $1}' file
```
- 描述: 打印每行第 1 列，$NF 最后一列，$(NF-1) 倒数第二列

### 自定义分隔符

```bash
awk -F: '{print $1}' /etc/passwd
```
- 描述: -F 指定输入分隔符；-v OFS= 指定输出分隔符

### 条件过滤

```bash
awk '$3 > 100 {print $1, $3}' file
```
- 描述: 第 3 列大于 100 时才输出第 1 列和第 3 列，支持 == != > < >= <=

### 按行号取行

```bash
awk 'NR==5' file
```
- 描述: NR 为当前行号，取第 5 行；NR>=10&&NR<=20 取区间

### 统计求和

```bash
awk '{sum+=$2} END {print sum}' file
```
- 描述: 累加第 2 列，END 块在全部行处理完后执行

### BEGIN/END 结构

```bash
awk 'BEGIN {print "start"} {n++} END {print n" lines"}' file
```
- 描述: BEGIN 在读行前执行，主体每行执行，END 收尾输出

### 格式化输出

```bash
awk -F: '{printf "%-15s %s\n", $1, $3}' /etc/passwd
```
- 描述: printf 格式化输出，%-15s 左对齐占 15 字符，类似 C 语言

### 正则匹配

```bash
awk '/ERROR/ {print NR": "$0}' app.log
```
- 描述: /正则/ 作为模式，输出匹配行号与内容

## 示例

### 日志列提取

- 描述: 从 nginx 日志中提取 IP 和状态码

```bash
# nginx 日志格式: IP - - [time] "GET /path" status size ...
awk '{print $1}' access.log
# 输出所有访问 IP

awk '{print $1, $9}' access.log
# 输出 IP 和状态码两列

# 统计 404 的请求 IP
awk '$9 == 404 {print $1}' access.log | sort | uniq -c
```

### 数值统计汇总

- 描述: 求和、求平均、最大值

```bash
# data.txt: 每行 "名称 数量"，如 apple 10
awk '{sum+=$2} END {print "总和:", sum}' data.txt

awk '{sum+=$2; n++} END {printf "平均: %.1f\n", sum/n}' data.txt

awk 'BEGIN{max=0} $2>max {max=$2} END {print "最大:", max}' data.txt
```

### 多分隔符与字段重组

- 描述: 正则作分隔符、调整列顺序

```bash
# 多个连续空格或冒号都当分隔符
awk -F'[ :]+' '{print $1, $2}' mixed.txt

# 交换第 1、2 列输出
awk -v OFS="," '{print $2, $1}' data.txt
```

### 综合实战

- 描述: 统计访问量 TOP 3 的 IP

```bash
# 提取 IP 列去重计数后排序取前 3
awk '{print $1}' access.log | sort | uniq -c | sort -rn | head -3

# 纯 awk 实现（关联数组计数）
awk '{count[$1]++} END {for (ip in count) print count[ip], ip}' access.log | sort -rn | head -3
```