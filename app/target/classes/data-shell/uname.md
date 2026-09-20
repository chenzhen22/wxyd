---
name: Uname 系统信息
category: 系统运维
order: 14
---

## 介绍

uname 打印内核与系统的基本信息。它回答三个问题：**这是什么系统、内核多新、什么架构** —— 跨平台脚本做兼容分支时，它几乎总是第一个被调用的命令。

要特别注意它**只管内核，不管发行版**：`uname -a` 只会告诉你 `Linux`，想知道是 Ubuntu 22.04 还是 CentOS 7 得去看 `/etc/os-release`。这是上手时最容易踩的坑。

核心特点：
- **-a 一把梭**：一次输出全部字段，但格式随平台变化，适合人看、不适合脚本解析
- **-r 内核版本**：判断是否支持某内核特性（cgroup v2、eBPF 等）时最常用
- **-m 架构最可靠**：x86_64 / aarch64，决定该下哪个架构的二进制包
- **-p 和 -i 基本没用**：多数 Linux 上直接返回 unknown，别拿它判断 CPU
- **不给发行版信息**：发行版必须走 `/etc/os-release` 或 `hostnamectl`

## 语法

### 全部信息

```bash
uname -a
```
- 描述: 一次性输出内核名/主机名/内核版本/架构/操作系统；格式随平台不同，适合人看，不建议脚本解析

### 内核版本

```bash
uname -r
```
- 描述: 只输出内核 release，如 `5.15.0-91-generic`；判断内核特性支持与否就看它

### 硬件架构

```bash
uname -m
```
- 描述: 输出机器硬件架构，如 x86_64 / aarch64 / armv7l，比 -p 可靠得多

### 组合字段

```bash
uname -snrvm
```
- 描述: 按「内核名 主机名 内核版本 架构」组合输出，采集系统指纹时最实用的一组

### 逐项查看

```bash
uname -s -n -v -o
```
- 描述: -s 内核名(Linux)、-n 主机名、-v 内核构建版本、-o 操作系统(GNU/Linux)

### 发行版信息（uname 给不了）

```bash
cat /etc/os-release
```
- 描述: uname 只讲内核不讲发行版；要 Ubuntu/CentOS/Debian 这类信息必须走这里，`hostnamectl` 也行

### 位数与平台

```bash
getconf LONG_BIT
```
- 描述: -m 给架构名、getconf LONG_BIT 给 32/64 位数；-p / -i 在多数 Linux 上是 unknown，不要用

## 示例

### 读懂 uname -a

- 描述: 逐字段拆解一行输出

```bash
uname -a
# Linux web01 5.15.0-91-generic #101-Ubuntu SMP Tue Nov 14 13:30:08 UTC 2023 x86_64 x86_64 x86_64 GNU/Linux
#
#  -s 内核名       Linux
#  -n 主机名       web01
#  -r 内核 release 5.15.0-91-generic   <- 判断内核特性支持看这个
#  -v 内核构建版本 #101-Ubuntu SMP ...  <- 编译时间 + 厂商补丁标记
#  -m 硬件架构     x86_64              <- 决定下哪个架构的包
#  -p 处理器       x86_64（常为 unknown，不可靠）
#  -i 硬件平台     x86_64（常为 unknown，不可靠）
#  -o 操作系统     GNU/Linux
```

### 一条命令采集系统指纹

- 描述: 生产级脚本，输出可直接贴进工单或资产表

```bash
#!/usr/bin/env bash
set -euo pipefail

# uname 只给内核信息，发行版要另外取；两者拼起来才是完整系统指纹
os_pretty="unknown"
if [[ -r /etc/os-release ]]; then
  # shellcheck disable=SC1091
  os_pretty="$(. /etc/os-release && echo "${PRETTY_NAME:-$NAME}")"
elif command -v sw_vers >/dev/null 2>&1; then
  os_pretty="macOS $(sw_vers -productVersion 2>/dev/null)"
fi

bits="$(getconf LONG_BIT 2>/dev/null || echo unknown)"

printf '%-14s %s\n' \
  "内核名"   "$(uname -s)" \
  "主机名"   "$(uname -n)" \
  "内核版本" "$(uname -r)" \
  "内核构建" "$(uname -v)" \
  "硬件架构" "$(uname -m)" \
  "发行版"   "$os_pretty" \
  "位数"     "$bits"
```

