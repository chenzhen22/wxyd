---
name: 用户账号管理
category: 系统运维
order: 12
---

## 介绍

用户账号管理是运维最日常也最容易出事的操作：建号漏了 `-m` 导致家目录不存在、改密用了 `--stdin` 结果在 Ubuntu 上报错、给权限时 `usermod -G` 少了 `a` 把用户踢出了原有组。本节把「创建用户 → 设置密码 → 修改密码 → 授予权限」四件事脚本化，每条命令都标注了发行版差异和幂等写法，可直接改造为批量运维脚本。

核心特点：
- **建号要成套**：`useradd` 只是建了条记录，家目录、shell、初始密码、组归属要一次配齐
- **设密有两条路**：交互式 `passwd` 适合人工操作，`chpasswd` 和 `--stdin` 适合脚本批处理
- **改密可用规则批量做**：`chage -M 90` 强制口令周期，`chage -d 0` 强制下次登录改密
- **授权分两层**：`usermod -aG` 给附加组（粗粒度），`sudoers` 给具体命令（细粒度）

## 语法

### 创建用户

```bash
useradd -m -s /bin/bash -c "运维账号" zhangsan
useradd -r -m -s /sbin/nologin appuser
useradd -m -d /opt/apphome -s /bin/bash -u 1500 -G wheel,docker tom
```
- 描述: -m 建家目录（不加就只有空账号）、-s 指定登录 shell、-r 建系统账号、-d 自定义家目录、-u 指定 UID、-G 同时加入附加组

### 检查用户是否存在

```bash
id zhangsan >/dev/null 2>&1 && echo "已存在" || useradd -m -s /bin/bash zhangsan
getent passwd zhangsan
```
- 描述: 脚本里建号前必须先判存（幂等关键）；id 判存最轻量，getent 可跨 NIS/LDAP 查询

### 设置初始密码

```bash
passwd zhangsan
passwd -e zhangsan
```
- 描述: 交互式输入两次；-e 立即把密码置为过期，用户下次登录强制改密（交付密码给同事的标准做法）

### 脚本批量设密

```bash
echo "zhangsan:Passw0rd@2026" | chpasswd
printf '%s:%s\n' "$USER_NAME" "$PASS" | chpasswd
echo "Passw0rd@2026" | passwd --stdin zhangsan
```
- 描述: chpasswd 从标准输入读「用户:密码」，全发行版通用，**脚本首选**；passwd --stdin 仅 RHEL/CentOS 系支持，Debian/Ubuntu 会报错

### 加密方式设密

```bash
chpasswd -c SHA512
chpasswd -e
```
- 描述: -c 指定加密算法（默认看 /etc/login.defs）；-e 表示输入已是密文，用于批量迁移 passwd 记录

### 修改已有密码

```bash
passwd zhangsan
echo "zhangsan:NewPass@2026" | chpasswd
passwd -l zhangsan
passwd -u zhangsan
```
- 描述: 改密与设密同命令；-l 锁定账号（口令前加 ! ），-u 解锁，比 userdel 温和，保留数据只停登录

### 密码有效期策略

```bash
chage -l zhangsan
chage -M 90 -m 7 -W 7 zhangsan
chage -d 0 zhangsan
chage -E 2026-12-31 zhangsan
```
- 描述: -l 查看现有策略；-M 最长有效天数（90 天强制改密）、-m 最短间隔（防改回去）、-W 提前告警天数；-d 0 置为零表示下次登录必改；-E 账号到期日

### 修改用户属性与组

```bash
usermod -aG docker,ops zhangsan
usermod -g ops zhangsan
usermod -s /sbin/nologin zhangsan
usermod -L zhangsan
```
- 描述: -aG 追加附加组（**漏掉 a 会覆盖，把用户踢出所有原组**）、-g 改主组、-s 改 shell、-L/-U 锁定解锁

### 授予 sudo 权限

```bash
usermod -aG wheel zhangsan
usermod -aG sudo zhangsan
```
- 描述: wheel 组是 RHEL/CentOS 的 sudo 组，Ubuntu 用 sudo 组，加入即获全部 sudo 权限——生产环境慎用

### 细粒度 sudo 授权

```bash
visudo -f /etc/sudoers.d/ops
```
- 描述: 在 /etc/sudoers.d/ 下建独立规则文件比改主文件更好维护；必须用 visudo 编辑以校验语法，写错会导致所有人都无法 sudo

### 删除用户

```bash
userdel zhangsan
userdel -r zhangsan
usermod -L -s /sbin/nologin zhangsan
```
- 描述: 不带 -r 只删账号留文件（可恢复）；-r 连家目录邮件一起删（**不可逆**）；离职场景推荐先锁定观察再删

## 示例

### 创建应用专用账号

- 描述: 完整可执行脚本，幂等安全，可重复运行

