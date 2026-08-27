---
name: BinaryOperator
package: java.util.function
order: 59
---

## 介绍

`BinaryOperator<T>` 是 Java 8 引入的一个**函数式接口**，是 `BiFunction<T, T, T>` 的特化版本。它代表一个接受**两个同类型参数**并返回**相同类型结果**的函数，即对两个值执行"二元运算"。

BinaryOperator 的核心特点：
- **双参同类型**：两个参数和返回值的类型完全相同
- **minBy/maxBy**：提供便捷的静态方法，基于 Comparator 获取最小/最大值
- **与 Stream 紧密配合**：`Stream.reduce()` 的常用参数类型
- **继承自 BiFunction**：拥有 `andThen` 组合能力

BinaryOperator 除了继承 BiFunction 的 `apply` 和 `andThen` 外，还提供了两个实用静态方法：
- `minBy(Comparator)` — 返回一个 BinaryOperator，返回两个元素中较小的那个
- `maxBy(Comparator)` — 返回一个 BinaryOperator，返回两个元素中较大的那个

## 方法

### apply

```java
T apply(T t, T u)
```

对给定两个参数执行运算，返回同类型结果。

- **参数**: `t` — 第一个输入参数；`u` — 第二个输入参数
- **返回**: `T` — 运算结果

### minBy

```java
static <T> BinaryOperator<T> minBy(Comparator<? super T> comparator)
```

返回一个 BinaryOperator，使用给定的 Comparator 取两个元素中的**较小值**。

- **参数**: `comparator` — 比较器
- **返回**: `BinaryOperator<T>` — 取较小值的二元运算符

### maxBy

```java
static <T> BinaryOperator<T> maxBy(Comparator<? super T> comparator)
```

返回一个 BinaryOperator，使用给定的 Comparator 取两个元素中的**较大值**。

- **参数**: `comparator` — 比较器
- **返回**: `BinaryOperator<T>` — 取较大值的二元运算符

## 测试

### apply 求和

- 描述: 使用 `apply` 方法对两个整数求和
- 断言: 5 + 3 = 8

```java
// 方法体开始
System.out.println("=== apply 求和 ===");
BinaryOperator<Integer> sum = (a, b) -> a + b;
assertEquals(Integer.valueOf(8), sum.apply(5, 3));
assertEquals(Integer.valueOf(0), sum.apply(-2, 2));
System.out.println("5 + 3 = " + sum.apply(5, 3));
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### apply 字符串拼接

- 描述: 使用 BinaryOperator 拼接两个字符串，中间加空格
- 断言: `"Hello"` + `"World"` = `"Hello World"`

```java
// 方法体开始
System.out.println("=== apply 字符串拼接 ===");
BinaryOperator<String> joinWithSpace = (a, b) -> a + " " + b;
assertEquals("Hello World", joinWithSpace.apply("Hello", "World"));
assertEquals("A B", joinWithSpace.apply("A", "B"));
System.out.println(joinWithSpace.apply("Hello", "World"));
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### Stream.reduce

- 描述: 使用 `BinaryOperator` 配合 `Stream.reduce` 求最大值
- 断言: 数组中最大值为 9

```java
// 方法体开始
System.out.println("=== Stream.reduce ===");
BinaryOperator<Integer> maxOp = (a, b) -> a > b ? a : b;
Optional<Integer> max = Stream.of(3, 7, 2, 9, 5).reduce(maxOp);
assertEquals(Integer.valueOf(9), max.get());
System.out.println("最大值: " + max.get());
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### minBy

- 描述: 使用 `minBy` 根据字符串长度取较短者
- 断言: `"hi"` 比 `"hello"` 短

```java
// 方法体开始
System.out.println("=== minBy ===");
BinaryOperator<String> shorter = BinaryOperator.minBy(Comparator.comparing(String::length));
assertEquals("hi", shorter.apply("hello", "hi"));
assertEquals("a", shorter.apply("a", "bb"));
System.out.println("较短者: " + shorter.apply("hello", "hi"));
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### maxBy

- 描述: 使用 `maxBy` 根据字符串长度取较长者
- 断言: `"hello"` 比 `"hi"` 长

```java
// 方法体开始
System.out.println("=== maxBy ===");
BinaryOperator<String> longer = BinaryOperator.maxBy(Comparator.comparing(String::length));
assertEquals("hello", longer.apply("hello", "hi"));
assertEquals("bb", longer.apply("a", "bb"));
System.out.println("较长者: " + longer.apply("hello", "hi"));
System.out.println("=== 测试通过 ===");
// 方法体结束
```
