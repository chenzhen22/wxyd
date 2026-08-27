---
name: DatagramSocket
package: java.net
order: 264
---

## 介绍

`java.net.DatagramSocket` 是 **UDP 数据报套接字**，用于发送和接收 UDP 数据报。

## 方法

构造方法：
```java
public DatagramSocket() throws SocketException
public DatagramSocket(int port) throws SocketException
```

### send / receive

```java
public void send(DatagramPacket p) throws IOException
public void receive(DatagramPacket p) throws IOException
```

### close / isClosed

关闭/检查状态。

## 测试

- 描述: UDP 通信
- 断言: 数据收发正常

```java
// 方法体开始
System.out.println("=== DatagramSocket ===");
DatagramSocket socket = new DatagramSocket();
assertFalse(socket.isClosed());
System.out.println("UDP Socket 已创建, 本地端口: " + socket.getLocalPort());
socket.close();
assertTrue(socket.isClosed());
System.out.println("UDP Socket 已关闭");
System.out.println("=== 测试通过 ===");
// 方法体结束
```
