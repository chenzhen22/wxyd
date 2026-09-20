---
name: Grep 文本搜索
category: 文本处理
order: 1
---

## 介绍

grep（global regular expression print）是 Linux 下最常用的文本搜索工具，按正则表达式或固定字符串在文件/输入流中逐行匹配，输出匹配的行。日志过滤、配置查找、结果筛选几乎都离不开它。

核心特点：
- **逐行匹配**：按行输出包含模式的整行内容
- **正则支持**：基础正则（BRE）、扩展正则（-E）、Perl 正则（-P）
- **多种过滤模式**：忽略大小写、反向匹配、递归目录、仅输出匹配部分
- **配合管道**：作为过滤器串联在其他命令后使用

## 语法

### 基本搜索

```bash
grep "pattern" file
```
- 描述: 在 file 中搜索包含 pattern 的行并输出

### 忽略大小写

```bash
grep -i "error" app.log
```
- 描述: -i 忽略大小写，同时匹配 error/ERROR/Error

### 反向匹配

```bash
grep -v "DEBUG" app.log
```
- 描述: -v 反向过滤，输出不包含 DEBUG 的行

### 显示行号

```bash
grep -n "exception" app.log
```
- 描述: -n 在输出前加上匹配行在原文件中的行号

### 递归搜索目录

```bash
grep -r "serviceName" /etc/nginx/
```
- 描述: -r 递归搜索目录下所有文件，-n 可同时带行号

### 统计匹配行数

```bash
grep -c "ERROR" app.log
```
- 描述: -c 只输出匹配行的数量，不输出内容

### 输出匹配部分

```bash
grep -o "https\?://[a-zA-Z0-9./]*" page.html
```
- 描述: -o 只输出匹配到的部分而非整行，常配合正则做提取

### 显示上下文

```bash
grep -A 3 -B 1 "ERROR" app.log
```
- 描述: -A 后 3 行、-B 前 1 行、-C 前后各 N 行，方便看报错上下文

### 扩展正则

```bash
grep -E "^(GET|POST) /api" access.log
```
- 描述: -E 使用扩展正则，| 或、+ 一到多次等无需转义

## 示例

### 日志过滤常用组合

- 描述: 过滤错误并排除噪音

```bash
# 查 ERROR 且排除超时噪音
grep "ERROR" app.log | grep -v "Timeout"

# 统计今天日志里 ERROR 出现次数
grep -c "ERROR" app-2026.log

# 显示错误行及其后 5 行上下文
grep -A 5 "NullPointerException" app.log
```

### 进程与端口查询过滤

- 描述: 配合 ps 过滤进程（排除 grep 自身）

```bash
# 查找 java 进程，排除 grep 自身
ps aux | grep java | grep -v grep

# 等价写法：用正则字符技巧
ps aux | grep "[j]ava"
```

### 提取与统计

- 描述: -o 提取 IP 并配合排序计数

```bash
# 从 access.log 提取所有 IP 并统计 TOP 10
grep -oE "([0-9]{1,3}\.){3}[0-9]{1,3}" access.log | sort | uniq -c | sort -rn | head -10
```

### 递归查配置

- 描述: 在项目目录中定位配置项所在文件与行

```bash
# 在 /opt/app/config 下找包含 datasource.url 的文件与行号
grep -rn "datasource.url" /opt/app/config/

# 只搜指定后缀的文件
grep -rn --include="*.yml" "server.port" /opt/app/config/
```