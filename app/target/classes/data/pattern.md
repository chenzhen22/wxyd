---
name: Pattern
package: java.util.regex
order: 43
---

## 介绍

`java.util.regex.Pattern` 是 Java 正则表达式的编译表示类。将正则表达式字符串编译为 `Pattern` 对象，再用它创建 `Matcher` 来匹配输入字符串。

常见用途：
- **匹配验证**：`matches()` 判断是否完全匹配
- **查找提取**：`find()` 查找子串、`group()` 提取分组
- **分割替换**：`split()` 分割字符串、`replaceAll()` 替换

## 方法

### compile

```java
public static java.util.regex.Pattern compile(String regex)
```

将正则表达式编译为 Pattern。

- **参数**: `regex` — 正则表达式
- **返回**: `Pattern`

### compile(String, int)

```java
public static java.util.regex.Pattern compile(String regex, int flags)
```

编译正则表达式，指定匹配标志。

- **参数**: `regex` — 正则表达式；`flags` — 标志位
- **返回**: `Pattern`

### matcher

```java
public Matcher matcher(CharSequence input)
```

创建匹配器，用于匹配输入字符串。

- **参数**: `input` — 输入字符串
- **返回**: `Matcher`

### matches

```java
public static boolean matches(String regex, CharSequence input)
```

静态方法，判断字符串是否完全匹配正则表达式。

- **参数**: `regex` — 正则表达式；`input` — 输入字符串
- **返回**: `boolean`

### split

```java
public String[] split(CharSequence input)
```

按正则表达式分割字符串。

- **参数**: `input` — 输入字符串
- **返回**: `String[]`

### split(CharSequence, int)

```java
public String[] split(CharSequence input, int limit)
```

按正则表达式分割字符串，指定分割次数限制。

- **参数**: `input` — 输入字符串；`limit` — 限制
- **返回**: `String[]`

### quote

```java
public static String quote(String s)
```

转义字符串中的特殊字符，使其被当作字面量匹配。

- **参数**: `s` — 字符串
- **返回**: `String`

### splitAsStream

```java
public Stream<String> splitAsStream(CharSequence input)
```

根据正则表达式分割输入序列，返回 Stream（Java 8 新增）。

- **参数**: `input` — 要分割的字符序列
- **返回**: `Stream<String>` — 分割后的字符串流

## 测试

### compile

- 描述: 编译正则表达式
- 断言: java.util.regex.Pattern 不为 null

```java
// 方法体开始
System.out.println("=== compile ===");
java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("\\d+");
assertNotNull(pattern);
System.out.println("正则: \\\\d+, pattern: " + pattern);
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### matches

- 描述: 完全匹配
- 断言: 匹配成功返回 true

```java
// 方法体开始
System.out.println("=== matches ===");
boolean result = java.util.regex.Pattern.matches("\\d+", "12345");
assertTrue(result);
assertFalse(java.util.regex.Pattern.matches("\\d+", "abc"));
System.out.println("12345 匹配数字: " + result);
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### matcher

- 描述: 使用 Matcher 查找子串
- 断言: find 找到匹配子串

```java
// 方法体开始
System.out.println("=== matcher ===");
java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("\\d+");
java.util.regex.Matcher matcher = pattern.matcher("abc123def456");
assertTrue(matcher.find());
assertEquals("123", matcher.group());
assertTrue(matcher.find());
assertEquals("456", matcher.group());
System.out.println("找到数字: 123, 456");
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### split

- 描述: 分割字符串
- 断言: 分割结果正确

```java
// 方法体开始
System.out.println("=== split ===");
java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(",");
String[] parts = pattern.split("a,b,c");
assertArrayEquals(new String[]{"a", "b", "c"}, parts);
System.out.println("分割结果: " + java.util.Arrays.toString(parts));
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### splitWithLimit

- 描述: 限制分割次数
- 断言: 分割次数有限

```java
// 方法体开始
System.out.println("=== splitWithLimit ===");
java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(",");
String[] parts = pattern.split("a,b,c,d", 3);
System.out.println("限制3次分割: " + java.util.Arrays.toString(parts));
assertEquals(3, parts.length);
assertEquals("a", parts[0]);
assertEquals("b", parts[1]);
assertEquals("c,d", parts[2]);
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### quote

- 描述: 转义特殊字符
- 断言: 转义后作为字面量匹配

```java
// 方法体开始
System.out.println("=== quote ===");
String literal = java.util.regex.Pattern.quote("$5.00");
System.out.println("转义后: " + literal);
assertTrue(java.util.regex.Pattern.matches(literal, "$5.00"));
assertFalse(java.util.regex.Pattern.matches(literal, "100"));
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### flags

- 描述: 使用标志位编译
- 断言: CASE_INSENSITIVE 忽略大小写

```java
// 方法体开始
System.out.println("=== flags ===");
java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("hello", java.util.regex.Pattern.CASE_INSENSITIVE);
assertTrue(pattern.matcher("Hello").matches());
assertTrue(pattern.matcher("HELLO").matches());
System.out.println("CASE_INSENSITIVE 匹配 Hello: true");
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### splitAsStream

- 描述: 使用 splitAsStream 分割字符串并流式处理
- 断言: 正确分割为各部分

```java
// 方法体开始
System.out.println("=== splitAsStream ===");
java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(",");
java.util.List<String> parts = pattern.splitAsStream("a,b,c,d")
        .map(String::trim)
        .collect(java.util.stream.Collectors.toList());
assertEquals(4, parts.size());
assertEquals("a", parts.get(0));
assertEquals("d", parts.get(3));
System.out.println("分割结果: " + parts);
System.out.println("=== 测试通过 ===");
// 方法体结束
```
