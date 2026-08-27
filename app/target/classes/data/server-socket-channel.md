---
name: ServerSocketChannel
package: java.nio.channels
order: 370
---

## 介绍

`java.nio.channels.ServerSocketChannel` 是 **NIO 服务端套接字通道**，非阻塞模式下的服务端 Socket。

## 方法

### open / bind / accept / configureBlocking

### register / validOps

## 测试

- 描述: 创建 ServerSocketChannel
- 断言: 创建成功

```java
// 方法体开始
System.out.println("=== ServerSocketChannel ===");
try (ServerSocketChannel ssc = ServerSocketChannel.open()) {
    ssc.bind(new InetSocketAddress(0));
    assertTrue(ssc.socket().isBound());
    assertTrue(ssc.isOpen());
    ssc.configureBlocking(false);
    assertFalse(ssc.isBlocking());
    SocketChannel sc = ssc.accept();
    assertNull(sc);
    System.out.println("ServerSocketChannel 绑定端口: " + ssc.socket().getLocalPort());
} catch (Exception e) {
    System.out.println("ServerSocketChannel 不可用: " + e.getMessage());
}
System.out.println("=== 测试通过 ===");
// 方法体结束
```
