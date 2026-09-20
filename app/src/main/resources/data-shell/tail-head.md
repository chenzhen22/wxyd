---
name: Tail 与 Head
category: 文件操作
order: 3
---

## 介绍

tail 看文件尾部、head 看文件头部、wc 计数，三个轻量命令组合起来覆盖了「看日志、追日志、数行数」的日常。其中 tail -f 实时追踪日志是运维使用频率最高的单条命令。

核心特点：
- **tail -f 实时追**：文件新增内容即时刷屏，盯日志神器
- **-F 带轮转追踪**：日志被切割重建后自动跟上新文件，-f 则会停
- **head/tail 取片段**：-n 指定行数，组合可取中间任意区段
- **wc 计数三件**：-l 行数、-w 单词数、-c 字节数

## 语法

### 查看尾部

```bash
tail -n 100 app.log
```
- 描述: 看最后 100 行；-n 100 可简写为 tail -100 app.log

### 实时追踪

```bash
tail -f app.log
```
- 描述: 阻塞式实时输出新增内容，Ctrl+C 退出

### 轮转安全追踪

```bash
tail -F app.log
```
- 描述: 文件被 logrotate 切割重建后仍能继续追踪，盯日志推荐 -F

### 过滤着追

```bash
tail -f app.log | grep --line-buffered "ERROR"
```
- 描述: 追日志同时只看错误行；grep 需 --line-buffered 才能实时刷出

### 查看头部

```bash
head -n 20 app.log
```
- 描述: 看前 20 行，常用来确认日志起始时间/文件格式

### 取中间区段

```bash
sed -n '100,200p' app.log
head -200 app.log | tail -100
```
- 描述: 两种方式取第 100-200 行，sed 更直观

### 计数

```bash
wc -l app.log
grep -c "ERROR" app.log
```
- 描述: wc -l 统计总行数；grep -c 直接统计匹配行数更省事

## 示例

### 日常追日志

- 描述: 实时盯错误与请求

```bash
# 实时盯最新日志
tail -F /opt/app/logs/app.log

# 只看错误实时刷屏
tail -F /opt/app/logs/app.log | grep --line-buffered ERROR

# 追两个日志同时看
tail -F app.log access.log

# 看最后 500 行里最近发生的错误
tail -500 /opt/app/logs/app.log | grep ERROR
```

### 多机日志采集

- 描述: 免密配合 ssh 远程追日志

```bash
# 远程日志实时拉到本机看
ssh root@192.168.1.100 "tail -F /opt/app/logs/app.log" | grep --line-buffered ERROR

# 统计每个 ERROR 类型的出现次数
grep "ERROR" app.log | awk -F'ERROR ' '{print $2}' | cut -d: -f1 | sort | uniq -c | sort -rn | head
```

### 文件体检

- 描述: 快速了解一个陌生日志文件

```bash
# 总行数
wc -l app.log

# 看开头 3 行确认格式与起始时间
head -3 app.log

# 看结尾 3 行确认结束时间
tail -3 app.log

# 大小与行数一起看
ls -lh app.log; wc -l app.log
```