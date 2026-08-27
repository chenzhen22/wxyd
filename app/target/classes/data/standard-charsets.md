---
name: StandardCharsets
package: java.nio.charset
order: 134
---

## 介绍

`java.nio.charset.StandardCharsets` 是 Java 7 引入的**字符集常量类**，包含常用字符集的静态常量。在 Java 8 的 Files、String 等 API 中广泛使用，是 Lambda 和 Stream 中避免受检异常的好帮手。

## 常量

```java
public static final Charset US_ASCII
public static final Charset ISO_8859_1
public static final Charset UTF_8
public static final Charset UTF_16
public static final Charset UTF_16BE
public static final Charset UTF_16LE
```

## 测试

### UTF_8 常量

- 描述: 使用 StandardCharsets.UTF_8
- 断言: 与 Charset.forName("UTF-8") 等价

```java
// 方法体开始
System.out.println("=== UTF_8 ===");
assertEquals(Charset.forName("UTF-8"), StandardCharsets.UTF_8);
byte[] bytes = "Hello".getBytes(StandardCharsets.UTF_8);
String decoded = new String(bytes, StandardCharsets.UTF_8);
assertEquals("Hello", decoded);
System.out.println("UTF-8 编解码正确");
System.out.println("=== 测试通过 ===");
// 方法体结束
```
