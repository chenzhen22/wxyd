---
name: Charset
package: java.nio.charset
order: 348
---

## 介绍

`java.nio.charset.Charset` 是**字符集类**，用于字符编码和解码。

## 方法

### forName

```java
public static Charset forName(String charsetName)
```

### availableCharsets

```java
public static SortedMap<String, Charset> availableCharsets()
```

### encode / decode

## 测试

- 描述: 字符集编解码
- 断言: 编解码正确

```java
// 方法体开始
System.out.println("=== Charset ===");
Charset utf8 = Charset.forName("UTF-8");
ByteBuffer bb = utf8.encode("你好 Java 8");
String decoded = utf8.decode(bb).toString();
assertEquals("你好 Java 8", decoded);
SortedMap<String, Charset> charsets = Charset.availableCharsets();
assertTrue(charsets.containsKey("UTF-8"));
assertTrue(charsets.containsKey("GBK"));
System.out.println("可用字符集数: " + charsets.size());
System.out.println("=== 测试通过 ===");
// 方法体结束
```
