---
name: StringBuffer
package: java.lang
order: 166
---

## 介绍

`java.lang.StringBuffer` 是**线程安全的可变字符串**。Java 8 中新增了 `chars()` 和 `codePoints()` 方法，与 StringBuilder 和 String 保持一致。

## 方法

### chars

```java
public IntStream chars()
```

返回字符的 IntStream。

### codePoints

```java
public IntStream codePoints()
```

返回 Unicode 码点的 IntStream。

## 测试

### chars

- 描述: 从 StringBuffer 获取字符流
- 断言: 字符流操作正确

```java
// 方法体开始
System.out.println("=== chars ===");
StringBuffer sb = new StringBuffer("Java");
long count = sb.chars().count();
assertEquals(4, count);
List<Integer> chars = sb.chars().boxed().collect(Collectors.toList());
assertEquals(4, chars.size());
System.out.println("字符数: " + count);
System.out.println("=== 测试通过 ===");
// 方法体结束
```
