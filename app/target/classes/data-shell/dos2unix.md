---
name: Dos2Unix 格式转换
category: 文件操作
order: 5
---

## 介绍

Windows 的换行是 CRLF（\r\n），Linux 是 LF（\n）。在 Windows 编辑过的脚本传到 Linux 执行会报 `$'\r': command not found` 或 `/bin/bash^M: bad interpreter`。dos2unix 一键转换换行符，是 Windows 与 Linux 协作开发必会的小工具。

核心特点：
- **换行符差异**：Windows=CRLF、Linux/macOS=LF，肉眼不可见但程序敏感
- **典型报错**：`command not found`、`bad interpreter`、yaml 解析失败都可能是它
- **双向转换**：dos2unix 转 Linux 格式，unix2dos 反向转回
- **识别方法**：cat -A 看行尾 ^M$、file 命令看 CRLF 标记

## 语法

### 转换文件

```bash
dos2unix script.sh
```
- 描述: 把 CRLF 转为 LF，直接原地修改

### 批量转换

```bash
dos2unix *.sh
find . -name "*.sh" -exec dos2unix {} \;
```
- 描述: 通配符或配合 find 批量处理整个脚本目录

### 保留原文件

```bash
dos2unix -n win.txt linux.txt
```
- 描述: -n 新旧文件分开，win.txt 保持不动，结果写入 linux.txt

### 反向转换

```bash
unix2dos linux.txt
```
- 描述: LF 转 CRLF，把 Linux 文件给 Windows 记事本用时才需要

### 识别换行符

```bash
file script.sh
cat -A script.sh | head
```
- 描述: file 显示 "with CRLF line terminators" 即 Windows 格式；cat -A 里 ^M$ 表示 CRLF

### 无 dos2unix 时替代

```bash
sed -i 's/\r$//' script.sh
tr -d '\r' < win.txt > linux.txt
```
- 描述: 用 sed 删行尾 \r 或 tr 过滤，服务器没装工具时用

## 示例

### 脚本报错排查

- 描述: 从报错到修复的完整流程

```bash
# 报错现象：./deploy.sh 执行报
# bash: ./deploy.sh: /bin/bash^M: bad interpreter

# 1. 确认是 CRLF 问题
file deploy.sh
# 输出: deploy.sh: ... with CRLF line terminators

# 2. 一键修复
dos2unix deploy.sh

# 3. 验证并执行
file deploy.sh
./deploy.sh
```

### 批量修复整个项目

- 描述: 从 Windows 拷来的目录整体转换

```bash
# 转换所有 sh 与 conf
find /opt/scripts -name "*.sh" -o -name "*.conf" | xargs dos2unix

# 转换前先备份一份
cp -r /opt/scripts /tmp/scripts.bak
find /opt/scripts -type f -exec dos2unix -q {} \;
```

### 预防措施

- 描述: 让问题不再发生

```bash
# 方法一：git 统一换行符（.gitattributes 加一行）
# *.sh text eol=lf

# 方法二：vim 里手动转换
# :set ff=unix 然后保存

# 方法三：CI/部署脚本里固定兜底一步
sed -i 's/\r$//' /opt/scripts/*.sh
```