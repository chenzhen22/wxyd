---
name: URLEncoder
package: java.net
order: 247
---

## 介绍

`java.net.URLEncoder` 是 **URL 编码工具类**，用于将字符串编码为 `application/x-www-form-urlencoded` MIME 格式。

## 方法

### encode

```java
public static String encode(String s, String enc) throws UnsupportedEncodingException
```

将字符串编码为 URL 编码格式。

## 测试

- 描述: URL 编码和解码
- 断言: 编码后空格变为 +

```java
// 方法体开始
System.out.println("=== URLEncoder ===");
String encoded = URLEncoder.encode("Hello World! Java 8", "UTF-8");
assertTrue(encoded.contains("+"));
System.out.println("编码: " + encoded);
String decoded = URLDecoder.decode(encoded, "UTF-8");
assertEquals("Hello World! Java 8", decoded);
System.out.println("解码: " + decoded);
System.out.println("=== 测试通过 ===");
// 方法体结束
```
