---
name: URLDecoder
package: java.net
order: 272
---

## 介绍

`java.net.URLDecoder` 是 **URL 解码工具类**，将 URL 编码格式的字符串解码回原始字符串。

## 方法

### decode

```java
public static String decode(String s, String enc) throws UnsupportedEncodingException
```

将 URL 编码字符串解码。

## 测试

- 描述: 编码和解码 URL 字符串
- 断言: 解码后与原文一致

```java
// 方法体开始
System.out.println("=== URLDecoder ===");
String original = "Hello World! + Java 8";
String encoded = URLEncoder.encode(original, "UTF-8");
String decoded = URLDecoder.decode(encoded, "UTF-8");
assertEquals(original, decoded);
System.out.println("原始: " + original);
System.out.println("编码: " + encoded);
System.out.println("解码: " + decoded);
System.out.println("=== 测试通过 ===");
// 方法体结束
```
