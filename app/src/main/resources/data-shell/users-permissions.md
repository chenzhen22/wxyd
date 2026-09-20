---
name: 用户与权限
category: 系统运维
order: 9
---

## 介绍

Linux 权限模型围绕「属主/属组/其他人」三组身份与「读/写/执行」三种权限展开。应用该用专用用户跑、日志目录该给组权限、sudo 该细粒度授权——这些是安全审计的基本要求，也是 Permission denied 报错的全部来源。

核心特点：
- **三段九位**：rwxr-xr-- 依次是属主/属组/其他人，数字 421 相加表示
- **chmod 改权限**：数字法 755 直观，符号法 u+x 精准增量
- **chown 改归属**：属主与属组一起改 chown user:group file
- **sudo 细粒度**：sudoers 授权特定命令，比给 root 密码安全得多

## 语法

### 权限查看

```bash
ls -l /opt/app/run.sh
stat /opt/app/run.sh
```
- 描述: -rwxr-xr-- 一位是类型，后九位三组分属主/组/其他；stat 信息更全

### 数字法改权限

```bash
chmod 755 deploy.sh
chmod 644 config.yml
```
- 描述: 7=4+2+1 读写执行，5=4+1 读执行，4=只读；脚本 755、配置 644 是惯例

### 符号法改权限

```bash
chmod u+x run.sh
chmod g-w,o-r secret.key
```
- 描述: u/g/o 身份 + +/-/= 动作 + rwx 权限，只动指定部分

### 递归改

```bash
chmod -R 755 /opt/app/bin
chown -R appuser:appgroup /opt/app
```
- 描述: -R 递归整个目录；chown 属主:属组 一次改齐

### 用户管理

```bash
useradd -m -s /bin/bash appuser
passwd appuser
usermod -aG appgroup appuser
```
- 描述: -m 建家目录、-s 指定 shell；-aG 追加到附加组（漏 a 会踢出其他组）

### sudo 授权

```bash
visudo
# appuser ALL=(ALL) NOPASSWD: /bin/systemctl restart wxyd
```
- 描述: 只允许免密执行指定命令，权限收口；visudo 校验语法防锁死

### 切换身份

```bash
su - appuser
sudo -u appuser java -jar app.jar
```
- 描述: su - 完整切换环境；sudo -u 以指定用户执行单条命令

## 示例

### 给应用建专用账号

- 描述: 告别 root 跑应用

```bash
# 1. 建系统用户（不可登录 shell）
useradd -r -m -s /sbin/nologin appuser

# 2. 目录归属
chown -R appuser:appuser /opt/app

# 3. 以该用户启动
sudo -u appuser java -jar /opt/app/wxyd.jar

# 4. 确认归属
ps -o user= -p $(pgrep -f wxyd.jar)
```

### Permission denied 排查

- 描述: 三层检查定位归属

```bash
# 1. 文件权限与归属
ls -l /opt/app/logs/app.log

# 2. 目录路径逐级检查（父目录无 x 权限同样进不去）
namei -l /opt/app/logs/app.log

# 3. SELinux（centos 偶发）
ls -Z /opt/app/logs/
# 临时关闭验证：setenforce 0
```

### 运维人员授权

- 描述: 给同事最小化 sudo 权限

```bash
# visudo 追加：ops 组可免密重启应用与查日志
# %ops ALL=(root) NOPASSWD: /bin/systemctl restart wxyd, /bin/journalctl -u wxyd*

# 建组拉人
groupadd ops
usermod -aG ops zhangsan

# 验证
sudo -l    # 看自己被授权的命令清单
```