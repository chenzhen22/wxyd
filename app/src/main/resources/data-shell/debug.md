---
name: 脚本调试技巧
category: Shell 进阶
order: 2
---

## 介绍

Shell 脚本没有断点调试器，主要靠「执行追踪 + 严格模式 + 陷阱捕获」三板斗。掌握 bash -x 逐行追踪、set -e/-u/-o pipefail 让脚本遇错即停、trap 定位报错行号，绝大多数脚本问题都能快速定位。

核心特点：
- **-x 追踪**：展开每条命令与变量实际值，看得见脚本在干什么
- **set -e**：任何命令失败立即退出，避免带病继续跑
- **set -u**：使用未定义变量直接报错，防手误
- **pipefail**：管道中任一环失败即整条失败，防止错误被吞
- **trap ERR**：出错时自动打印行号，秒定位问题行

## 语法

### 逐行追踪

```bash
bash -x script.sh
```
- 描述: 每条命令前输出 + 开头的展开结果，变量显示实际值

### 局部追踪

```bash
set -x
command_to_debug
set +x
```
- 描述: 只追踪某一段，set -x 开、set +x 关

### 严格模式三件套

```bash
set -euo pipefail
```
- 描述: -e 出错即停、-u 未定义变量报错、-o pipefail 管道任一环失败即失败

### ERR 陷阱打印行号

```bash
trap 'echo "错误发生在第 $LINENO 行" >&2' ERR
```
- 描述: 任何命令返回非 0 时触发，自动报告出错行号

### 打印调试信息

```bash
echo "DEBUG: var=$var" >&2
```
- 描述: 调试信息走 stderr，与正常输出分离，上线时统一关闭

### 检查语法

```bash
bash -n script.sh
```
- 描述: 只做语法检查不执行，发布前快速过一遍

## 示例

### 一次完整调试

- 描述: 用 -x 看清变量展开过程

```bash
# 调试 script.sh
bash -x script.sh
# 输出示例:
# + name=world
# + echo Hello, world
# Hello, world
# + 号后面是实际执行的命令，变量已展开，一眼看出问题
```

### 生产脚本的健壮性模板

- 描述: 推荐的脚本骨架，出错即停且能定位

```bash
#!/bin/bash
set -euo pipefail
trap 'echo "[$(date +%T)] 脚本在第 $LINENO 行失败退出" >&2' ERR

LOG=/var/log/myjob.log
exec >> "$LOG" 2>&1   # 后续所有输出自动进日志

echo "任务开始 $(date +%F' '%T)"
# ... 业务命令 ...
echo "任务完成"
```

### 变量为空引发的惨案

- 描述: set -u 提前拦截手误

```bash
# 未加 -u 时：rm -rf "$TMP_DIR/" 空变量会变成 rm -rf / （灾难）
# 加 -u 后：使用未定义变量立即报错退出
set -u
backup_dir=$BACKUP_DIR   # 若 BACKUP_DIR 未定义，立刻报错，不会带病执行
echo "$backup_dir"
```