---
name: Dmidecode 硬件信息
category: 系统运维
order: 13
---

## 介绍

dmidecode 读取 SMBIOS/DMI 表并把原始二进制解析成可读的硬件信息：主板型号、BIOS 版本、CPU、内存插槽与序列号、机箱、电源、TPM 等。它是**不看机房、不拆机箱**就能拿到硬件资产清单的首选工具，也是排查「内存条是否插满、是否降频、是否有一根坏了」的关键手段。

核心特点：
- **需要 root**：读的是 `/dev/mem`，普通用户会直接报 Permission denied
- **数据来自主板固件**：厂商写什么就显示什么，可能存在占位值，脚本解析要过滤
- **按 DMI 类型取值**：`-t` 指定类型编号，比 grep 文本更准
- **`-s` 取单个字段**：脚本里取序列号、UUID 最方便
- **虚拟机里信息不可信**：会显示 QEMU/SeaBIOS 之类的虚拟化标识

## 语法

### 基本查看

```bash
sudo dmidecode
```
- 描述: 输出全部 DMI 表内容，通常几百行，日常建议配合 `less` 或 `-t` 使用

### 按类型查看

```bash
sudo dmidecode -t system
sudo dmidecode -t 1
sudo dmidecode -t bios -t baseboard -t system
```
- 描述: `-t` 后接类型名或编号，可叠加多个；1=System、0=BIOS、2=Baseboard、3=Chassis、4=Processor、16=物理内存阵列、17=内存条

### 常用类型速查

```bash
sudo dmidecode -t 0,1,2,3,4,16,17
```
- 描述: 用逗号一次取多类，覆盖 BIOS/主机/主板/机箱/CPU/内存，是「报硬件配置」的最简命令

### 取单个字段

```bash
sudo dmidecode -s system-serial-number
sudo dmidecode -s system-product-name
sudo dmidecode -s bios-version
```
- 描述: `-s` 只输出指定字符串，适合脚本赋值；不带参数则列出全部可用关键字

### 内存与 CPU 专项

```bash
sudo dmidecode -t memory
sudo dmidecode -t processor
```
- 描述: `-t memory` 含物理内存阵列与每根内存条；`-t processor` 含插槽、核心数、主频、缓存

### 安静模式

```bash
sudo dmidecode -q
```
- 描述: 省去 Handle 标题行与结尾提示，输出更紧凑，适合管道处理

### 导出与离线解析原始表

```bash
sudo dmidecode --dump-bin dmi.bin
sudo dmidecode --from-dump dmi.bin
```
- 描述: `--dump-bin` 把原始 DMI 表导出成文件，`--from-dump` 再离线解析；换机排障时不必在目标机装 dmidecode，也便于前后比对硬件变更

## 示例

### 读懂输出结构

- 描述: 一条 dmidecode 结果里的关键字段含义

```bash
sudo dmidecode -t 1
# Handle 0x0001, DMI type 1, 27 bytes        <- 句柄 / 类型 / 表长度
# System Information
#         Manufacturer: Dell Inc.            <- 整机厂商
#         Product Name: PowerEdge R740       <- 机型
#         Version: Not Specified             <- 占位值，无意义
#         Serial Number: 1A2B3C4               <- 整机序列号（报修/盘点用）
#         UUID: 4c4c4544-...                 <- 全网唯一，虚拟化授权常看它
#         Wake-up Type: Power Switch
```

### 采集服务器硬件清单

- 描述: 一条命令拿到报修和盘点要的全部信息

```bash
# 机型 / 序列号 / UUID / BIOS
for s in system-manufacturer system-product-name system-serial-number system-uuid bios-version bios-release-date; do
  printf '%-24s %s\n' "$s" "$(sudo dmidecode -s "$s" 2>/dev/null)"
done

# 主板与机箱
sudo dmidecode -t 2,3 | grep -E 'Manufacturer|Product Name|Version|Serial Number'
```

### 内存插槽盘点

- 描述: 统计插了几根、空几个槽、频率是否一致

