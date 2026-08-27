---
name: Predicate
package: java.util.function
order: 25
---

## 介绍

`Predicate<T>` 是 Java 8 引入的一个**函数式接口**，代表一个布尔值函数（真值判断）。它接受一个参数，返回 `boolean` 值。

Predicate 常用于：
- **集合筛选**：配合 Stream 的 `filter` 方法
- **条件组合**：通过 `and`、`or`、`negate` 组合多个条件
- **对象匹配**：用 `test` 方法判断对象是否满足条件

Predicate 的四个基本方法：
- `test(T t)` — 核心方法，执行判断
- `and(Predicate)` — 与操作，两个条件同时满足
- `or(Predicate)` — 或操作，两个条件满足其一
- `negate()` — 非操作，取反
- `isEqual(Object)` — 静态方法，判断是否相等

## 方法

### test

```java
boolean test(T t)
```

对给定参数执行判断，返回 `boolean` 值。

- **参数**: `t` — 要判断的参数
- **返回**: `boolean` — 判断结果

### and

```java
default Predicate<T> and(Predicate<? super T> other)
```

返回一个组合 Predicate，当前 Predicate 与 `other` 都返回 `true` 时才返回 `true`。

- **参数**: `other` — 另一个 Predicate
- **返回**: `Predicate<T>` — 组合后的 Predicate

### or

```java
default Predicate<T> or(Predicate<? super T> other)
```

返回一个组合 Predicate，当前 Predicate 或 `other` 任一返回 `true` 就返回 `true`。

- **参数**: `other` — 另一个 Predicate
- **返回**: `Predicate<T>` — 组合后的 Predicate

### negate

```java
default Predicate<T> negate()
```

返回当前 Predicate 的逻辑取反。

- **返回**: `Predicate<T>` — 取反后的 Predicate

### isEqual

```java
static <T> Predicate<T> isEqual(Object targetRef)
```

返回一个 Predicate，用于判断输入对象是否与 `targetRef` 相等（使用 `equals` 方法）。

- **参数**: `targetRef` — 参照对象
- **返回**: `Predicate<T>` — 相等判断 Predicate

## 测试

### test

- 描述: 使用 `test` 方法判断字符串是否为空
- 断言: `"".isEmpty()` 返回 true，`"hello".isEmpty()` 返回 false

```java
// 方法体开始
System.out.println("=== test ===");
Predicate<String> isEmpty = String::isEmpty;
assertTrue(isEmpty.test(""));
assertFalse(isEmpty.test("hello"));
System.out.println("空字符串判断: " + isEmpty.test(""));
System.out.println("非空字符串判断: " + isEmpty.test("hello"));
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### and

- 描述: 使用 `and` 组合两个 Predicate，两个条件都满足时返回 true
- 断言: 长度大于 3 且包含 "h" 的字符串返回 true

```java
// 方法体开始
System.out.println("=== and ===");
Predicate<String> lengthGT3 = s -> s.length() > 3;
Predicate<String> containsH = s -> s.contains("h");
Predicate<String> both = lengthGT3.and(containsH);
assertTrue(both.test("hello"));
assertFalse(both.test("hi"));
assertFalse(both.test("world"));
System.out.println("\"hello\" and: " + both.test("hello"));
System.out.println("\"hi\" and: " + both.test("hi"));
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### or

- 描述: 使用 `or` 组合两个 Predicate，任一条件满足时返回 true
- 断言: 长度小于 3 或包含 "x" 的字符串返回 true

```java
// 方法体开始
System.out.println("=== or ===");
Predicate<String> lengthLT3 = s -> s.length() < 3;
Predicate<String> containsX = s -> s.contains("x");
Predicate<String> either = lengthLT3.or(containsX);
assertTrue(either.test("hi"));
assertTrue(either.test("xylophone"));
assertFalse(either.test("hello"));
System.out.println("\"hi\" or: " + either.test("hi"));
System.out.println("\"xylophone\" or: " + either.test("xylophone"));
System.out.println("\"hello\" or: " + either.test("hello"));
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### negate

- 描述: 使用 `negate` 对 Predicate 取反
- 断言: 空字符串判断取反后，非空返回 true

```java
// 方法体开始
System.out.println("=== negate ===");
Predicate<String> isEmpty = String::isEmpty;
Predicate<String> isNotEmpty = isEmpty.negate();
assertFalse(isNotEmpty.test(""));
assertTrue(isNotEmpty.test("hello"));
System.out.println("非空判断 \"\": " + isNotEmpty.test(""));
System.out.println("非空判断 \"hello\": " + isNotEmpty.test("hello"));
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### isEqual

- 描述: 使用 `isEqual` 静态方法判断相等
- 断言: 与目标值相等的对象返回 true，不等返回 false

```java
// 方法体开始
System.out.println("=== isEqual ===");
Predicate<String> isHello = Predicate.isEqual("hello");
assertTrue(isHello.test("hello"));
assertFalse(isHello.test("world"));
assertFalse(isHello.test(null));
System.out.println("isEqual(\"hello\") 判断 \"hello\": " + isHello.test("hello"));
System.out.println("isEqual(\"hello\") 判断 \"world\": " + isHello.test("world"));
System.out.println("=== 测试通过 ===");
// 方法体结束
```
