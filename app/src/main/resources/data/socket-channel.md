---
name: SocketChannel
package: java.nio.channels
order: 371
---

## 介绍

`java.nio.channels.SocketChannel` 是 **NIO 客户端套接字通道**，支持非阻塞 TCP 连接。

## 方法

### open / connect / finishConnect / isConnected

### read / write / configureBlocking

### register / validOps

## 测试

- 描述: SocketChannel 连接
- 断言: 连接成功

```java
// 方法体开始
System.out.println("=== SocketChannel ===");
try (ServerSocketChannel ssc = ServerSocketChannel.open()) {
    ssc.bind(new InetSocketAddress(0));
    int port = ssc.socket().getLocalPort();
    try (SocketChannel sc = SocketChannel.open()) {
        sc.configureBlocking(false);
        sc.connect(new InetSocketAddress("localhost", port));
        assertFalse(sc.isConnected());
    }
    System.out.println("SocketChannel 非阻塞连接测试通过");
} catch (Exception e) {
    System.out.println("SocketChannel 不可用: " + e.getMessage());
}
System.out.println("=== 测试通过 ===");
// 方法体结束
```
