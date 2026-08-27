---
name: String
package: java.lang
order: 125
---

## 介绍

`java.lang.String` 类在 Java 8 中新增了多个实用方法，主要是静态的 `join` 方法和 `chars()`/`codePoints()` 流方法。

Java 8 新增的 String 方法：
- `join(CharSequence, CharSequence...)` — 静态方法，用分隔符拼接字符串
- `join(CharSequence, Iterable)` — 用分隔符拼接可迭代对象中的字符串
- `chars()` — 返回字符的 IntStream
- `codePoints()` — 返回 Unicode 码点的 IntStream

## 方法

### join

```java
public static String join(CharSequence delimiter, CharSequence... elements)
public static String join(CharSequence delimiter, Iterable<? extends CharSequence> elements)
```

使用指定的分隔符拼接字符串元素。

- **参数**: `delimiter` — 分隔符；`elements` — 要拼接的元素
- **返回**: `String` — 拼接后的字符串

### chars

```java
public IntStream chars()
```

返回字符串中字符（char 值）的 IntStream。

- **返回**: `IntStream` — 字符码点流

### codePoints

```java
public IntStream codePoints()
```

返回字符串中 Unicode 码点的 IntStream。与 `chars()` 不同，它能正确处理增补字符（如 emoji）。

- **返回**: `IntStream` — Unicode 码点流

## 测试

### join 可变参数

- 描述: 使用 `join` 拼接多个字符串
- 断言: 拼接结果正确

```java
// 方法体开始
System.out.println("=== join ===");
String result = String.join("-", "a", "b", "c");
assertEquals("a-b-c", result);
result = String.join(", ", "apple", "banana", "orange");
assertEquals("apple, banana, orange", result);
System.out.println("join: " + String.join(" / ", "A", "B", "C"));
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### join Iterable

- 描述: 使用 `join` 拼接列表
- 断言: 拼接结果正确

```java
// 方法体开始
System.out.println("=== join Iterable ===");
List<String> list = Arrays.asList("Java", "Python", "Go");
String result = String.join(" | ", list);
assertEquals("Java | Python | Go", result);
System.out.println("join list: " + result);
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### chars

- 描述: 使用 `chars()` 获取字符流
- 断言: 统计字符数和过滤字母

```java
// 方法体开始
System.out.println("=== chars ===");
String s = "Hello";
long count = s.chars().count();
assertEquals(5, count);
// 过滤出大写字母
List<Character> upper = s.chars()
        .filter(Character::isUpperCase)
        .mapToObj(c -> (char) c)
        .collect(Collectors.toList());
assertEquals(1, upper.size());
assertEquals('H', (char) upper.get(0).charValue());
System.out.println("大写字母: " + upper);
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### codePoints

- 描述: 使用 `codePoints()` 处理 Unicode 字符串
- 断言: 正确处理增补字符

```java
// 方法体开始
System.out.println("=== codePoints ===");
String emoji = "A🚀B";
long charCount = emoji.chars().count();    // chars 将 🚀 拆为两个 char
long codePointCount = emoji.codePoints().count(); // codePoints 正确识别为一个
assertEquals(4, charCount);   // 'A' + 高代理 + 低代理 + 'B'
assertEquals(3, codePointCount); // 'A' + 🚀 + 'B'
System.out.println("chars count: " + charCount + ", codePoints count: " + codePointCount);
// 码点转字符串
String collected = emoji.codePoints()
        .mapToObj(Character::toChars)
        .map(String::valueOf)
        .collect(Collectors.joining());
assertEquals(emoji, collected);
System.out.println("原始: " + emoji + ", 重建: " + collected);
System.out.println("=== 测试通过 ===");
// 方法体结束
```