### 判断发行版（uname 不够用）

- 描述: 脚本里按发行版分支的正确写法

```bash
# 错误：uname -a 只说 Linux，里面没有发行版名字，基本不可能命中
uname -a | grep -iq ubuntu && echo "ubuntu"

# 正确：读 /etc/os-release（systemd 系发行版通用）
. /etc/os-release
echo "ID=$ID  VERSION_ID=$VERSION_ID  PRETTY=$PRETTY_NAME"

case "$ID" in
  ubuntu|debian)                       PKG=apt ;;
  centos|rhel|rocky|almalinux|fedora)  PKG=yum ;;
  opensuse*)                           PKG=zypper ;;
  alpine)                              PKG=apk ;;
  *)                                   PKG=unknown ;;
esac
echo "包管理器: $PKG"
```

### 内核版本判断与特性检查

- 描述: 用 -r 做版本比较，避免在不支持的内核上开特性

```bash
# 取主次版本号（去掉 -generic / -aws 之类的后缀）
krel="$(uname -r)"                        # 5.15.0-91-generic
knum="${krel%%-*}"                        # 5.15.0
major="${knum%%.*}"                       # 5
minor="${knum#*.}"; minor="${minor%%.*}"  # 15

# 版本比较统一用 sort -V；字符串比较是错的（"5.9" > "5.15"）
if [[ "$(printf '%s\n%s\n' "4.18" "$knum" | sort -V | head -n1)" == "4.18" ]]; then
  echo "内核 $knum >= 4.18，可用 cgroup v2 / eBPF 等基础特性"
else
  echo "内核 $knum 偏旧，注意特性兼容"
fi

# 顺便确定架构标签，决定下载哪种二进制
case "$(uname -m)" in
  x86_64)  ARCH=amd64 ;;
  aarch64) ARCH=arm64 ;;
  armv7l)  ARCH=arm   ;;
  *)       ARCH="$(uname -m)" ;;
esac
echo "架构标签: $ARCH"
```

### 跨平台兼容脚本

- 描述: Linux 与 macOS 的 uname 选项有差异，写兼容脚本必须分支

```bash
case "$(uname -s)" in
  Linux)
    echo "Linux 内核 $(uname -r)"
    sed -i 's/a/b/' file            # GNU sed
    ;;
  Darwin)
    # macOS：不支持 -o；sed -i 必须跟一个空参数；readlink 需 -f
    echo "macOS $(sw_vers -productVersion 2>/dev/null)"
    sed -i '' 's/a/b/' file         # BSD sed
    ;;
  MINGW*|MSYS*|CYGWIN*)
    # Windows 上的 Git Bash：uname -s 形如 MINGW64_NT-10.0
    echo "Windows + Git Bash"
    ;;
  *)
    echo "未识别的内核: $(uname -s)"
    ;;
esac
```

### 容器与虚拟化里的坑

- 描述: 容器内 uname 返回的是宿主机内核，不是镜像的

```bash
# 容器与宿主机共享内核，uname -r 显示的是宿主机内核版本
uname -r                    # 如 5.15.0-91-generic，来自宿主机

# 判断「自己在不在容器里」不能用 uname，要用这些：
[[ -f /.dockerenv ]] && echo "在 Docker 容器内"
grep -qa 'docker\|kubepods\|containerd' /proc/1/cgroup && echo "在容器内"
cat /proc/1/comm            # 容器里常是 sh / tini，宿主机是 systemd

# 镜像自身的发行版信息依然可以正常拿到
cat /etc/os-release
```
