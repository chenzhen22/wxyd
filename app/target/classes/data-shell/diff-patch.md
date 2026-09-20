---
name: Diff 与 Patch
category: 文件操作
order: 7
---

## 介绍

diff 对比两个文件的差异，patch 把差异应用到另一份文件上。配置变更前后对比、代码补丁分发、批量给多台服务器改同一个文件，都可以走「diff 出补丁 → patch 打补丁」的标准化流程，比手工改可靠得多。

核心特点：
- **逐行对比**：> 新文件行、< 旧文件行、c 改变/a 新增/d 删除
- **-u 统一格式**：带上下文与文件名，patch 的标准输入格式
- **支持目录递归**：-r 对比整个目录树，常用于发布包比对
- **可回滚**：patch -R 反向应用即可撤销补丁

## 语法

### 基本对比

```bash
diff old.conf new.conf
```
- 描述: 默认正常格式；4a5 表示旧文件第 4 行后加新文件第 5 行

### 统一格式

```bash
diff -u old.conf new.conf
```
- 描述: -u 带 3 行上下文与 ---/+++ 文件头，最常用

### 生成补丁文件

```bash
diff -u old.conf new.conf > change.patch
```
- 描述: 补丁重定向保存，可分发到其他机器

### 应用补丁

```bash
patch old.conf < change.patch
```
- 描述: 把补丁打到目标文件；-b 自动生成 .orig 备份

### 目录级对比

```bash
diff -ru old_dir new_dir > release.patch
```
- 描述: -r 递归对比目录，生成整个目录树的补丁

### 回滚补丁

```bash
patch -R old.conf < change.patch
```
- 描述: -R 反向应用，撤销已打的补丁

### 忽略空白差异

```bash
diff -u -w -B old.conf new.conf
```
- 描述: -w 忽略空白、-B 忽略空行变化，减少无意义差异

## 示例

### 配置变更标准化下发

- 描述: 一台改好，全部机器同款补丁

```bash
# 1. 在测试机改好配置，与原版生成补丁
diff -u /opt/app/conf/server.properties.orig /opt/app/conf/server.properties > jvm.patch

# 2. 分发到生产各节点应用
for h in prod1 prod2 prod3; do
    scp jvm.patch $h:/tmp/
    ssh $h "patch -b /opt/app/conf/server.properties < /tmp/jvm.patch"
done

# 3. 出问题整体回滚
ssh prod1 "patch -R -b /opt/app/conf/server.properties < /tmp/jvm.patch"
```

### 发布包比对

- 描述: 升级前确认到底改了哪些文件

```bash
# 对比两个版本解压目录
diff -rq app-v1 app-v2
# 只列出差异文件清单（-q 不显示具体内容）

# 生成完整补丁包供评审
diff -ru app-v1 app-v2 > release-review.patch
```

### 差异可视化与过滤

- 描述: 只看关心的差异

```bash
# 忽略空白与空行的干净对比
diff -u -w -B conf.properties.orig conf.properties

# 统计改动行数（评估影响面）
diff -u old new | grep -c "^+[^+]"

# 并排对比更直观
diff -y old.conf new.conf
```