---
name: Tar 压缩解压
category: 文件操作
order: 2
---

## 介绍

tar 是 Linux 下最常用的打包压缩工具，先打包（tar）再压缩（gzip/bzip2），一个命令完成。日志归档、代码发布包、数据库备份传输都离不开它。

核心特点：
- **打包与压缩分离**：tar 负责打包，-z 调用 gzip、-j 调用 bzip2、-J 调用 xz
- **保留权限属性**：默认保留目录结构与权限（配合 -p）
- **可查看可解压**：-t 只看不解、-x 解压、-c 创建
- **远程传输**：配合 ssh 可直接在主机间管道传输

## 语法

### 打包压缩

```bash
tar -czvf archive.tar.gz /path/to/dir
```
- 描述: c 创建、z gzip 压缩、v 显示过程、f 指定文件名，最常用组合

### 解压

```bash
tar -xzvf archive.tar.gz -C /target/dir
```
- 描述: x 解压，-C 指定解压目标目录（默认当前目录）

### 仅查看内容

```bash
tar -tzvf archive.tar.gz
```
- 描述: t 列出包内文件清单，不解压即可确认内容

### 排除目录打包

```bash
tar -czvf app.tar.gz --exclude="*.log" /opt/app
```
- 描述: --exclude 排除匹配项，可写多个，注意放在源路径之前

### bzip2 高压缩

```bash
tar -cjvf archive.tar.bz2 /path/to/dir
```
- 描述: j 使用 bzip2，压缩率更高但更慢；-J 为 xz 极限压缩

### 只打包不压缩

```bash
tar -cvf archive.tar /path/to/dir
```
- 描述: 不加 z/j/J 只打包，速度快，适合已压缩数据（如图片、jar）

### 追加文件到包

```bash
tar -rvf archive.tar newfile.txt
```
- 描述: r 追加文件，仅对未压缩的 .tar 有效

## 示例

### 日常备份

- 描述: 带日期命名的日志归档

```bash
# 把日志目录打包成带日期的归档文件
tar -czvf logs-$(date +%Y%m%d).tar.gz /opt/app/logs/

# 排除不需要的文件再打包
tar -czvf backup.tar.gz --exclude="*.log" --exclude="target" /opt/app

# 解压到指定目录
mkdir -p /tmp/restore && tar -xzvf backup.tar.gz -C /tmp/restore
```

### 发布包制作与校验

- 描述: 制作部署包、验证完整性

```bash
# 制作发布包（排除开发文件）
tar -czvf app-1.0.0.tar.gz --exclude=".git" --exclude="*.iml" ./app

# 解压前先看清单，确认没有多余文件
tar -tzvf app-1.0.0.tar.gz | head -20

# 校验包完整性（对比 md5）
md5sum app-1.0.0.tar.gz
```

### 跨主机传输

- 描述: tar + ssh 管道直接传远程

```bash
# 本机目录直接打包传输到远程并解压
tar -czf - /opt/app | ssh user@192.168.1.100 'tar -xzf - -C /opt/app'

# 远程目录打包拉回本地
ssh user@192.168.1.100 'tar -czf - /var/log/app' | tar -xzvf - -C ./backup
```