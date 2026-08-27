---
name: InetAddress
package: java.net
order: 215
---

## 介绍

`java.net.InetAddress` 是 **IP 地址封装类**，用于表示 Internet 协议（IP）地址。

## 方法

### getByName

```java
public static InetAddress getByName(String host) throws UnknownHostException
```

根据主机名或 IP 字符串获取 InetAddress。

### getHostName / getHostAddress

```java
public String getHostName()
public String getHostAddress()
```

获取主机名/IP 地址。

### isReachable

```java
public boolean isReachable(int timeout) throws IOException
```

测试地址是否可达。

## 测试

- 描述: 获取 localhost 地址
- 断言: 获取成功

```java
// 方法体开始
System.out.println("=== InetAddress ===");
InetAddress addr = InetAddress.getByName("localhost");
assertNotNull(addr);
assertTrue(addr.getHostAddress().equals("127.0.0.1") || addr.getHostAddress().equals("::1"));
System.out.println("localhost: " + addr.getHostAddress());
System.out.println("=== 测试通过 ===");
// 方法体结束
```
