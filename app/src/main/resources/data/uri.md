---
name: URI
package: java.net
order: 213
---

## 介绍

`java.net.URI` 是**统一资源标识符**类，用于解析和操作 URI。Java 8 中 URI 可以配合 Stream 和 Files API 使用。

## 方法

构造方法：
```java
public URI(String str) throws URISyntaxException
```

### getScheme / getHost / getPath / getQuery

```java
public String getScheme()
public String getHost()
public String getPath()
public String getQuery()
```

### toURL

```java
public URL toURL() throws MalformedURLException
```

## 测试

- 描述: 解析 URI 获取各组件
- 断言: 解析结果正确

```java
// 方法体开始
System.out.println("=== URI ===");
URI uri = new URI("https://example.com:8080/path/to/file?name=test#section");
assertEquals("https", uri.getScheme());
assertEquals("example.com", uri.getHost());
assertEquals(8080, uri.getPort());
assertEquals("/path/to/file", uri.getPath());
assertEquals("name=test", uri.getQuery());
assertEquals("section", uri.getFragment());
System.out.println("URI: " + uri);
System.out.println("=== 测试通过 ===");
// 方法体结束
```
