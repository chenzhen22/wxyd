---
name: Nginx 安装
category: 环境搭建
---

## 安装所需环境

- gcc 安装安装 nginx 需要先将官网下载的源码进行编译，编译依赖 gcc 环境，如果没有 gcc 环境，则需要安装：
- yum install gcc-c++

- 二. PCRE pcre-devel 安装
- PCRE(Perl Compatible Regular Expressions) 是一个Perl库，包括 perl 兼容的正则表达式库。nginx 的 http 模块使用 pcre 来解析正则表达式，所以需要在 linux 上安装 pcre 库，pcre-devel 是使用 pcre 开发的一个二次开发库。nginx也需要此库。命令：
- yum install -y pcre pcre-devel

- 三. zlib 安装
- zlib 库提供了很多种压缩和解压缩的方式， nginx 使用 zlib 对 http 包的内容进行 gzip ，所以需要在 Centos 上安装 zlib 库。
- yum install -y zlib zlib-devel

- 四. OpenSSL 安装
- OpenSSL 是一个强大的安全套接字层密码库，囊括主要的密码算法、常用的密钥和证书封装管理功能及 SSL 协议，并提供丰富的应用程序供测试或其它目的使用。
- nginx 不仅支持 http 协议，还支持 https（即在ssl协议上传输http），所以需要在 Centos 安装 OpenSSL 库。
- yum install -y openssl openssl-devel

## 安装

- ①下载nginx包：https://nginx.org/en/download.html
- ②将nginx包放入/usr/src/nginx/
- ③开始安装：
- cd /usr/src/nginx/
- tar -zxvf nginx-1.20.2.tar.gz
- cd nginx-1.20.2
- --默认配置
- ./configure
- ./configure --with-http_stub_status_module --with-http_ssl_module --with-stream

- --编译安装
- make
- make install
- ④查找安装路径
- whereis nginx

- ⑤启动、停止nginx
- cd /usr/local/nginx/sbin/
- ./nginx
- ./nginx -s stop
- ./nginx -s quit
- ./nginx -s reload

## 开机自启动

- 即在rc.local增加启动代码就可以了。
- vi /etc/rc.local
- 增加一行 /usr/local/nginx/sbin/nginx
- 设置执行权限：
- chmod 755 rc.local

## 加入systemctl

- 先删除nginx的进程
- cd /usr/local/nginx/sbin/
- ./nginx -s  stop
- 编辑 nginx.service文件
- vim /usr/lib/systemd/system/nginx.service
- [Unit]
- Description=nginx
- After=network.target

- [Service]
- Type=forking
- ExecStart=/usr/local/nginx/sbin/nginx
- ExecReload=/usr/local/nginx/sbin/nginx -s reload
- ExecStop=/usr/local/nginx/sbin/nginx -s stop
- PrivateTmp=true

- [Install]
- WantedBy=multi-user.target

- 配置生效&&nginx重新启动&&nginx开机自启
- systemctl daemon-reload
- systemctl start nginx
- systemctl enable nginx
