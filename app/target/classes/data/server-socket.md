---
name: ServerSocket
package: java.net
order: 262
---

## 介绍

`java.net.ServerSocket` 是**服务端套接字**，监听客户端连接请求。

## 方法

构造方法：
```java
public ServerSocket(int port) throws IOException
```

### accept

```java
public Socket accept() throws IOException
```

监听并接受连接。

### close / isClosed

关闭/检查状态。

## 测试

- 描述: 创建 ServerSocket 并监听
- 断言: 绑定端口成功

```java
// 方法体开始
System.out.println("=== ServerSocket ===");
ServerSocket server = new ServerSocket(0);  // 随机端口
assertTrue(server.getLocalPort() > 0);
assertFalse(server.isClosed());
System.out.println("ServerSocket 已绑定端口: " + server.getLocalPort());
server.close();
assertTrue(server.isClosed());
System.out.println("ServerSocket 已关闭");
System.out.println("=== 测试通过 ===");
// 方法体结束
```
