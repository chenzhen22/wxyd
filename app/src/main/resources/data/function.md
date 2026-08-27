---
name: Function
package: java.util.function
order: 26
---

## 介绍

`Function<T, R>` 是 Java 8 引入的一个**函数式接口**，代表一个接受一个参数并返回结果的函数。它将输入对象 `T` 转换为输出对象 `R`。

Function 的核心特点：
- **类型转换**：将一个类型的值转换为另一个类型
- **链式组合**：通过 `andThen` 和 `compose` 组合多个转换
- **恒等函数**：`identity()` 返回原样返回输入的函数

Function 的四个方法：
- `apply(T t)` — 核心方法，对参数执行转换
- `andThen(Function)` — 先执行当前函数，再执行后一个函数
- `compose(Function)` — 先执行前一个函数，再执行当前函数
- `identity()` — 静态方法，返回输入本身

## 方法

### apply

```java
R apply(T t)
```

对给定参数执行转换，返回结果。

- **参数**: `t` — 输入参数
- **返回**: `R` — 转换结果

### andThen

```java
default <V> Function<T, V> andThen(Function<? super R, ? extends V> after)
```

返回一个组合 Function，先应用当前函数，再对结果应用 `after` 函数。

- **参数**: `after` — 后执行的函数
- **返回**: `Function<T, V>` — 组合后的函数

### compose

```java
default <V> Function<V, R> compose(Function<? super V, ? extends T> before)
```

返回一个组合 Function，先应用 `before` 函数，再对结果应用当前函数。

- **参数**: `before` — 先执行的函数
- **返回**: `Function<V, R>` — 组合后的函数

### identity

```java
static <T> Function<T, T> identity()
```

返回一个恒等函数，即输入什么就返回什么。

- **返回**: `Function<T, T>` — 恒等函数

## 测试

### apply

- 描述: 使用 `apply` 方法将字符串转换为其长度
- 断言: `"hello"` 的长度为 5

```java
// 方法体开始
System.out.println("=== apply ===");
Function<String, Integer> toLen = String::length;
assertEquals(Integer.valueOf(5), toLen.apply("hello"));
assertEquals(Integer.valueOf(0), toLen.apply(""));
System.out.println("\"hello\" 长度: " + toLen.apply("hello"));
System.out.println("空字符串长度: " + toLen.apply(""));
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### andThen

- 描述: 使用 `andThen` 组合两个 Function，先取长度再翻倍
- 断言: `"hello"` 长度 5 翻倍为 10

```java
// 方法体开始
System.out.println("=== andThen ===");
Function<String, Integer> toLen = String::length;
Function<Integer, Integer> doubleIt = x -> x * 2;
Function<String, Integer> composed = toLen.andThen(doubleIt);
assertEquals(Integer.valueOf(10), composed.apply("hello"));
assertEquals(Integer.valueOf(0), composed.apply(""));
System.out.println("\"hello\" 长度翻倍: " + composed.apply("hello"));
System.out.println("空字符串长度翻倍: " + composed.apply(""));
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### compose

- 描述: 使用 `compose` 组合两个 Function，先执行括号内的函数
- 断言: 先对整数加 1，再翻倍，`(5+1)*2=12`

```java
// 方法体开始
System.out.println("=== compose ===");
Function<Integer, Integer> doubleIt = x -> x * 2;
Function<Integer, Integer> addOne = x -> x + 1;
Function<Integer, Integer> composed = doubleIt.compose(addOne);
assertEquals(Integer.valueOf(12), composed.apply(5));
assertEquals(Integer.valueOf(2), composed.apply(0));
System.out.println("(5+1)*2: " + composed.apply(5));
System.out.println("(0+1)*2: " + composed.apply(0));
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### identity

- 描述: 使用 `identity` 返回输入本身
- 断言: 输入什么就返回什么

```java
// 方法体开始
System.out.println("=== identity ===");
Function<String, String> id = Function.identity();
assertEquals("hello", id.apply("hello"));
assertEquals("", id.apply(""));
assertNull(id.apply(null));
System.out.println("identity(\"hello\"): " + id.apply("hello"));
System.out.println("identity(\"\"): " + id.apply(""));
System.out.println("=== 测试通过 ===");
// 方法体结束
```
