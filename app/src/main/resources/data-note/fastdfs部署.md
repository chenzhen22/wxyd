---
name: FastDFS 部署
category: 环境搭建
---

# fastdfs部署

## 传包

- 将fastdfs放入/usr/src下

## 安装libfastcommon

- 解压安装包
- cd /usr/src/fastdfs/
- unzip libfastcommon-master.zip

- 进入libfastcommon-master安装目录，分别执行./make.sh和./make.sh install

- libfastcommon默认会被安装到/usr/lib64/libfastcommon.so，FastDFS的主程序在/usr/local/lib目录下，创建软连接
- ln -s /usr/lib64/libfastcommon.so /usr/local/lib/libfastcommon.so
- ln -s /usr/lib64/libfastcommon.so /usr/lib/libfastcommon.so
- ln -s /usr/lib64/libfdfsclient.so /usr/local/lib/libfdfsclient.so
- ln -s /usr/lib64/libfdfsclient.so /usr/lib/libfdfsclient.so

- 至此，libfastcommon安装完成

## 安装fastdfs

- 在安装fastdfs之前，需要先安装libserverframe，这是因为我这里安装的最新fastdfs安装包，缺少libserverframe，github地址，否则执行./make.sh时报错

- 解压安装包unzip libserverframe-master.zip
- 进入安装目录，分别执行./make.sh和./make.sh install，如遇到如下问题，执行./make.sh clean然后重新./make.sh

- 安装完毕后，安装fasfdfs，解压安装包unzip fastdfs-master.zip
- 进入fastdfs-master目录，分别执行./make.sh和./make.sh install
- fastdfs配置文件默认在/etc/fdfs，我们继续执行./setup.sh /etc/fdfs安装

## 配置tracker

- mkdir -p  /opt/fastdfs

- 先创建tracker的工作目录，主要用来保存data和log，我直接在fastdfs同级目录创建了/tools/fastdfs_tracker，进入/etc/fdfs配置目录，编辑配置文件vi /etc/fdfs/tracker.conf，修改配置如下:
- 1.disabled=false #默认开启（false是启用这个配置文件）
- 2.port=22122 #默认端口号 （tracker默认端口）
- 3.base_path=/opt/fastdfs  #上面创建的目录就用在这里
- 4.http.server_port=8083#默认端口是8080（tracker的默认http端口）

- 修改完成保存，然后启动tracker，systemctl start fdfs_trackerd，启动后，在工作目录可以看到创建了data和log目录，也可以查看下端口监听netstat -unltp|grep fdfs

- 修改完成保存，然后启动tracker，systemctl start fdfs_trackerd，启动后，在工作目录可以看到创建了data和log目录，也可以查看下端口监听netstat -unltp|grep fdfs

- 关闭trackerservice fdfs_trackerd stop，然后将其加入开机启动，查看ll /etc/rc.d/rc.local是否有修改权限，若为-rwxr-xr-x表示可以，若为-rw-r–r–则需要刷新权限

## 配置storage

- 新建storage安装目录/tools/storage和文件存储目录/tools/fdfs_storage_data，编辑配置文件vi /etc/fdfs/storage.conf，如下
- 1.disabled=false
- 2.group_name=group1 #组名，根据实际情况修改
- 3.port=23000 #设置storage的端口号，默认是23000，同一个组的storage端口号必须一致
- 4.base_path=/opt/fastdfs #设置storage数据文件和日志目录
- 5.store_path_count=1 #存储路径个数，需要和store_path个数匹配
- 6.tracker_server=ip:22122 #这里我写的是服务器内网ip
- 7.http.server_port=8083 #设置 http 端口号，别设置6667，坑
- 创建软连接ln -s /usr/bin/fdfs_storaged /usr/local/bin，启动前确认tracker已启动，然后启动systemctl start fdfs_storaged，启动后查看netstat -unltp | grep fdfs

## 修改密钥

- vi /etc/fdfs/http.conf
- http.anti_steal.secret_key = FastDFS123!!
