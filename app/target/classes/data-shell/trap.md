---
name: Trap 信号捕获
category: Shell 进阶
order: 4
---

## 介绍

trap 在脚本收到指定信号时执行预置动作，最典型的用途是「优雅退出」：脚本被 Ctrl+C 或 kill 中断时，先删临时文件、杀子进程、恢复现场再退出，不留垃圾和僵尸进程。长任务脚本都该有 trap。

核心特点：
- **信号三剑客**：SIGINT(2) Ctrl+C、SIGTERM(15) kill 默认、EXIT(0) 脚本退出必触发
- **EXIT 万能收尾**：不管正常结束还是异常退出都触发，清理逻辑挂这里最稳
- **调试定位**：trap 'echo 行号 $LINENO' ERR 出错时报行号
- **临时文件防泄漏**：mktemp + trap rm 组合是标准姿势

## 语法

### 退出清理

```bash
trap 'cleanup' EXIT
```
- 描述: EXIT 信号（可写 0）在脚本任何方式结束时触发，最适合做统一清理

### 捕获 Ctrl+C

```bash
trap 'echo "收到中断，正在清理..."; exit 1' SIGINT SIGTERM
```
- 描述: 覆盖默认行为（直接死），先清理再退出

### 调试打印行号

```bash
trap 'echo "错误在第 $LINENO 行" >&2' ERR
```
- 描述: 任何命令失败触发，配合 set -e 使用，秒定位出错行

### 忽略信号

```bash
trap '' SIGINT
```
- 描述: 空动作=忽略该信号；恢复默认用 trap - SIGINT

### 临时文件标准姿势

```bash
tmp=$(mktemp)
trap 'rm -f "$tmp"' EXIT
```
- 描述: mktemp 建唯一临时文件，trap 保证无论如何退出都删除

### 看信号清单

```bash
trap -l
kill -l
```
- 描述: 列出全部信号名称与编号，常用的记住 2/9/15 即可

## 示例

### 长任务优雅退出

- 描述: 批量处理随时可安全中断

```bash
#!/bin/bash
# 批量处理脚本：Ctrl+C 时记录进度并清理
set -euo pipefail

PROGRESS=/tmp/batch.progress
trap '
    echo "任务中断，进度已保存到 $PROGRESS"
    exit 130
' SIGINT SIGTERM

for i in $(seq 1 1000); do
    echo "$i" >> "$PROGRESS"
    process_item "$i"    # 假设的处理函数
done
echo "全部完成"
# 中断后重跑时从 $PROGRESS 读最后一条继续
```

### 临时文件零泄漏

- 描述: mktemp + EXIT 的组合拳

```bash
#!/bin/bash
set -euo pipefail
tmp=$(mktemp /tmp/report.XXXXXX)
trap 'rm -f "$tmp"' EXIT

generate_report > "$tmp"
if grep -q "CRITICAL" "$tmp"; then
    mail -s "告警" ops@example.com < "$tmp"
fi
# 无论走到哪退出，$tmp 都会被删掉
```

### 子进程连带清理

- 描述: 杀掉脚本时别留孤儿进程

```bash
#!/bin/bash
# 启动后台任务，脚本退出时一并带走
./long-runner.sh &
runner_pid=$!
trap 'kill "$runner_pid" 2>/dev/null; echo "子任务已终止"; exit' SIGINT SIGTERM EXIT

wait "$runner_pid"
echo "正常完成"
```