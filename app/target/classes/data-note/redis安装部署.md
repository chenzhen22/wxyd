---
name: Redis 安装部署
category: 环境搭建
---

## 一、Redis介绍

Redis是当前比较热门的NOSQL系统之一，它是一个key-value存储系统。和Memcache类似，但很大程度补偿了Memcache的不足，它支持存储的value类型相对更多，包括string、list、set、zset和hash。这些数据类型都支持push/pop、add/remove及取交集并集和差集及更丰富的操作。在此基础上，Redis支持各种不同方式的排序。和Memcache一样，Redis数据都是缓存在计算机内存中，不同的是，Memcache只能将数据缓存到内存中，无法自动定期写入硬盘，这就表示，一断电或重启，内存清空，数据丢失。所以Memcache的应用场景适用于缓存无需持久化的数据。而Redis不同的是它会周期性的把更新的数据写入磁盘或者把修改操作写入追加的记录文件，实现数据的持久化。

## 二、Redis的安装

下面介绍在Linux环境下，Redis的安装与部署

注：redis是c语言开发，安装redis需要先将官网下载的源码进行编译，编译依赖gcc环境。如果没有gcc环境，需要安装gcc：

执行命令：yum install gcc-c++

1、首先上官网下载Redis 压缩包，地址：http://redis.io/download 下载稳定版3.2.8即可。

2、通过远程管理工具，将压缩包拷贝到Linux服务器中，执行解压操作

3、执行make 对Redis解压后文件进行编译

编译完成之后，可以看到解压文件redis-3.2.8 中会有对应的src、conf等文件夹，这和windows下安装解压的文件一样，大部分安装包都会有对应的类文件、配置文件和一些命令文件。

4、编译成功后，进入src文件夹，执行make install进行Redis安装

5、安装完成，界面如下

## 三、Redis的部署

安装成功后，下面对Redis 进行部署

1、首先为了方便管理，将Redis文件中的conf配置文件和常用命令移动到统一文件中

a)创建bin和redis.conf文件

复制代码代码如下:mkdir -p /usr/local/redis/binmkdir -p /usr/local/redis/etc

b)执行Linux文件移动命令：

复制代码代码如下:mv /usr/src/redis/redis-5.0.3/redis.conf  /usr/local/redis/etccd  /usr/src/redis/redis-5.0.3/srcmv mkreleasehdr.sh redis-benchmark redis-check-aof redis-cli redis-server /usr/local/redis/bin

- 执行Redis-server 命令，启动Redis 服务
- cd /usr/local/redis/bin
- ./redis-server

注意：这里直接执行Redis-server 启动的Redis服务，是在前台直接运行的(效果如上图)，也就是说，执行完该命令后，如果Lunix关闭当前会话，则Redis服务也随即关闭。正常情况下，启动Redis服务需要从后台启动，并且指定启动配置文件。

3、后台启动redis服务

a)首先编辑conf文件，将daemonize属性改为yes（表明需要在后台运行）

```
cd /usr/local/redis/etcvi redis.conf
```

bind 127.0.0.1 改为 bind 172.2.17.238

b)再次启动redis服务，并指定启动服务配置文件

- **cd ****/usr/local/redis/bin**

```
./redis-server /usr/local/redis/etc/redis.conf
```

4、服务端启动成功后，执行redis-cli启动Redis 客户端，查看端口号。

## 四、密码设置

- 1.redis密码设置有两种方式，一种需要重启redis服务，一种不需要重启redis服务。
- 方法一：通过配置文件（/usr/local/redis/etc/redis.conf）进行设置
- 这种方法在设置密码后需要重启redis生效。首先找到redis的配置文件—redis.conf文件，然后修改里面的requirepass（requirepass  是配置redis访问密码的参数），这个本来是注释起来了的，将注释去掉，并将后面对应的字段设置成自己想要的密码，保存退出。重启redis服务，即可。（推荐学习：Redis视频教程）

