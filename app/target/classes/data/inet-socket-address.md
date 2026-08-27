---
name: InetSocketAddress
package: java.net
order: 260
---

## 介绍

`java.net.InetSocketAddress` 是 **IP 地址 + 端口**封装类。

## 方法

构造方法：
```java
public InetSocketAddress(String hostname, int port)
public InetSocketAddress(InetAddress addr, int port)
public InetSocketAddress(int port)  // 通配符地址
```

### getHostName / getPort / getAddress

获取主机名/端口/IP 地址。

### isUnresolved

检查是否已被解析。

## 测试

- 描述: 创建 Socket 地址
- 断言: 主机名和端口正确

```java
// 方法体开始
System.out.println("=== InetSocketAddress ===");
InetSocketAddress addr = new InetSocketAddress("localhost", 8080);
assertEquals("localhost", addr.getHostName());
assertEquals(8080, addr.getPort());
System.out.println("地址: " + addr);
InetSocketAddress unresolved = InetSocketAddress.createUnresolved("unknown-host", 80);
assertTrue(unresolved.isUnresolved());
System.out.println("未解析: " + unresolved);
System.out.println("=== 测试通过 ===");
// 方法体结束
```
