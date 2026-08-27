---
name: NetworkInterface
package: java.net
order: 261
---

## 介绍

`java.net.NetworkInterface` 是**网络接口类**，表示本机的网络接口（网卡）。

## 方法

### getNetworkInterfaces

```java
public static Enumeration<NetworkInterface> getNetworkInterfaces() throws SocketException
```

获取所有网络接口。

### getByName

```java
public static NetworkInterface getByName(String name) throws SocketException
```

根据名称获取网络接口。

### getName / getDisplayName / getInetAddresses

获取接口名称/显示名称/IP 地址。

## 测试

- 描述: 获取本机网络接口
- 断言: 至少有一个接口

```java
// 方法体开始
System.out.println("=== NetworkInterface ===");
Enumeration<NetworkInterface> ifaces = NetworkInterface.getNetworkInterfaces();
assertNotNull(ifaces);
assertTrue(ifaces.hasMoreElements());
NetworkInterface iface = ifaces.nextElement();
assertNotNull(iface.getName());
System.out.println("接口: " + iface.getName() + " - " + iface.getDisplayName());
Enumeration<InetAddress> addrs = iface.getInetAddresses();
if (addrs.hasMoreElements()) {
    System.out.println("  IP: " + addrs.nextElement().getHostAddress());
}
System.out.println("=== 测试通过 ===");
// 方法体结束
```
