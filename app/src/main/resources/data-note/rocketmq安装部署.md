---
name: RocketMQ 安装部署
category: 环境搭建
---

## 一．安装

1.将rocketmq-all-5.2.0-bin-release.zip放到/usr/src/下

2.解压文件

unzip没有安装的话，执行yum install unzip

- unzip rocketmq-all-5.2.0-bin-release.zip
- mv rocketmq-all-5.2.0-bin-release rocketmq

##  修改内存大小

- cd /usr/src/rocketmq
- vi bin/runbroker.sh
- vi bin/runserver.sh
- vi bin/tools.sh
- 将JAVA_OPT="${JAVA_OPT} -server -Xms8g -Xmx8g"修改为“JAVA_OPT="${JAVA_OPT} -server -Xms512m -Xmx512m -Xmn256m"

##  启动NameServer

- 启动namesrv
- cd /usr/src/rocketmq
- nohup sh bin/mqnamesrv -n 172.2.17.238:9876 &

- 验证namesrv是否启动成功
- tail -f ~/logs/rocketmqlogs/namesrv.log
- The Name Server boot success...

##  启动Broker+Proxy

- 修改 conf 目录下 rmq-proxy.json。
- cd /usr/src/rocketmq/conf
- vi rmq-proxy.json
- 2、在 proxyConfig.json 文件中将remotingListenPort 8080修改为自己要使用的端口。
- {
- "rocketMQClusterName": "DefaultCluster",
- "grpcServerPort": 8084,
- "remotingListenPort": 8085
- }
- NameServer成功启动后，我们启动Broker和Proxy。这里我们使用 Local 模式部署，即 Broker 和 Proxy 同进程部署。5.x 版本也支持 Broker 和 Proxy 分离部署以实现更灵活的集群能力。

- 先启动broker
- cd /usr/src/rocketmq
- nohup sh bin/mqbroker -n 172.2.17.238:9876 --enable-proxy &
- 验证broker是否启动成功, 比如, broker的ip是192.168.1.2 然后名字是broker-a
- tail -f ~/logs/rocketmqlogs/proxy.log
- The broker[broker-a,192.169.1.2:10911] boot success...

##  关闭服务器

```
cd /usr/src/rocketmq
```

- sh bin/mqshutdown broker
- #The mqbroker(36695) is running...
- #Send shutdown request to mqbroker with proxy enable OK(36695)

- sh bin/mqshutdown namesrv
- #The mqnamesrv(36664) is running...
- #Send shutdown request to mqnamesrv(36664) OK

- **注安装详情见：****https://rocketmq.apache.org/zh/docs/quickStart/01quickstart/**

## rocketmq控制台安装

- **第一步，下载安装rocketmq控制台。**
- **登录网址：**
- **https://archive.apache.org/dist/rocketmq/rocketmq-dashboard/**
- **下载rocketmq-dashboard-1.0.0-source-release.zip安装包，然后将rocketmq控制台安装包上传到/usr/src文件夹。**

- **第二步，解压rocketmq控制台安装包。**

- **命令：cd /usr/src**

- **unzip**** rocketmq-dashboard-1.0.0-source-release.zip**
- **修改端口和设置登录密码**
- **cd ****/usr/src/rocketmq-dashboard-1.0.0/src/main/resources**
- **vi application.properties**
- **server.port=808****0改为server.port=8082**
- **rocketmq.config.loginRequired=true**
- **保存退出**
- **vi users.properties**
- **admin****=****admin****,1**

- **第三步，编译rocketmq控制台。**
- **命令：**
- **cd /usr/src/rocketmq-dashboard-1.0.0**
- **mvn clean package -Dmaven.test.skip=true**

- **若结果最后显示“BUILD SUCCESS”，则说明rocketmq控制台编译成功。**

- **rocketmq控制台配置**
- **第一步，在文件夹/usr****/src****/rocketmq下新建console文件夹。**

- **命令：mkdir -p /usr****/src****/rocketmq/console**

- **第二步，将编译rocketmq控制台得到的jar包移动到新建的console文件夹。**

- **命令：**
- **cd ****/usr/src/rocketmq-dashboard-1.0.0/target**

- **mv rocketmq-dashboard-1.0.0.jar /usr/****src****/rocketmq/console**

- **第三步，启动jar包。**

- **命令：**
- **cd** ** /usr/****src****/rocketmq/console**
- **nohup ****java -jar rocketmq-dashboard-1.0.0.jar** **--server.port=****9848 ****--rocketmq.config.namesrvAddr=****172.2.17.238****:9876**** &**

- **第四步，访问页面。**

- **登录网址：****http://172.2.17.238:****9848****/#/访问，就可以看到下图的页面，右上角可以切换中文。**

- **注意：如果使用阿里云ECS服务器，那么至少需要开放9876和8080两个端口的访问。**
