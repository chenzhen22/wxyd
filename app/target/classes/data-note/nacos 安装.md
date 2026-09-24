---
name: Nacos 安装
category: 环境搭建
---

## 下载Nacos1.2.1

- https://github.com/alibaba/nacos/releases/tag/1.2.1

## 单机版本安装

- 2.1 将下载的nacos安装包传输到服务器
- 2.2 解压文件

```
cd /usr/src/nacos/
tar -zxvf nacos-server-1.2.1.tar.gz
```

- 2.3 进入bin目录下 单机版本启动

```
cd /usr/src/nacos/nacos/bin
sh startup.sh -m standalone
```

- 2.4 关闭nacos

```
sh shutdown.sh
```

- 2.5 访问Nacos地址 IP：8848/nacos

```
http://localhost:8848/nacos/#/login
```

输入账号密码 账号：nacos 密码：nacos
