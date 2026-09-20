---
name: Find 文件查找
category: 文件操作
order: 1
---

## 介绍

find 用于在目录树中按条件查找文件与目录，可按名称、大小、时间、类型、权限等组合过滤，还能用 -exec 对找到的文件直接执行命令。磁盘清理、日志归档、批量改名都靠它。

核心特点：
- **实时全盘查找**：遍历目录树实时搜索（区别于 locate 查数据库）
- **条件丰富**：-name 名称、-size 大小、-mtime 时间、-type 类型
- **逻辑组合**：-a 与、-o 或、! 非，精确圈定范围
- **-exec 执行**：找到即处理，与 xargs 配合威力更大

## 语法

### 按名称查找

```bash
find /path -name "*.log"
```
- 描述: 通配符要加引号；-iname 忽略大小写

### 按类型查找

```bash
find /path -type f
```
- 描述: f 普通文件、d 目录、l 软链接

### 按时间查找

```bash
find /path -mtime +7
```
- 描述: +7 为 7 天前修改过；-7 为 7 天内；mmin 按分钟计

### 按大小查找

```bash
find /path -size +100M
```
- 描述: +100M 大于 100 兆；-1k 小于 1K；c/k/M/G 单位

### 执行命令

```bash
find /path -name "*.tmp" -exec rm {} \;
```
- 描述: {} 是占位符代表找到的文件，\; 结束；+ 可批量传递

### 条件组合

```bash
find /path -name "*.log" -a -mtime +30
```
- 描述: -a 与、-o 或、! 非；默认多条件即为与

### 限制目录深度

```bash
find /path -maxdepth 2 -type d
```
- 描述: maxdepth 最多下探 2 层，mindepth 最浅层数

### 空目录与空文件

```bash
find /path -type d -empty
```
- 描述: -empty 查找空目录或空文件，常配合 -delete 清理

## 示例

### 日志清理

- 描述: 定时清理旧日志的经典组合

```bash
# 删除 30 天前的日志
find /opt/app/logs -name "*.log" -mtime +30 -exec rm -f {} \;

# 等价写法：-delete 更简洁高效
find /opt/app/logs -name "*.log" -mtime +30 -delete

# 清理空目录
find /opt/app/logs -type d -empty -delete
```

### 查大文件定位磁盘

- 描述: 找出占空间的元凶

```bash
# 查找大于 500M 的文件并列出详情
find / -type f -size +500M -exec ls -lh {} \; 2>/dev/null

# 查找 7 天内修改过的大文件
find /data -type f -size +100M -mtime -7
```

### 批量操作

- 描述: 批量改名、复制、改权限

```bash
# 把所有 .sh 加执行权限
find /opt/scripts -name "*.sh" -exec chmod +x {} \;

# 把 30 天前的日志移到归档目录
find /opt/app/logs -name "*.log" -mtime +30 -exec mv {} /archive/ \;

# 批量改后缀 .log 为 .log.bak
find . -name "*.log" -exec mv {} {}.bak \;
```

### 内容联合查找

- 描述: find 圈文件 + grep 查内容

```bash
# 在所有 yml 文件中查含 datasource 的文件
find /opt/app/config -name "*.yml" -exec grep -l "datasource" {} \;

# 统计当前项目 java 文件总数
find . -name "*.java" -type f | wc -l
```