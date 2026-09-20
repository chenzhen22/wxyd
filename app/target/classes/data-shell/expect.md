---
name: Expect 自动交互
category: Shell 进阶
order: 3
---

## 介绍

expect 专门处理交互式命令：ssh 密码、su 切换、ftp 登录这类需要人工回答的程序，用它按「期待什么 → 回答什么」的脚本自动应答。SSH 免密是首选方案，但改密码、老设备、一次性迁移等场景 expect 是不二之选。

核心特点：
- **spawn 启动**：spawn 拉起交互命令并接管其输入输出
- **expect/send 配对**：expect 等待匹配文本，send 发送应答
- **超时兜底**：set timeout 防止挂死，timeout 分支处理异常
- **eof 收尾**：interact 交还控制权（调试用），expect eof 等命令结束（脚本用）

## 语法

### 脚本结构

```bash
#!/usr/bin/expect
set timeout 10
spawn ssh user@host
expect "password:"
send "mypass\r"
expect eof
```
- 描述: 模板四件套：timeout、spawn、expect/send、eof；\r 是回车

### 通配与多分支

```bash
expect {
    "yes/no"  { send "yes\r"; exp_continue }
    "password:" { send "mypass\r" }
    timeout   { exit 1 }
}
```
- 描述: 首次连接会问 yes/no，exp_continue 继续等下一个模式；timeout 兜底

### 命令行参数

```bash
set host [lindex $argv 0]
set pass [lindex $argv 1]
```
- 描述: [lindex $argv n] 取第 n 个参数，脚本通用化必备

### 变量与拼接

```bash
set pass "secret123"
send -- "$pass\r"
```
- 描述: -- 防止以 - 开头的内容被当选项；expect 是 Tcl 语法，字符串双引号内可插变量

### 交互移交

```bash
interact
```
- 描述: 匹配完成后把控制权交还给人工，适合「自动登录后继续手动操作」

### 不装 expect 的替代

```bash
sshpass -p "mypass" ssh user@host "uptime"
```
- 描述: sshpass 单命令带密码，简单场景更轻量（同样不推荐明文长期使用）

## 示例

### 批量改服务器密码

- 描述: expect 批量化典型场景

```bash
#!/bin/bash
# change-pass.sh: 批量修改多台机器 root 密码
hosts="192.168.1.101 192.168.1.102"
OLD="oldpass"
NEW="NewPass@2026"

for h in $hosts; do
    expect <<EOF
set timeout 10
spawn ssh root@$h "echo 'root:$NEW' | chpasswd"
expect {
    "yes/no"   { send "yes\r"; exp_continue }
    "password:" { send "$OLD\r" }
    timeout    { exit 1 }
}
expect eof
EOF
    echo "$h 处理完成"
done
```

### 自动 ssh 取数据

- 描述: 免密未配置时的巡检脚本

```bash
#!/usr/bin/expect
set host [lindex $argv 0]
set timeout 15
spawn ssh root@$host "df -h /data; free -m | head -2"
expect {
    "password:" { send "mypass\r"; exp_continue }
    eof
}
# 用法: ./remote-check.exp 192.168.1.100
```

### scp 自动输密码

- 描述: 传输场景的应答

```bash
expect -c "
set timeout 60
spawn scp -P 2222 app.jar root@192.168.1.100:/opt/app/
expect {
    \"password:\" { send \"mypass\r\"; exp_continue }
    eof
}
"
# 安全提醒：密码写在脚本/命令行会被 ps 看到，优先改用 SSH 免密
```