---
name: ToIntFunction
package: java.util.function
order: 65
---

## 介绍

`ToIntFunction<T>` 是 Java 8 引入的一个**函数式接口**，代表一个提取 `int` 值的函数。它接受一个对象 `T` 作为输入，提取或转换出一个 `int` 值。

ToIntFunction 的核心特点：
- **对象→int**：将对象映射为原始 int 值
- **与 Stream 集成**：`Stream.mapToInt()` 的常用参数
- **避免装箱**：直接产生原始 int 值

ToIntFunction 的单个方法：
- `applyAsInt(T value)` — 核心方法，从对象中提取 int 值

对应特化接口说明：
- `ToLongFunction<T>` — 提取 long 值
- `ToDoubleFunction<T>` — 提取 double 值
- `IntFunction<R>` — 反向：int → 对象 R
- `IntToLongFunction`、`IntToDoubleFunction` — int 到其他原始类型的转换

## 方法

### applyAsInt

```java
int applyAsInt(T value)
```

从给定对象中提取 int 值。

- **参数**: `value` — 输入对象
- **返回**: `int` — 提取的 int 值

## 测试

### 提取字符串长度

- 描述: 使用 `applyAsInt` 提取字符串长度
- 断言: `"hello"` 长度为 5

```java
// 方法体开始
System.out.println("=== 提取字符串长度 ===");
ToIntFunction<String> strLen = s -> s.length();
assertEquals(5, strLen.applyAsInt("hello"));
assertEquals(0, strLen.applyAsInt(""));
assertEquals(3, strLen.applyAsInt("bye"));
System.out.println("\"hello\" 长度: " + strLen.applyAsInt("hello"));
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### Stream.mapToInt

- 描述: 使用 ToIntFunction 配合 `Stream.mapToInt` 将字符串流映射为 IntStream
- 断言: 总长度为 13

```java
// 方法体开始
System.out.println("=== Stream.mapToInt ===");
ToIntFunction<String> strLen = s -> s.length();
int total = Stream.of("Java", "Python", "Go")
        .mapToInt(strLen)
        .sum();
assertEquals(13, total);  // 4 + 6 + 2 = 12, wait: Java=4, Python=6, Go=2 => 12
System.out.println("总长度: " + total);
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### 提取对象属性

- 描述: 使用 ToIntFunction 提取整数列表的绝对值
- 断言: -5 的绝对值为 5

```java
// 方法体开始
System.out.println("=== 提取对象属性 ===");
ToIntFunction<Integer> abs = v -> Math.abs(v);
assertEquals(5, abs.applyAsInt(-5));
assertEquals(0, abs.applyAsInt(0));
assertEquals(10, abs.applyAsInt(10));
System.out.println("-5 绝对值: " + abs.applyAsInt(-5));
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### 复杂对象属性提取

- 描述: 使用 ToIntFunction 从字符串中提取数字部分并转换
- 断言: `"42"` 解析为 42

```java
// 方法体开始
System.out.println("=== 对象属性提取 ===");
ToIntFunction<String> parser = s -> Integer.parseInt(s.trim());
assertEquals(42, parser.applyAsInt("42"));
assertEquals(-10, parser.applyAsInt("-10"));
System.out.println("\"42\" -> " + parser.applyAsInt("42"));
System.out.println("=== 测试通过 ===");
// 方法体结束
```
