---
name: BiPredicate
package: java.util.function
order: 63
---

## 介绍

`BiPredicate<T, U>` 是 Java 8 引入的一个**函数式接口**，代表一个接受两个参数并返回 `boolean` 值条件的函数。它是 `Predicate` 的"双参数版本"，常用于双值判断和过滤。

BiPredicate 的核心特点：
- **双参数判断**：接受两个输入，返回 boolean 结果
- **链式组合**：通过 `and`、`or`、`negate` 组合多个条件
- **无 identity**：与 Predicate 不同，没有静态 `isEqual` 方法

BiPredicate 的三个默认方法和一个静态方法：
- `test(T t, U u)` — 核心方法，对两个参数执行判断
- `and(BiPredicate)` — 且条件组合
- `or(BiPredicate)` — 或条件组合
- `negate()` — 取反

最常见的 BiPredicate 用法是 `Map.Entry.comparingByValue`、流式操作的双值比较等场景。

## 方法

### test

```java
boolean test(T t, U u)
```

对给定两个参数执行条件判断。

- **参数**: `t` — 第一个输入参数；`u` — 第二个输入参数
- **返回**: `boolean` — 判断结果

### and

```java
default BiPredicate<T, U> and(BiPredicate<? super T, ? super U> other)
```

返回一个组合 BiPredicate，当前判断和 `other` 判断都为 true 才返回 true。

- **参数**: `other` — 另一个 BiPredicate
- **返回**: `BiPredicate<T, U>` — 且条件组合

### or

```java
default BiPredicate<T, U> or(BiPredicate<? super T, ? super U> other)
```

返回一个组合 BiPredicate，当前判断或 `other` 判断任一为 true 就返回 true。

- **参数**: `other` — 另一个 BiPredicate
- **返回**: `BiPredicate<T, U>` — 或条件组合

### negate

```java
default BiPredicate<T, U> negate()
```

返回当前 BiPredicate 的取反。

- **返回**: `BiPredicate<T, U>` — 取反后的 BiPredicate

## 测试

### test 基本判断

- 描述: 使用 `test` 方法判断两个字符串是否具有包含关系
- 断言: `"hello"` 包含 `"ll"`，但 `"hello"` 不包含 `"x"`

```java
// 方法体开始
System.out.println("=== test 基本判断 ===");
BiPredicate<String, String> contains = (str, sub) -> str.contains(sub);
assertTrue(contains.test("hello", "ll"));
assertFalse(contains.test("hello", "x"));
System.out.println("\"hello\" 包含 \"ll\": " + contains.test("hello", "ll"));
System.out.println("\"hello\" 包含 \"x\": " + contains.test("hello", "x"));
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### and

- 描述: 使用 `and` 组合两个条件，字符串既达到长度又包含特定字符
- 断言: `"hello"` 长度 > 3 且包含 `"ll"` 为 true

```java
// 方法体开始
System.out.println("=== and ===");
BiPredicate<String, Integer> longerThan = (str, len) -> str.length() > len;
BiPredicate<String, Integer> containsChar = (str, c) -> str.contains(String.valueOf((char)c.intValue()));
BiPredicate<String, Integer> combined = longerThan.and(containsChar);
assertTrue(combined.test("hello", 3));  // 长度 > 3 且包含 'l'
assertFalse(combined.test("hi", 3));    // 长度 <= 3
System.out.println("\"hello\" 长度 > 3 且包含 l: " + combined.test("hello", 3));
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### or

- 描述: 使用 `or` 组合条件，字符串为空或长度超过限制
- 断言: `""` 或 `"hello world"` 任一满足条件

```java
// 方法体开始
System.out.println("=== or ===");
BiPredicate<String, Integer> isEmpty = (str, limit) -> str.isEmpty();
BiPredicate<String, Integer> exceedsLimit = (str, limit) -> str.length() > limit;
BiPredicate<String, Integer> combined = isEmpty.or(exceedsLimit);
assertTrue(combined.test("", 10));
assertTrue(combined.test("hello world!!!", 10));
assertFalse(combined.test("hello", 10));
System.out.println("空字符串: " + combined.test("", 10));
System.out.println("\"hello world!!!\" 超过 10: " + combined.test("hello world!!!", 10));
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### negate

- 描述: 使用 `negate` 取反一个 BiPredicate
- 断言: 取反后 "hi" 长度 > 5 的判断变为 false

```java
// 方法体开始
System.out.println("=== negate ===");
BiPredicate<String, Integer> longerThan = (str, len) -> str.length() > len;
BiPredicate<String, Integer> notLongerThan = longerThan.negate();
assertTrue(notLongerThan.test("hi", 5));   // 不 > 5，取反后为 true
assertFalse(longerThan.test("hi", 5));     // 原始为 false
System.out.println("\"hi\" 长度不大于 5: " + notLongerThan.test("hi", 5));
System.out.println("=== 测试通过 ===");
// 方法体结束
```
