---
name: NetProxy
package: java.net
order: 295
---

## 介绍

`java.net.Proxy` 是**代理类**，指定网络连接的代理服务器。

## 常量

- `NO_PROXY` — 不使用代理

### Type

```java
public enum Type { DIRECT, HTTP, SOCKS }
```

## 方法

构造方法：
```java
public Proxy(Type type, SocketAddress sa)
```

### type / address

获取代理类型和地址。

## 测试

- 描述: 创建代理对象
- 断言: 代理信息正确

```java
// 方法体开始
System.out.println("=== Proxy ===");
Proxy noProxy = Proxy.NO_PROXY;
assertEquals(Proxy.Type.DIRECT, noProxy.type());
InetSocketAddress addr = new InetSocketAddress("proxy.example.com", 8080);
Proxy httpProxy = new Proxy(Proxy.Type.HTTP, addr);
assertEquals(Proxy.Type.HTTP, httpProxy.type());
assertEquals(addr, httpProxy.address());
System.out.println("NO_PROXY 类型: " + noProxy.type());
System.out.println("HTTP 代理: " + httpProxy.address());
System.out.println("=== 测试通过 ===");
// 方法体结束
```
