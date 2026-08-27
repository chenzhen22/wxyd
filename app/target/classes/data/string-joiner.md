---
name: StringJoiner
package: java.util
order: 17
---

## 介绍

`StringJoiner` 是 Java 8 引入的一个实用工具类，用于构造由分隔符分隔的字符序列。它可以选择以提供的前缀开始，并以提供的后缀结束。在内部使用 `StringBuilder` 实现高效拼接。

常见用途：
- **拼接 CSV 行**：使用逗号分隔
- **拼接路径**：使用斜杠或点号分隔
- **带前后缀的拼接**：如 JSON 数组 `[a, b, c]`

## 方法

### StringJoiner(CharSequence delimiter)

```java
public StringJoiner(CharSequence delimiter)
```

构造一个 `StringJoiner`，使用指定的分隔符。

- **参数**: `delimiter` — 分隔符

### StringJoiner(CharSequence delimiter, CharSequence prefix, CharSequence suffix)

```java
public StringJoiner(CharSequence delimiter, CharSequence prefix, CharSequence suffix)
```

构造一个 `StringJoiner`，使用指定的分隔符、前缀和后缀。

- **参数**: `delimiter` — 分隔符；`prefix` — 前缀；`suffix` — 后缀

### add(CharSequence)

```java
public StringJoiner add(CharSequence newElement)
```

添加一个新的元素。

- **参数**: `newElement` — 新元素
- **返回**: `StringJoiner` — 当前 StringJoiner 实例（链式调用）

### merge(StringJoiner)

```java
public StringJoiner merge(StringJoiner other)
```

合并另一个 `StringJoiner` 的内容。

- **参数**: `other` — 另一个 StringJoiner
- **返回**: `StringJoiner` — 当前 StringJoiner 实例

### setEmptyValue(CharSequence)

```java
public StringJoiner setEmptyValue(CharSequence emptyValue)
```

当没有元素时，设置返回的默认值。

- **参数**: `emptyValue` — 空值时的默认字符串
- **返回**: `StringJoiner` — 当前 StringJoiner 实例

### toString

```java
public String toString()
```

返回当前拼接的字符串。如果没有元素，返回空值（如果设置了）或空字符串。

- **返回**: `String`

### length

```java
public int length()
```

返回当前字符串的长度。

- **返回**: `int`

## 测试

### add

- 描述: 基本元素添加和拼接
- 断言: 用逗号拼接 "a", "b", "c" 结果为 "a,b,c"

```java
// 方法体开始
System.out.println("=== add ===");
StringJoiner joiner = new StringJoiner(",");
joiner.add("a");
joiner.add("b");
joiner.add("c");
String result = joiner.toString();
System.out.println("拼接结果: " + result);
assertEquals("a,b,c", result);
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### StringJoiner(CharSequence)

- 描述: 基本构造器（单分隔符）
- 断言: 无元素时返回空字符串

```java
// 方法体开始
System.out.println("=== StringJoiner(CharSequence) ===");
StringJoiner joiner = new StringJoiner(",");
assertEquals("", joiner.toString());
System.out.println("空 StringJoiner: '" + joiner.toString() + "'");
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### StringJoiner(3)

- 描述: 带前后缀的构造器
- 断言: 无元素时返回前缀和后缀

```java
// 方法体开始
System.out.println("=== StringJoiner(3) ===");
StringJoiner joiner = new StringJoiner(", ", "[", "]");
assertEquals("[]", joiner.toString());
System.out.println("结果: " + joiner.toString());
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### toString

- 描述: 测试 toString 输出
- 断言: 拼接后 toString 返回正确字符串

```java
// 方法体开始
System.out.println("=== toString ===");
StringJoiner joiner = new StringJoiner("-");
joiner.add("x");
joiner.add("y");
String result = joiner.toString();
assertEquals("x-y", result);
System.out.println("toString 结果: " + result);
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### merge

- 描述: 合并两个 StringJoiner
- 断言: 合并后包含两个 joiner 的元素

```java
// 方法体开始
System.out.println("=== merge ===");
StringJoiner joiner1 = new StringJoiner(",");
joiner1.add("a");
joiner1.add("b");
StringJoiner joiner2 = new StringJoiner(",");
joiner2.add("c");
joiner2.add("d");
StringJoiner merged = joiner1.merge(joiner2);
System.out.println("合并结果: " + merged.toString());
assertEquals("a,b,c,d", merged.toString());
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### setEmptyValue

- 描述: 设置空值默认字符串
- 断言: 无元素时返回设置的默认值

```java
// 方法体开始
System.out.println("=== setEmptyValue ===");
StringJoiner joiner = new StringJoiner(",");
joiner.setEmptyValue("empty");
assertEquals("empty", joiner.toString());
joiner.add("a");
assertEquals("a", joiner.toString());
System.out.println("空时: empty, 添加后: " + joiner.toString());
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### length

- 描述: 获取拼接字符串长度
- 断言: "a,b,c" 长度为 5

```java
// 方法体开始
System.out.println("=== length ===");
StringJoiner joiner = new StringJoiner(",");
assertEquals(0, joiner.length());
joiner.add("a");
joiner.add("b");
joiner.add("c");
System.out.println("拼接结果: " + joiner.toString() + ", 长度: " + joiner.length());
assertEquals(5, joiner.length());
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### chain

- 描述: 链式调用
- 断言: add 返回 StringJoiner 实例支持链式调用

```java
// 方法体开始
System.out.println("=== chain ===");
String result = new StringJoiner("-")
    .add("x")
    .add("y")
    .add("z")
    .toString();
System.out.println("链式调用结果: " + result);
assertEquals("x-y-z", result);
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### singleElement

- 描述: 单个元素拼接
- 断言: 只有一个元素时不加分隔符

```java
// 方法体开始
System.out.println("=== singleElement ===");
StringJoiner joiner = new StringJoiner(",");
joiner.add("only");
assertEquals("only", joiner.toString());
System.out.println("单个元素: " + joiner.toString());
System.out.println("=== 测试通过 ===");
// 方法体结束
```