```bash
#!/bin/bash
# 用途：创建应用专用账号并配置目录归属
set -euo pipefail

USER_NAME="appuser"
APP_HOME="/opt/app"
GROUP_NAME="appgroup"

echo "==> 1. 检查并创建用户组"
if ! getent group "$GROUP_NAME" >/dev/null; then
    groupadd "$GROUP_NAME"
    echo "    组 $GROUP_NAME 创建完成"
else
    echo "    组 $GROUP_NAME 已存在，跳过"
fi

echo "==> 2. 检查并创建用户"
if id "$USER_NAME" >/dev/null 2>&1; then
    echo "    用户 $USER_NAME 已存在，跳过创建"
else
    # -r 系统账号，-m 建家目录，-s 禁登录，-g 指定主组
    useradd -r -m -s /sbin/nologin -g "$GROUP_NAME" "$USER_NAME"
    echo "    用户 $USER_NAME 创建完成"
fi

echo "==> 3. 配置目录归属"
mkdir -p "$APP_HOME/logs"
chown -R "$USER_NAME:$GROUP_NAME" "$APP_HOME"
chmod 755 "$APP_HOME"
chmod 750 "$APP_HOME/logs"

echo "==> 4. 结果确认"
id "$USER_NAME"
ls -ld "$APP_HOME" "$APP_HOME/logs"
echo "==> 完成，未设置密码（系统账号不可登录，无需密码）"
```

### 交互式建用户脚本

- 描述: 带参数校验和密码强度检查，适合交付给运维同事日常使用

```bash
#!/bin/bash
# 用途：交互式创建普通用户，含用户名与密码强度校验
set -euo pipefail

read -r -p "请输入用户名: " USER_NAME
read -r -p "请输入备注(可选): " COMMENT || true

# 1. 用户名合法性校验：小写字母开头，只含小写字母数字下划线横线
if ! [[ "$USER_NAME" =~ ^[a-z_][a-z0-9_-]{0,31}$ ]]; then
    echo "[ERROR] 用户名不合法：需小写字母或下划线开头，长度 1-32" >&2
    exit 1
fi

# 2. 重名检查
if id "$USER_NAME" >/dev/null 2>&1; then
    echo "[ERROR] 用户 $USER_NAME 已存在" >&2
    exit 1
fi

# 3. 创建用户
useradd -m -s /bin/bash -c "${COMMENT:-created by script}" "$USER_NAME"
echo "[OK] 用户 $USER_NAME 创建完成，家目录 /home/$USER_NAME"

# 4. 循环设置密码，带强度校验，-s 隐藏输入
while true; do
    read -r -s -p "请输入密码: " PASS1; echo
    read -r -s -p "请再输一遍: " PASS2; echo

    if [ "$PASS1" != "$PASS2" ]; then
        echo "[WARN] 两次输入不一致，请重试"; continue
    fi
    if [ ${#PASS1} -lt 12 ]; then
        echo "[WARN] 密码长度不足 12 位，请重试"; continue
    fi
    if ! [[ "$PASS1" =~ [a-z] && "$PASS1" =~ [A-Z] && "$PASS1" =~ [0-9] ]]; then
        echo "[WARN] 密码需同时包含大小写字母和数字，请重试"; continue
    fi
    break
done

# 5. 非交互设密并强制首次登录修改
printf '%s:%s\n' "$USER_NAME" "$PASS1" | chpasswd
chage -d 0 "$USER_NAME"

echo "[OK] 密码设置完成，该用户首次登录时将被强制修改密码"
unset PASS1 PASS2
id "$USER_NAME"
```

### 批量创建用户

- 描述: 从清单文件批量建号，一次设密，适合项目组开号

```bash
#!/bin/bash
# 用途：批量创建用户并设置初始密码
set -euo pipefail

USER_FILE="/opt/scripts/userlist.txt"
LOG_FILE="/var/log/user-create-$(date +%Y%m%d).log"
INIT_PASS="Init@2026#Change"      # 统一初始密码，配合 chage -d 0 强制改
DEFAULT_GROUP="project"

# 清单格式：每行一个用户名，支持 # 注释
: > "$LOG_FILE"

getent group "$DEFAULT_GROUP" >/dev/null || groupadd "$DEFAULT_GROUP"

while read -r uname; do
    # 跳过空行和注释行
    uname=$(echo "$uname" | tr -d '[:space:]')
    [[ -z "$uname" || "$uname" == \#* ]] && continue

    if id "$uname" >/dev/null 2>&1; then
        echo "[SKIP] $uname 已存在" | tee -a "$LOG_FILE"
        continue
    fi

    if useradd -m -s /bin/bash -g "$DEFAULT_GROUP" "$uname" >> "$LOG_FILE" 2>&1; then
        echo "$uname:$INIT_PASS" | chpasswd
        chage -d 0 -M 90 -W 7 "$uname"
        echo "[OK]   $uname 创建成功" | tee -a "$LOG_FILE"
    else
        echo "[FAIL] $uname 创建失败，详见 $LOG_FILE" | tee -a "$LOG_FILE"
    fi
done < "$USER_FILE"

echo "==> 批量创建完成，日志: $LOG_FILE"
echo "==> 成功: $(grep -c '\[OK\]' "$LOG_FILE") 个"
```