```bash
# -t 17 是「内存条」逐条信息；-t 16 是「物理内存阵列」总量与槽位数
sudo dmidecode -t 17 | grep -E '^\s+(Size|Type|Speed|Manufacturer|Serial Number|Locator):'

# 已插条数 / 总容量 / 空槽数
sudo dmidecode -t 17 | awk '
  /^\s+Size:/        { if ($2 == "No") empty++; else { used++; size += $2 * ($3=="GB"?1024:1) } }
  END { printf "已插 %d 根，空槽 %d 个，合计约 %.0f GB\n", used, empty, size/1024 }
'
```

### 批量硬件资产盘点脚本

- 描述: 生产级脚本，收集本机硬件信息并落盘留档，字段缺失自动降级为 unknown

```bash
#!/usr/bin/env bash
set -euo pipefail

if [[ $EUID -ne 0 ]]; then
  echo "需要 root 权限" >&2; exit 1
fi

# dmidecode 在部分 ARM / 容器 / 云主机上不存在或无 DMI 表
if ! command -v dmidecode >/dev/null 2>&1; then
  echo "未安装 dmidecode，请先安装：yum install -y dmidecode 或 apt install -y dmidecode" >&2
  exit 1
fi

OUT="${1:-/var/log/hardware-$(hostname)-$(date +%F).txt}"
umask 077

# 取单个字段：查不到或为占位值时统一回退成 unknown
getval() {
  local v
  v="$(dmidecode -s "$1" 2>/dev/null | tr -d '\r' | head -n1)" || true
  case "$v" in
    ""|"Not Specified"|"To Be Filled By O.E.M."|"Unknown"|"None") echo "unknown" ;;
    *) echo "$v" ;;
  esac
}

{
  echo "===== 硬件资产清单 ====="
  echo "采集时间: $(date '+%F %T')"
  echo "主机名  : $(hostname)"
  echo "厂商    : $(getval system-manufacturer)"
  echo "机型    : $(getval system-product-name)"
  echo "序列号  : $(getval system-serial-number)"
  echo "UUID    : $(getval system-uuid)"
  echo "BIOS    : $(getval bios-version) ($(getval bios-release-date))"
  echo "主板    : $(getval baseboard-manufacturer) $(getval baseboard-product-name)"
  echo "CPU     : $(getval processor-version)"

  echo
  echo "----- 内存条 -----"
  dmidecode -t 17 | grep -E '^\s+(Size|Type|Speed|Locator):' || echo "无内存信息"

  echo
  echo "----- 物理磁盘 -----"
  lsblk -d -o NAME,SIZE,TYPE,MODEL 2>/dev/null || echo "lsblk 不可用"
} > "$OUT"

chmod 600 "$OUT"
echo "已写入 $OUT"
```

### 虚拟机上的坑

- 描述: 在 KVM/QEMU、云主机里 dmidecode 的典型输出

```bash
sudo dmidecode -s system-manufacturer   # 常见返回: QEMU / Bochs / Alibaba Cloud / Amazon EC2
sudo dmidecode -s system-product-name   # 常见返回: Standard PC (i440FX + PIIX, 1996) / KVM

# 典型现象：
#   1. 内存 Speed 显示 "Unknown" —— 虚拟内存没有物理频率概念
#   2. 内存 Manufacturer 为空 —— 虚机不模拟内存条厂商
#   3. Chassis / Power Supply 等类型整段缺失
# 结论：虚拟机上 dmidecode 只适合取 UUID 和机型，
#       容量类数据请改用 lscpu / free / lsblk 等系统接口
```

### 内存故障定位

- 描述: 内存告警或容量对不上时，用 dmidecode 核对物理配置

```bash
# 1. 系统认到的内存总量（对比物理配置判断是否有条未识别）
grep MemTotal /proc/meminfo
free -h

# 2. 物理上实际插了什么（含每根的容量/频率/位置）
sudo dmidecode -t 17 | grep -E '^\s+(Size|Speed|Configured Memory Speed|Locator|Bank Locator):'

# 3. 关注两种异常：
#    Size: No Module Installed         -> 空槽，正常
#    Configured Memory Speed < Speed   -> 降频，通常是混插不同频率的内存条
# 4. 若某槽容量为 0 或整个插槽消失，多为内存条未插紧或已损坏
```
