---
name: URL
package: java.net
order: 214
---

## 介绍

`java.net.URL` 是**统一资源定位符**类，指向网络资源。Java 8 中 URL 可以配合 Stream 读取远程资源内容。

## 方法

构造方法：
```java
public URL(String spec) throws MalformedURLException
```

### openStream

```java
public final InputStream openStream() throws IOException
```

打开到此 URL 的连接流。

### getContent / openConnection

获取内容 / 打开连接。

## 测试

- 描述: 创建 URL 并获取组件
- 断言: 解析正确

```java
// 方法体开始
System.out.println("=== URL ===");
URL url = new URL("https://example.com/index.html");
assertEquals("https", url.getProtocol());
assertEquals("example.com", url.getHost());
assertEquals("/index.html", url.getPath());
System.out.println("URL: " + url);
System.out.println("=== 测试通过 ===");
// 方法体结束
```
