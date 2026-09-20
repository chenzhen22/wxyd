---
name: Shell 陷阱集锦
category: Shell 进阶
order: 6
---

## 介绍

本节汇总生产脚本里最常见、最致命的十个坑：每一个都来自真实事故。写完脚本对照过一遍，能挡住 90% 的翻车；出事故时按图索骥也能快速对号入座。

核心特点：
- **rm -rf 类**：变量为空、路径拼错是最惨烈事故的头号来源
- **引号类**：未加双引号导致分词/通配展开，空格路径秒变灾难
- **CRLF 类**：Windows 编辑带来的隐藏字符
- **管道与子 Shell**：管道内变量修改失效，容易误判逻辑错误

## 语法

### 坑 1：空变量导致 rm -rf /

```bash
# 错误：$DIR 为空时变成 rm -rf /xxx/* 甚至 rm -rf /
rm -rf "$DIR/old"
# 防御：set -u + 使用前校验
[ -n "${DIR:-}" ] && [ -d "$DIR" ] || { echo "DIR 未设置或不存在"; exit 1; }
```
- 描述: 永远在 rm 前验证变量非空且目录存在；脚本开头 set -u

### 坑 2：rm 变量带空格分词

```bash
# 错误：路径带空格时被拆成多个参数
rm -rf $TARGET_DIR
# 正确：双引号包裹
rm -rf "$TARGET_DIR"
```
- 描述: 所有含变量的路径都加双引号，养成肌肉记忆

### 坑 3：管道右侧变量丢失

```bash
# 错误：while 在子 Shell 里跑，count 始终为 0
count=0
cat list.txt | while read line; do count=$((count+1)); done
# 正确：重定向写法让 while 在当前 Shell
while read line; do count=$((count+1)); done < list.txt
```
- 描述: 管道右侧是新子进程，改不了父 Shell 变量；用 < 重定向替代

### 坑 4：CRLF 换行符

```bash
# Windows 编辑过的脚本报: $'\r': command not found
# 修复
sed -i 's/\r$//' script.sh    # 或 dos2unix script.sh
```
- 描述: 详见 Dos2Unix 主题；CI 流程里固定兜底一步最稳

### 坑 5：测试括号里的变量未加引号

```bash
# 错误：变量为空时 [ = "x" ] 语法错误
if [ $VAR = "x" ]; then
# 正确
if [ "$VAR" = "x" ]; then
```
- 描述: [ ] 里变量必加双引号；或改用 [[ ]]（Bash 专属，对空值更宽容）

### 坑 6：sh 与 bash 差异

```bash
# 错误：shebang 用 sh 却用了数组/[[ ]]
#!/bin/sh
arr=(a b c)        # dash 下报错
# 正确：明确用 bash
#!/bin/bash
```
- 描述: Debian/Ubuntu 的 sh 是 dash，功能阉割版；用 bash 特性就写 /bin/bash

### 坑 7：read 丢失行内容

```bash
# 错误：行首尾空格丢失、反斜杠被吃
while read line; do echo "$line"; done < file
# 正确：关字段分割、不转义
while IFS= read -r line; do echo "$line"; done < file
```
- 描述: IFS= 保留空白，-r 保留反斜杠，read 处理文件的固定搭配

### 坑 8：定时任务环境差异

```bash
# cron 里 PATH 只有 /usr/bin:/bin，java/自定义命令找不到
# 正确：crontab 里用绝对路径或顶部补 PATH
*/5 * * * * /usr/local/jdk/bin/java -jar /opt/app/app.jar
```
- 描述: cron 环境极简，脚本里所有命令用绝对路径最保险

### 坑 9：后台任务随终端关闭死亡

```bash
# 错误：关 SSH 终端任务被 SIGHUP 杀死
java -jar app.jar &
# 正确
nohup java -jar app.jar > app.out 2>&1 &
```
- 描述: nohup 免挂断 + 输出重定向；正式环境用 systemd 更好

### 坑 10：错误被管道吞掉

```bash
# 错误：curl 失败了但脚本继续跑
curl -s URL | tar -xz
# 正确：pipefail 让整条管道感知失败
set -o pipefail
curl -s URL | tar -xz
```
- 描述: 默认管道只看最后一环的退出码，-o pipefail 修复这一盲区

## 示例

### 生产脚本安全模板

- 描述: 集防御于一身的开头

```bash
#!/bin/bash
set -euo pipefail                          # 坑 5/6/10 一并防住
IFS=$'\n\t'                                # 收紧分词
trap 'echo "失败于第 $LINENO 行" >&2' ERR

# 所有路径变量集中定义 + 使用前校验（坑 1/2）
TARGET="${TARGET:-}"
[ -n "$TARGET" ] && [ -d "$TARGET" ] || { echo "TARGET 必须是存在的目录"; exit 1; }

# 清理动作白名单化，绝不拼接未校验变量
```

### 事故复盘对照表

- 描述: 出事后按症状找坑

```bash
# rm 删错目录        → 坑 1（空变量）或坑 2（分词）
# 脚本 sh 语法报错   → 坑 4（CRLF）或坑 6（shebang）
# 统计值不对         → 坑 3（管道子 Shell）
# cron 不执行        → 坑 8（环境差异），grep CRON /var/log/cron 查调度记录
# 关终端服务就死     → 坑 9（nohup/systemd）
```