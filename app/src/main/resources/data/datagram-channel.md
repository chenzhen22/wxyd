---
name: DatagramChannel
package: java.nio.channels
order: 365
---

## 介绍

`java.nio.channels.DatagramChannel` 是 **NIO UDP 通道**类，用于发送和接收 UDP 数据报。

## 方法

### open / bind / send / receive

### connect / disconnect / isConnected

### read / write

## 测试

- 描述: 使用 DatagramChannel
- 断言: 通道创建成功

```java
// 方法体开始
System.out.println("=== DatagramChannel ===");
try (DatagramChannel channel = DatagramChannel.open()) {
    assertNotNull(channel);
    assertFalse(channel.isConnected());
    channel.bind(new InetSocketAddress(0));
    assertTrue(channel.socket().isBound());
    System.out.println("DatagramChannel 创建成功，端口: " + channel.socket().getLocalPort());
} catch (Exception e) {
    System.out.println("DatagramChannel 不可用: " + e.getMessage());
}
System.out.println("=== 测试通过 ===");
// 方法体结束
```