### 批量重置密码

- 描述: 定期改密或人员变动时批量重置，随机密码并落盘留档

```bash
#!/bin/bash
# 用途：批量重置用户密码为随机强密码，结果写入加密文件
set -euo pipefail

USER_LIST=("zhangsan" "lisi" "wangwu")
RESULT_FILE="/root/passwd-reset-$(date +%Y%m%d).txt"
PASS_LEN=16

# 生成随机密码：去除易混淆字符 O0Il1
gen_pass() {
    tr -dc 'A-Za-z0-9!@#%^&*' < /dev/urandom | tr -d 'O0Il1' | head -c "$PASS_LEN"
}

umask 077          # 确保新文件仅属主可读
: > "$RESULT_FILE"

for uname in "${USER_LIST[@]}"; do
    if ! id "$uname" >/dev/null 2>&1; then
        echo "[SKIP] $uname 不存在" >&2
        continue
    fi

    NEW_PASS=$(gen_pass)
    # chpasswd 全发行版通用，不用 passwd --stdin
    printf '%s:%s\n' "$uname" "$NEW_PASS" | chpasswd
    # -d 0 让用户下次登录必须自己再改一次
    chage -d 0 "$uname"

    printf '%s\t%s\n' "$uname" "$NEW_PASS" >> "$RESULT_FILE"
    echo "[OK] $uname 密码已重置"
done

chmod 600 "$RESULT_FILE"
echo "==> 新密码已写入 $RESULT_FILE（权限 600，请通过安全渠道分发后及时删除）"
```

### 修改用户权限

- 描述: 按岗位授权，附加组给粗粒度、sudoers 给细粒度

```bash
#!/bin/bash
# 用途：为运维人员授予最小化权限（附加组 + 指定命令 sudo）
set -euo pipefail

USER_NAME="${1:-}"
if [ -z "$USER_NAME" ]; then
    echo "用法: $0 <用户名>" >&2
    exit 1
fi

id "$USER_NAME" >/dev/null 2>&1 || { echo "[ERROR] 用户 $USER_NAME 不存在" >&2; exit 1; }

echo "==> 1. 创建岗位组"
for g in ops deploy; do
    getent group "$g" >/dev/null || groupadd "$g"
done

echo "==> 2. 追加到附加组（-aG 保留原有组，切勿漏掉 a）"
usermod -aG ops "$USER_NAME"
usermod -aG deploy "$USER_NAME"

echo "==> 3. 写入细粒度 sudo 规则"
SUDO_FILE="/etc/sudoers.d/ops-${USER_NAME}"
cat > "$SUDO_FILE" <<EOF
# 由脚本自动生成，授权范围：应用重启与日志查看
${USER_NAME} ALL=(root) NOPASSWD: /bin/systemctl restart wxyd, /bin/systemctl status wxyd
${USER_NAME} ALL=(root) NOPASSWD: /bin/journalctl -u wxyd*
EOF

echo "==> 4. 校验 sudo 语法（必须，写错会导致 sudo 全局不可用）"
if visudo -cf "$SUDO_FILE"; then
    chmod 440 "$SUDO_FILE"
    echo "    语法校验通过，权限已生效"
else
    rm -f "$SUDO_FILE"
    echo "[ERROR] sudo 语法校验失败，已回滚删除 $SUDO_FILE" >&2
    exit 1
fi

echo "==> 5. 结果确认"
id "$USER_NAME"
sudo -l -U "$USER_NAME"
```

### 撤销权限与锁定账号

- 描述: 人员离岗或安全事件时的收敛操作

```bash
#!/bin/bash
# 用途：撤销用户权限并锁定账号（保留数据，可随时恢复）
set -euo pipefail

USER_NAME="${1:-}"
id "$USER_NAME" >/dev/null 2>&1 || { echo "[ERROR] 用户不存在" >&2; exit 1; }

echo "==> 1. 锁定密码（禁止登录）"
passwd -l "$USER_NAME"
usermod -L "$USER_NAME"

echo "==> 2. 改为不可登录 shell"
usermod -s /sbin/nologin "$USER_NAME"

echo "==> 3. 从所有附加组移除"
# 先列出当前附加组，再逐个移除
for g in $(id -nG "$USER_NAME" | tr ' ' '\n' | grep -v "^$(id -gn "$USER_NAME")$"); do
    gpasswd -d "$USER_NAME" "$g" && echo "    已移出组 $g"
done

echo "==> 4. 撤销 sudo 规则"
rm -f "/etc/sudoers.d/ops-${USER_NAME}"
visudo -c >/dev/null && echo "    sudo 语法复核通过"

echo "==> 5. 踢掉在线会话（如有）"
pkill -KILL -u "$USER_NAME" 2>/dev/null && echo "    已终止该用户进程" || echo "    无在线会话"

echo "==> 6. 最终状态"
passwd -S "$USER_NAME"
id "$USER_NAME"
echo "==> 账号已锁定。确认无需保留后可执行：userdel -r $USER_NAME"
```
