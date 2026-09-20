---
name: Split 大文件切割
category: 文件操作
order: 4
---

## 介绍

split 把大文件按行数或字节大小切成多个小文件。超大日志不好打开、传输受限、需要并行处理时，先切割再逐个处理是标准做法；切割后的碎片还能用 cat 无损合并回去。

核心特点：
- **按行切割**：-l 每个分片固定行数，适合文本/日志
- **按大小切割**：-b 按字节切（100M 等），适合二进制大文件
- **自定义前缀后缀**：默认 xaa/xab，可用 -d 数字后缀、--additional-suffix 加扩展名
- **无损合并**：cat 分片按顺序拼接即还原原始文件

## 语法

### 按行切割

```bash
split -l 1000000 app.log part_
```
- 描述: 每个分片 100 万行，输出 part_aa part_ab ...

### 数字后缀

```bash
split -l 500000 -d app.log part_
```
- 描述: -d 后缀用数字（part_00 part_01），排序直观

### 按大小切割

```bash
split -b 100M bigdata.tar.gz part_
```
- 描述: 每片 100M，适合超大压缩包分批传输

### 加扩展名

```bash
split -l 500000 -d --additional-suffix=.log app.log part-
```
- 描述: 每个分片自动带 .log 后缀，part-00.log

### 合并还原

```bash
cat part_* > app-merged.log
```
- 描述: 通配符按字典序展开拼接，注意保证顺序（数字后缀需补零宽度）

### 切割并压缩

```bash
split -b 100M -d --filter='gzip > $FILE.gz' bigfile.bin part_
```
- 描述: --filter 对每个分片流式执行命令，边切边压缩省磁盘

## 示例

### 超大日志分而治之

- 描述: 几十 G 日志没法直接 grep 时

```bash
# 把大日志按 500 万行一片切开
split -l 5000000 -d --additional-suffix=.log app-2026.log part-

# 逐片并行统计（4 并发）
ls part-*.log | xargs -P 4 -I {} sh -c 'grep -c ERROR {} >> error-count.txt'

# 汇总
awk '{s+=$1} END {print "ERROR 总数:", s}' error-count.txt
```

### 大文件分批传输

- 描述: 网络不稳时分片传、传完合

```bash
# 切成 500M 一片
split -b 500M -d backup.tar.gz bak_

# 分批传输（配合 rsync/scp）
for f in bak_*; do scp "$f" user@host:/data/; done

# 远端合并校验
ssh user@host "cat bak_* > backup.tar.gz && md5sum backup.tar.gz"
```

### 处理 CSV 按行分片

- 描述: 保留表头的切割技巧

```bash
# 第一行是表头：head 单独存，数据行再切
head -1 data.csv > header.csv
tail -n +2 data.csv | split -l 100000 -d --additional-suffix=.csv chunk_

# 每个分片前拼回表头
for f in chunk_*.csv; do cat header.csv "$f" > "with-header-$f"; done
```