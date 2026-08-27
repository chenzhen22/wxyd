---
name: Socket
package: java.net
order: 263
---

## 介绍

`java.net.Socket` 是**客户端套接字**，用于与服务器建立 TCP 连接。

## 方法

构造方法：
```java
public Socket(String host, int port) throws UnknownHostException, IOException
public Socket(InetAddress address, int port) throws IOException
```

### getInputStream / getOutputStream / close

获取输入输出流 / 关闭连接。

### isConnected / isClosed

连接状态检查。

## 测试

- 描述: 创建 Socket 连接
- 断言: 连接和关闭正常

```java
// 方法体开始
System.out.println("=== Socket ===");
try (ServerSocket server = new ServerSocket(0)) {
    int port = server.getLocalPort();
    Socket client = new Socket("localhost", port);
    assertTrue(client.isConnected());
    System.out.println("Socket 连接成功: " + client.getRemoteSocketAddress());
    client.close();
    assertTrue(client.isClosed());
}
System.out.println("=== 测试通过 ===");
// 方法体结束
```