- 注：通过命令行修改了密码之后，配置文件（/etc/redis.conf）的requirepass字段后面的密码是不会随之修改的。

## 五、添加到systemctl

- vi /etc/systemd/system/redis.service
- [Unit]
- Description=redis
- After=network.target

- [Service]
- Type=forking
- ExecStart=/usr/local/redis/bin/redis-server /usr/local/redis/etc/redis.conf
- ExecStop=/usr/local/redis/bin/redis-cli shutdown
- Restart=always

- [Install]
- WantedBy=multi-user.target

- 重新加载systemd以读取新的服务文件:
- systemctl daemon-reload
- 启动Redis服务
- systemctl start redis
- 设置Redis服务开机自启：
- systemctl enable redis

## 六、总结Linux 、Redis 操作常用命令

```
Linux：
```

cd /usr 从子文件夹进入上级文件夹usrcd local 从父到子mv /A /B 将文件A移动到Bvi usr/local/redis/redis.conf 编辑redis.conf 文件:wq 保存修改，并退出

```
Redis：
```

Redis-server /usr..../redis.conf 启动redis服务，并指定配置文件Redis-cli 启动redis 客户端Pkill redis-server 关闭redis服务Redis-cli shutdown 关闭redis客户端Netstat -tunpl|grep 6379 查看redis 默认端口号6379占用情况

4. Redis的配置

4.1. Redis默认不是以守护进程的方式运行，可以通过该配置项修改，使用yes启用守护进程

```
daemonize no
```

4.2. 当Redis以守护进程方式运行时，Redis默认会把pid写入/var/run/redis.pid文件，可以通过pidfile指定

```
pidfile /var/run/redis.pid
```

4.3. 指定Redis监听端口，默认端口为6379，作者在自己的一篇博文中解释了为什么选用6379作为默认端口，因为6379在手机按键上MERZ对应的号码，而MERZ取自意大利歌女Alessia Merz的名字

```
port 6379
```

4.4. 绑定的主机地址

```
bind 127.0.0.1
```

4.5.当 客户端闲置多长时间后关闭连接，如果指定为0，表示关闭该功能

```
timeout 300
```

4.6. 指定日志记录级别，Redis总共支持四个级别：debug、verbose、notice、warning，默认为verbose

```
loglevel verbose
```

4.7. 日志记录方式，默认为标准输出，如果配置Redis为守护进程方式运行，而这里又配置为日志记录方式为标准输出，则日志将会发送给/dev/null

```
logfile stdout
```

4.8. 设置数据库的数量，默认数据库为0，可以使用SELECT< dbid>命令在连接上指定数据库id

```
databases 16
```

4.9. 指定在多长时间内，有多少次更新操作，就将数据同步到数据文件，可以多个条件配合

```
save< seconds> <changes>
```

Redis默认配置文件中提供了三个条件：

```
save 900 1
    save 300 10
    save 60 10000
```

分别表示900秒（15分钟）内有1个更改，300秒（5分钟）内有10个更改以及60秒内有10000个更改。

4.10. 指定存储至本地数据库时是否压缩数据，默认为yes，Redis采用LZF压缩，如果为了节省CPU时间，可以关闭该选项，但会导致数据库文件变的巨大

```
rdbcompression yes
```

4.11. 指定本地数据库文件名，默认值为dump.rdb

```
dbfilename dump.rdb
```

4.12. 指定本地数据库存放目录

```
dir ./
```

4.13. 设置当本机为slav服务时，设置master服务的IP地址及端口，在Redis启动时，它会自动从master进行数据同步

```
slaveof< masterip> <masterport>
```

4.14. 当master服务设置了密码保护时，slav服务连接master的密码

```
masterauth< master-password>
```

4.15. 设置Redis连接密码，如果配置了连接密码，客户端在连接Redis时需要通过AUTH< password>命令提供密码，默认关闭

