---
name: StringBuilder
package: java.lang
order: 159
---

## 介绍

`java.lang.StringBuilder` 在 Java 8 中新增了 **`chars()`** 和 **`codePoints()`** 方法，使其可像 String 一样获得字符流。

Java 8 新增的方法：
- `chars()` — 返回字符的 IntStream
- `codePoints()` — 返回 Unicode 码点的 IntStream

## 方法

### chars

```java
public IntStream chars()
```

返回字符的 IntStream（0 到 65535 的 char 值）。

### codePoints

```java
public IntStream codePoints()
```

返回 Unicode 码点的 IntStream。

## 测试

### chars

- 描述: 从 StringBuilder 获取字符流
- 断言: 字符流操作正确

```java
// 方法体开始
System.out.println("=== chars ===");
StringBuilder sb = new StringBuilder("Hello");
long count = sb.chars().count();
assertEquals(5, count);
List<Integer> chars = sb.chars().boxed().collect(Collectors.toList());
assertEquals(5, chars.size());
System.out.println("字符数: " + count);
System.out.println("=== 测试通过 ===");
// 方法体结束
```
