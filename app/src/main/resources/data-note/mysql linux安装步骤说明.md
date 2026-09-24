---
name: MySQL Linux 安装步骤说明
category: 环境搭建
---

# Linux

## 关于VIM常见异常

临时文件异常

说明:当使用VIM指令编辑文件时,当程序意外终止,文件没有完成保存操作时,这时Linux会动态的生成.swp后缀的文件.该文件是一种保护文件.有该文件存在.vim指令将不能正常执行.

解决方案:

- 点击D属性

- 直接删除临时文件

```
Rm -rf .a.txt.swp
```

## Linux系统安装JDK

准备JDK安装包

上传JDK安装包

说明:将JDK安装包上传到指定文件目录下/usr/local/src下

如图-16所示

图- 16

解压JDK文件

命令: tar -xvf jdk-7u51-linux-x64.tar.gz

如图-17所示

图- 17

配置环境变量

说明:修改Linux系统中环境变量需要修改/etc/profile文件

命令:vim /etc/profile

配置文件变量

```
#set java env
JAVA_HOME=/usr/local/src/java/jdk1.7.0_51
JAVA_BIN=/usr/local/src/java/jdk1.7.0_51/bin
PATH=$JAVA_HOME/bin:$PATH
CLASSPATH=.:$JAVA_HOME/lib/dt.jar:$JAVA_HOME/lib/tools.jar
export JAVA_HOME JAVA_BIN PATH CLASSPATH
```

环境变量生效:

```
source /etc/profile
```

检测JDK是否生效

命令:java -version

如图-18所示

图- 18

## 服务器部署原理图

服务部署软件说明

- 安装JDK
- 生成服务器jar/war包.
- Linux系统中安装Mysql数据库.

Linux链接Windows Mysql  可以

Linux链接Linux本地的Mysql 可以.

- 关闭防火墙.开放访问权限.

服务器部署流程图

## 安装Linux版数据库

数据库准备

在课前资料中找到数据库文件

上传Mysql安装包

解压数据库文件

说明:RPM文件相当于windows的Exe文件.

安装mysql数据库

**执行之前最好拍张快照.保护原始记录.**

- 56-debuginfo
- Shared
- Client
- Server

命令:

```
rpm -ivh Percona-Server-57-debuginfo-5.7.44-48.1.el7.x86_64.rpm
```

2.安装shards

检查mariadb-libs

```
rpm -qa |grep mariadb-libs
```

卸载

```
yum remove mariadb-libs-5.5.68-1.el7.x86_64
```

```
rpm -ivh Percona-Server-shared-compat-57-5.7.44-48.1.el7.x86_64.rpm
rpm -ivh Percona-Server-shared-57-5.7.44-48.1.el7.x86_64.rpm
```

3.安装client客户端

```
rpm -ivh Percona-Server-client-57-5.7.44-48.1.el7.x86_64.rpm
```

4.安装server数据库

```
yum install perl
yum install net-tools
rpm -ivh Percona-Server-server-57-5.7.44-48.1.el7.x86_64.rpm
```

安装完成.

启动mysql数据库

1.启动mysql   service mysql start

2.重启mysql   service mysql restart

3.停止mysql   service mysql stop

查看用户名和密码

命令: grep 'temporary password' /var/log/mysqld.log

- 登录mysql

2.设置密码安全等级

```
set global validate_password_policy=0;
```

```
set global validate_password_length=1;
```

3.修改密码

```
ALTER USER 'root'@'localhost' IDENTIFIED BY 'root';
```

4.检查数据库

创建用户：

```
CREATE USER ‘plgprd’@’localhost’ IDENTIFED BY ‘plgprd_test’;
```

开放mysql对外访问权限

如果需要通过远程连接mysql,则必然开启mysql权限.

语法：

grant [权限] on [数据库名].[表名] to ['用户名']@['web服务器的ip地址'] identified by ['密码'];

```
grant all on *.* to 'root'@'%' identified by 'root';
```

或者指定IP地址

```
grant all on *.* to 'root'@'192.168.1.103' identified by 'root';
```

关于数据库启动问题

如果数据库启动出现了问题.socket问题.

思路:

- 利用Linux的检索工具.检查Mysql服务项.
- 利用杀死进程命令,关闭mysql服务,之后重启.

检索mysql服务项

关闭Linux防火墙

一、

1.执行以下命令关闭firewalld服务：

```
systemctl stop firewalld
systemctl disable firewalld
```

2.检查防火墙状态

```
systemctl status firewalld
```

二、

- 永久操作

关闭防火墙设置:

```
chkconfig iptables off
```

开启防火墙设置:

```
chkconfig iptables on
```

说明:上述操作,修改的是防火墙的配置,修改之后重启才有效.当前的防火墙还没有关闭.

- 临时操作

关闭防火墙:

```
service iptables stop
```

开启防火墙:

```
service iptables start
```

远程操作数据库

新建数据库链接:

链接测试:

项目打包部署

说明:将京淘项目通过maven 进行打包部署.

部署多台tomcat服务

- 修改yml配置文件重新打包 8081/8082/8083
- 利用压缩文件.将war/jar进行修改.

- 准备tomcat服务器

方法3:

先将.war的文件解压.之后完成端口号的修改.再次将文件压缩.之后修改后缀名.

发布项目

单个项目发布

说明:利用java 命令启动项目

命令:

启动命令:java -jar 8081.war

关闭命令:ctrl + c

多个tomcat服务器部署

命令:

1.启动命令

```
java -jar 8081.war & java -jar 8082.war & java -jar 8083.war &
```

2.停止命令

检索tomcat的所有服务.杀死tomcat进程号（关闭多个tomcat服务器）

Nginx实现tomcat负载均衡

需求:在linux中部署了3台tomcat服务器.之后需要使用nginx实现负载均衡操作.

#部署Linux的集群部署  默认是轮询策略

```
upstream jtLinux {
server 192.168.126.174:8081;
server 192.168.126.174:8082;
server 192.168.126.174:8083;
}
```

#配置后台管理服务器

```
server {
listen 80;
server_name manage.jt.com;
```

#访问真实服务器 #默认是轮询策略

```
location / {
proxy_passhttp://jtLinux;
}
}
```

重启nginx服务器.

## 关于Linux部署常见问题说明

项目运行异常

问题说明: com.jt.service.ItemServiceImpl和com.jt.serviceImpl.ItemServiceImpl重复类异常.

检查错误:

- 经查询确实没有发现重复的类名.

问题说明:

问题出在编译的过程中.

.java文件------.class文件.

解决办法:

利用解压工具,将类名的**class**类型进行排查.

方法2:将jar包文件maven-clean

之后将项目clean一下.

Mysql数据库链接问题

笔记中记录: 设定用户名和密码 root:123456.

远程SqlYog链接:

报错:数据库链接异常.

修改mysql的对外权限的用户名和密码. 修改全部mysql的权限.之后保存再次测试.

作业:

作业1:

- 开启2台虚拟机. 各自部署3台tomcat服务器.

思路:

服务器+mysql在一起 可以使用localhost进行访问.

服务器+mysql数据库不在一起. 如何访问数据库????

- 配置6台tomcat服务器的轮询机制.

作业2:

自己整理常见Linux命令 检索/查询线程形成 work文档.
