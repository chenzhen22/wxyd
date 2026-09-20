---
name: Ln 软硬链接
category: 文件操作
order: 6
---

## 介绍

ln 创建文件链接：软链接（符号链接）类似快捷方式，指向路径，可跨文件系统、可链目录；硬链接是同一份数据的多个名字，不可跨文件系统、不可链目录。软链接用于版本切换、配置复用，是发布脚本的常客。

核心特点：
- **软链接**：ln -s 创建，存的是路径，源删则失效（红链）
- **硬链接**：ln 创建，同一 inode，删任意一个名字数据仍在
- **版本切换利器**：current -> app-1.0.0 软链，发布时重指向即回滚/升级
- **排查失效链**：find -xtype l 找断链

## 语法

### 创建软链接

```bash
ln -s /opt/app/app-1.0.0 /opt/app/current
```
- 描述: -s 创建软链接，current 指向 app-1.0.0 目录

### 查看链接指向

```bash
ls -l /opt/app/current
readlink /opt/app/current
readlink -f /opt/app/current
```
- 描述: ls -l 显示 -> 指向；readlink 看原始指向；-f 显示绝对真实路径

### 更新软链接指向

```bash
ln -sfn /opt/app/app-1.1.0 /opt/app/current
```
- 描述: -f 覆盖已有链接、-n 把目标当普通文件处理，目录软链更新必备三连

### 创建硬链接

```bash
ln /data/file.txt /data/file.bak
```
- 描述: 不加 -s 即硬链接，两名字指向同一 inode，修改互相同步

### 查找失效链接

```bash
find /opt -xtype l
```
- 描述: -xtype l 找指向已被删除的断链，找到即可清理

### 删除链接

```bash
rm /opt/app/current
```
- 描述: 只删链接本身；注意 rm current/ 带斜杠会试图操作目标目录，危险

## 示例

### 应用版本切换与秒级回滚

- 描述: 发布脚本的经典套路

```bash
# 目录布局
# /opt/app/releases/app-20260914/
# /opt/app/releases/app-20260915/
# /opt/app/current -> releases/app-20260915

# 发布新版本：解压到 releases 后重指
ln -sfn /opt/app/releases/app-20260915 /opt/app/current

# 出问题秒回滚
ln -sfn /opt/app/releases/app-20260914 /opt/app/current

# systemd 服务固定用 /opt/app/current 路径，无需改配置
```

### 配置文件复用

- 描述: 多实例共享一份配置

```bash
# 多个实例链接同一份公共配置
ln -sf /opt/app/conf/common.yml /opt/app/instance1/common.yml
ln -sf /opt/app/conf/common.yml /opt/app/instance2/common.yml

# 改一处全部生效
vim /opt/app/conf/common.yml
```

### 磁盘空间腾挪

- 描述: 大目录挪盘后无缝衔接

```bash
# /var/log 太满，把日志挪到大磁盘
mv /var/log/app /data/applog
ln -s /data/applog /var/log/app

# 应用无感知，继续写原路径
ls -l /var/log | grep app
```