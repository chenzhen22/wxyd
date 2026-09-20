---
name: Sed 流编辑
category: 文本处理
order: 3
---

## 介绍

sed（stream editor）是流式文本编辑器，逐行读取文本、按脚本命令处理后输出。与 vim 等交互式编辑器不同，sed 非交互、适合脚本化批量修改文件，是三剑客中负责「替换与编辑」的角色。

核心特点：
- **流式处理**：逐行读入 → 模式空间处理 → 输出，内存占用低
- **s 替换为王**：s/旧/新/标志 是 90% 场景的核心命令
- **-i 直接改文件**：不加 -i 只输出到屏幕，加了才真正写回
- **支持正则**：配合 BRE/ERE 实现复杂模式替换

## 语法

### 基本替换

```bash
sed 's/old/new/' file
```
- 描述: 每行替换第一处 old 为 new，输出到屏幕不改原文件

### 全局替换

```bash
sed 's/old/new/g' file
```
- 描述: g 标志替换每行所有匹配，不加只替换每行第一处

### 直接修改文件

```bash
sed -i 's/old/new/g' file
```
- 描述: -i 将结果写回原文件，生产环境建议先加 -i.bak 备份

### 指定行处理

```bash
sed '3s/old/new/' file
```
- 描述: 只处理第 3 行；'1,5s/...' 处理 1-5 行；'$s/...' 处理末行

### 删除行

```bash
sed '/pattern/d' file
```
- 描述: d 删除匹配行；'3d' 删第 3 行；'/^$/d' 删空行

### 追加与插入

```bash
sed '/pattern/a new_line' file
```
- 描述: a 在匹配行后追加，i 在匹配行前插入

### 打印指定行

```bash
sed -n '10,20p' file
```
- 描述: -n 抑制默认输出，p 打印第 10-20 行，等效 sed '10,20!d'

### 使用扩展正则

```bash
sed -E 's/(http:\/\/[^ ]+)/<a>\1<\/a>/' file
```
- 描述: -E 启用扩展正则，\1 引用第一个分组捕获内容

## 示例

### 配置文件批量修改

- 描述: 修改端口并安全备份

```bash
# 修改配置中的端口（先自动备份为 .bak）
sed -i.bak 's/port=8080/port=9090/' server.properties

# 替换配置里的 IP
sed -i 's/192.168.1.100/10.0.0.50/g' application.yml
```

### 删除与清理

- 描述: 删空行、去注释、去行尾空白

```bash
# 删除空行
sed '/^$/d' data.txt

# 删除 # 开头的注释行
sed '/^#/d' config.conf

# 去掉行尾空白字符
sed 's/[ \t]*$//' file.txt
```

### 行范围处理

- 描述: 按行号截取与编辑

```bash
# 提取第 10-20 行（-n + p）
sed -n '10,20p' app.log

# 删除最后一行
sed '$d' file.txt

# 第 2 行后插入一行
sed '2i # inserted by script' script.sh
```

### 正则捕获引用

- 描述: 用分组捕获重排内容

```bash
# 交换 CSV 的第 1、2 列
echo "name,age" | sed -E 's/^(.*),(.*)$/\2,\1/'
# 输出: age,name

# 给每行行首加序号
sed '=' file.txt | sed 'N;s/\n/. /'
```