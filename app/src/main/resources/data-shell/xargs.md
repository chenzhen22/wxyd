---
name: Xargs 批量处理
category: 文本处理
order: 5
---

## 介绍

xargs 把管道上一命令的输出（标准输入）转换成下一命令的参数列表。很多命令（rm、cp、chmod）不接受管道输入，xargs 就是它们与 grep/find/awk 之间的桥梁，还能控制并发批量执行。

核心特点：
- **管道转参数**：解决「命令不接受 stdin」的问题
- **-I 占位符**：自定义参数插入位置
- **-n 分批**：每批传几个参数
- **-P 并发**：并行执行多个进程，加速批量任务

## 语法

### 基本用法

```bash
cat filelist.txt | xargs rm -f
```
- 描述: 把每行内容作为参数追加到 rm -f 后执行

### 占位符

```bash
cat list.txt | xargs -I {} cp {} /backup/
```
- 描述: -I {} 指定占位符，{} 出现的位置就是参数插入点

### 每批数量

```bash
echo a b c d e | xargs -n 2 echo
```
- 描述: -n 2 每次只传 2 个参数，分多批执行

### 并发执行

```bash
cat urls.txt | xargs -P 4 -I {} curl -sO {}
```
- 描述: -P 4 最多 4 个进程并发，加速批量下载等任务

### 确认执行

```bash
ls *.log | xargs -p rm
```
- 描述: -p 每次执行前打印命令并询问 y/n，防误删

### 处理含空格文件名

```bash
find . -name "*.log" -print0 | xargs -0 rm -f
```
- 描述: -print0 与 -0 配对，以 \0 分隔，安全处理空格/特殊字符文件名

## 示例

### 与 find 组合

- 描述: 查找后批量处理（比 -exec 更灵活）

```bash
# 找到 30 天前的日志并删除
find /opt/app/logs -name "*.log" -mtime +30 | xargs rm -f

# 安全版：处理可能带空格的文件名
find /data -name "*.tmp" -print0 | xargs -0 rm -f

# 统计所有 java 文件的代码行数
find . -name "*.java" | xargs wc -l | tail -1
```

### 与 grep 组合

- 描述: 跨文件批量搜索

```bash
# 在 find 圈出的文件中搜关键词
find /opt/app/config -name "*.yml" | xargs grep -l "datasource"

# 多文件批量替换（sed -i 改一批文件）
ls *.conf | xargs sed -i 's/old_host/new_host/g'
```

### 并发批量任务

- 描述: -P 并行加速批量下载/处理

```bash
# 4 并发批量下载
cat urls.txt | xargs -P 4 -I {} curl -s -O {}

# 批量压缩目录，2 并发
ls -d */ | xargs -P 2 -I {} tar -czvf {}.tar.gz {}
```