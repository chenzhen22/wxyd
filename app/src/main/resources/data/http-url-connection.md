---
name: HttpURLConnection
package: java.net
order: 220
---

## 介绍

`java.net.HttpURLConnection` 是 **HTTP 连接类**，用于发送 HTTP 请求和接收响应。

## 方法

### setRequestMethod

```java
public void setRequestMethod(String method) throws ProtocolException
```

设置请求方法：GET、POST、PUT、DELETE 等。

### getResponseCode / getResponseMessage

```java
public int getResponseCode() throws IOException
```

获取响应状态码和消息。

### setRequestProperty / getHeaderField

设置请求头 / 获取响应头。

### connect / disconnect

连接/断开。

## 测试

- 描述: 创建 HTTP 连接并获取信息
- 断言: 连接设置正确

```java
// 方法体开始
System.out.println("=== HttpURLConnection ===");
URL url = new URL("http://localhost:8080/api/classes");
HttpURLConnection conn = (HttpURLConnection) url.openConnection();
conn.setRequestMethod("GET");
conn.setConnectTimeout(3000);
conn.setReadTimeout(3000);
assertEquals("GET", conn.getRequestMethod());
System.out.println("HTTP 连接创建成功");
conn.disconnect();
System.out.println("=== 测试通过 ===");
// 方法体结束
```