```
requirepass foobared
```

4.16. 设置同一时间最大客户端连接数，默认无限制，Redis可以同时打开的客户端连接数为Redis进程可以打开的最大文件描述符数，如果设置 maxclients 0，表示不作限制。当客户端连接数到达限制时，Redis会关闭新的连接并向客户端返回max number of clients reached错误信息

```
maxclients 128
```

4.17. 指定Redis最大内存限制，Redis在启动时会把数据加载到内存中，达到最大内存后，Redis会先尝试清除已到期或即将到期的Key，当此方法处理 后，仍然到达最大内存设置，将无法再进行写入操作，但仍然可以进行读取操作。Redis新的vm机制，会把Key存放内存，Value会存放在swap区

```
maxmemory< bytes>
```

4.18. 指定是否在每次更新操作后进行日志记录，Redis在默认情况下是异步的把数据写入磁盘，如果不开启，可能会在断电时导致一段时间内的数据丢失。因为 redis本身同步数据文件是按上面save条件来同步的，所以有的数据会在一段时间内只存在于内存中。默认为no

```
appendonly no
```

4.19. 指定更新日志文件名，默认为appendonly.aof

```
appendfilename appendonly.aof
```

4.20. 指定更新日志条件，共有3个可选值：     **no**：表示等**操作系统**进行数据缓存同步到磁盘（快）     **always**：表示每次更新操作后手动调用fsync()将数据写到磁盘（慢，安全）     **everysec**：表示每秒同步一次（折衷，默认值）

```
appendfsync everysec
```

4.21. 指定是否启用虚拟内存机制，默认值为no，简单的介绍一下，VM机制将数据分页存放，由Redis将访问量较少的页即冷数据swap到磁盘上，访问多的页面由磁盘自动换出到内存中（在后面的文章我会仔细分析Redis的VM机制）

```
vm-enabled no
```

4.22. 虚拟内存文件路径，默认值为/tmp/redis.swap，不可多个Redis实例共享

```
vm-swap-file /tmp/redis.swap
```

4.23. 将所有大于vm-max-memory的数据存入虚拟内存,无论vm-max-memory设置多小,所有索引数据都是内存存储的(Redis的索引数据 就是keys),也就是说,当vm-max-memory设置为0的时候,其实是所有value都存在于磁盘。默认值为0

```
vm-max-memory 0
```

4.24. Redis swap文件分成了很多的page，一个对象可以保存在多个page上面，但一个page上不能被多个对象共享，vm-page-size是要根据存储的 数据大小来设定的，作者建议如果存储很多小对象，page大小最好设置为32或者64bytes；如果存储很大大对象，则可以使用更大的page，如果不 确定，就使用默认值

```
vm-page-size 32
```

4.25. 设置swap文件中的page数量，由于页表（一种表示页面空闲或使用的bitmap）是在放在内存中的，，在磁盘上每8个pages将消耗1byte的内存。

```
vm-pages 134217728
```

4.26. 设置访问swap文件的线程数,最好不要超过机器的核数,如果设置为0,那么所有对swap文件的操作都是串行的，可能会造成比较长时间的延迟。默认值为4

```
vm-max-threads 4
```

4.27. 设置在向客户端应答时，是否把较小的包合并为一个包发送，默认为开启

```
glueoutputbuf yes
```

4.28. 指定在超过一定的数量或者最大的元素超过某一临界值时，采用一种特殊的哈希**算法**

```
hash-max-zipmap-entries 64
    hash-max-zipmap-value 512
```

4.29. 指定是否激活重置哈希，默认为开启（后面在介绍Redis的哈希算法时具体介绍）

```
activerehashing yes
```

4.30. 指定包含其它的配置文件，可以在同一主机上多个Redis实例之间使用同一份配置文件，而同时各个实例又拥有自己的特定配置文件

```
include /path/to/local.conf
```
