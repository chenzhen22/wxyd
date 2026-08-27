---
name: BiFunction
package: java.util.function
order: 56
---

## 介绍

`BiFunction<T, U, R>` 是 Java 8 引入的一个**函数式接口**，代表一个接受两个参数并返回结果的函数。它是 `Function` 的"双参数版本"，将输入对象 `T` 和 `U` 转换为输出对象 `R`。

BiFunction 的核心特点：
- **双参数转换**：同时接受两个参数参与计算
- **链式组合**：通过 `andThen` 在结果上继续组合另一个 Function
- **与 Map 紧密配合**：`Map.replaceAll`、`Map.compute` 等方法都使用 BiFunction

BiFunction 的三个方法：
- `apply(T t, U u)` — 核心方法，对两个参数执行计算并返回结果
- `andThen(Function)` — 返回先执行 BiFunction，再对结果执行 Function 的组合

另外，`BiFunction<T, U, R>` 有两个特化子接口：
- `BinaryOperator<T>` — 当 `T == U == R` 时的便捷版本
- `ToIntBiFunction<T, U>`、`ToLongBiFunction<T, U>`、`ToDoubleBiFunction<T, U>` — 原始类型特化

## 方法

### apply

```java
R apply(T t, U u)
```

对给定两个参数执行计算，返回结果。

- **参数**: `t` — 第一个输入参数；`u` — 第二个输入参数
- **返回**: `R` — 计算结果

### andThen

```java
default <V> BiFunction<T, U, V> andThen(Function<? super R, ? extends V> after)
```

返回一个组合 BiFunction，先执行当前函数，再对结果应用 `after` Function。如果当前函数抛出异常，`after` 不会执行。

- **参数**: `after` — 结果处理 Function
- **返回**: `BiFunction<T, U, V>` — 组合后的 BiFunction

## 测试

### apply 合并字符串

- 描述: 使用 `apply` 方法将两个字符串合并
- 断言: `"Hello, "` + `"World"` = `"Hello, World"`

```java
// 方法体开始
System.out.println("=== apply 合并字符串 ===");
BiFunction<String, String, String> concat = (a, b) -> a + b;
assertEquals("Hello, World", concat.apply("Hello, ", "World"));
assertEquals("ab", concat.apply("a", "b"));
System.out.println("concat: " + concat.apply("Hello, ", "World"));
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### apply 数值计算

- 描述: 使用 BiFunction 计算矩形面积
- 断言: 宽 5 高 3 面积为 15

```java
// 方法体开始
System.out.println("=== apply 数值计算 ===");
BiFunction<Integer, Integer, Integer> area = (w, h) -> w * h;
assertEquals(Integer.valueOf(15), area.apply(5, 3));
assertEquals(Integer.valueOf(0), area.apply(0, 10));
System.out.println("5 x 3 面积: " + area.apply(5, 3));
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### andThen

- 描述: 使用 `andThen` 先计算矩形面积，再转换为字符串描述
- 断言: 面积 15 描述为 `"面积: 15"`

```java
// 方法体开始
System.out.println("=== andThen ===");
BiFunction<Integer, Integer, Integer> area = (w, h) -> w * h;
Function<Integer, String> describe = a -> "面积: " + a;
BiFunction<Integer, Integer, String> areaDesc = area.andThen(describe);
assertEquals("面积: 15", areaDesc.apply(5, 3));
assertEquals("面积: 0", areaDesc.apply(0, 10));
System.out.println(areaDesc.apply(5, 3));
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### 与 Map 配合使用

- 描述: 使用 `Map.merge` 方法统计单词出现次数
- 断言: 重复 key 的值会累加

```java
// 方法体开始
System.out.println("=== Map.merge 统计 ===");
Map<String, Integer> wordCount = new HashMap<>();
BiFunction<Integer, Integer, Integer> sum = (oldVal, newVal) -> oldVal + newVal;
wordCount.merge("apple", 1, sum);
wordCount.merge("apple", 1, sum);
wordCount.merge("banana", 1, sum);
assertEquals(Integer.valueOf(2), wordCount.get("apple"));
assertEquals(Integer.valueOf(1), wordCount.get("banana"));
System.out.println("apple: " + wordCount.get("apple"));
System.out.println("banana: " + wordCount.get("banana"));
System.out.println("=== 测试通过 ===");
// 方法体结束
```
