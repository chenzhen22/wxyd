---
name: DatagramPacket
package: java.net
order: 265
---

## 介绍

`java.net.DatagramPacket` 是 **UDP 数据报包**类，封装了数据报数据、长度、地址和端口信息。

## 方法

构造方法：
```java
public DatagramPacket(byte[] buf, int length)
public DatagramPacket(byte[] buf, int length, InetAddress address, int port)
```

### getData / getLength / getAddress / getPort

获取数据/长度/地址/端口。

### setData / setAddress / setPort

设置数据/地址/端口。

## 测试

- 描述: 创建 UDP 数据包
- 断言: 包信息正确

```java
// 方法体开始
System.out.println("=== DatagramPacket ===");
byte[] data = "Hello UDP".getBytes("UTF-8");
InetAddress addr = InetAddress.getByName("localhost");
DatagramPacket packet = new DatagramPacket(data, data.length, addr, 9999);
assertEquals(data.length, packet.getLength());
assertEquals("localhost", packet.getAddress().getHostName());
assertEquals(9999, packet.getPort());
System.out.println("UDP 包: " + new String(packet.getData(), packet.getOffset(), packet.getLength(), "UTF-8"));
System.out.println("目标: " + packet.getAddress() + ":" + packet.getPort());
System.out.println("=== 测试通过 ===");
// 方法体结束
```
