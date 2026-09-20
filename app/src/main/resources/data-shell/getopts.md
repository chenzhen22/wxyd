---
name: Getopts 参数解析
category: Shell 进阶
order: 5
---

## 介绍

getopts 是 Shell 内置的命令行选项解析器，让自研脚本支持 -f file -v 这类标准选项风格。相比手工 shift 解析，它自动处理选项顺序、缺省值、合并选项（-vf）与错误提示，是写规范 CLI 工具的正路。

核心特点：
- **内建无依赖**：bash 自带，不装任何包
- **串定义**：":a:bc" 冒号规则——带参选项后跟冒号，首冒号=静默自管错误
- **OPTARG/OPTIND**：OPTARG 是当前选项参数，OPTIND 是处理进度游标
- **支持 -- 长选项前缀**：getopts 只管短选项，长选项需手工补（见示例）

## 语法

### 基本结构

```bash
while getopts ":f:vh" opt; do
    case $opt in
        f) file="$OPTARG" ;;
        v) verbose=1 ;;
        h) usage; exit 0 ;;
        \?) echo "未知选项: -$OPTARG"; usage; exit 1 ;;
        :) echo "选项 -$OPTARG 需要参数"; exit 1 ;;
    esac
done
shift $((OPTIND - 1))
```
- 描述: 标准模板；\?) 未知选项、:) 缺参数两个分支都要处理

### 串规则

```bash
getopts ":a:b:c" opt
```
- 描述: a: b: 表示需要参数值；c 不带冒号是开关；首冒号关闭系统默认报错，自管

### 剩余位置参数

```bash
shift $((OPTIND - 1))
echo "剩余参数: $@"
```
- 描述: 选项解析完后 shift 掉已消费部分，$@ 剩下的是纯位置参数

### usage 函数

```bash
usage() {
    cat <<EOF
用法: $(basename $0) [-f 配置文件] [-v] 目标目录
  -f  指定配置文件（默认 conf.yml）
  -v  输出详细日志
  -h  显示帮助
EOF
}
```
- 描述: heredoc 写帮助信息，规范脚本必备

### 长选项支持

```bash
case "$1" in
    --file) file="$2"; shift 2 ;;
    --verbose) verbose=1; shift ;;
    *) break ;;
esac
```
- 描述: getopts 不支持 --long，长选项用 case 前置处理或改用 GNU getopt

## 示例

### 完整参数化备份脚本

- 描述: 选项+位置参数+默认值的规范写法

```bash
#!/bin/bash
set -euo pipefail

usage() {
    echo "用法: $(basename $0) [-t 目录] [-k 保留天数] [-v] 源路径"
    echo "  -t  备份目标目录（默认 /data/backup）"
    echo "  -k  保留天数（默认 7）"
    echo "  -v  详细模式"
}

target="/data/backup"
keep=7
verbose=0

while getopts ":t:k:vh" opt; do
    case $opt in
        t) target="$OPTARG" ;;
        k) keep="$OPTARG" ;;
        v) verbose=1 ;;
        h) usage; exit 0 ;;
        :) echo "错误: -$OPTARG 需要参数"; usage; exit 1 ;;
        \?) echo "错误: 未知选项 -$OPTARG"; usage; exit 1 ;;
    esac
done
shift $((OPTIND - 1))

[ $# -lt 1 ] && { usage; exit 1; }
src="$1"

[ "$verbose" = 1 ] && echo "源: $src 目标: $target 保留: ${keep}天"
tar -czf "${target}/$(basename "$src")-$(date +%Y%m%d).tar.gz" "$src"
find "$target" -name "*.tar.gz" -mtime +"$keep" -delete
```

### 运行效果

- 描述: 各种调用方式都被正确解析

```bash
./backup.sh -t /bak -k 30 -v /opt/app    # 全参数
./backup.sh /opt/app                     # 用默认值
./backup.sh -k 14 /opt/app               # 部分参数 + 位置参数
./backup.sh -h                           # 显示帮助
./backup.sh -x                           # 报未知选项
./backup.sh -t                           # 报缺参数
```