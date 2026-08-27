---
name: IntPredicate
package: java.util.function
order: 73
---

## 介绍

`IntPredicate` 是 Java 8 引入的一个**函数式接口**，代表一个接受 `int` 参数并返回 `boolean` 的条件判断。它是 `Predicate<Integer>` 的原始 int 特化版本，避免了自动装箱/拆箱的开销。

IntPredicate 的核心特点：
- **避免装箱**：直接操作原始 int 值
- **与 IntStream 集成**：`IntStream.filter()` 的常用参数
- **链式组合**：通过 `and`、`or`、`negate` 组合条件

IntPredicate 的四个方法：
- `test(int value)` — 核心判断方法
- `and(IntPredicate)` — 且条件组合
- `or(IntPredicate)` — 或条件组合
- `negate()` — 取反

对应的原始类型特化 Predicate：
- `LongPredicate` — 接受 long 参数
- `DoublePredicate` — 接受 double 参数

## 方法

### test

```java
boolean test(int value)
```

对给定 int 值执行条件判断。

- **参数**: `value` — 输入的 int 值
- **返回**: `boolean` — 判断结果

### and

```java
default IntPredicate and(IntPredicate other)
```

返回当前判断和 `other` 判断都为 true 才返回 true 的组合。

- **参数**: `other` — 另一个 IntPredicate
- **返回**: `IntPredicate` — 且条件组合

### or

```java
default IntPredicate or(IntPredicate other)
```

返回当前判断或 `other` 判断任一为 true 就返回 true 的组合。

- **参数**: `other` — 另一个 IntPredicate
- **返回**: `IntPredicate` — 或条件组合

### negate

```java
default IntPredicate negate()
```

返回当前 IntPredicate 的取反。

- **返回**: `IntPredicate` — 取反后的 IntPredicate

## 测试

### test 基本判断

- 描述: 使用 `test` 方法判断整数是否为正数
- 断言: 5 为正数，-3 不是正数

```java
// 方法体开始
System.out.println("=== test ===");
IntPredicate isPositive = v -> v > 0;
assertTrue(isPositive.test(5));
assertFalse(isPositive.test(-3));
assertFalse(isPositive.test(0));
System.out.println("5 是正数: " + isPositive.test(5));
System.out.println("-3 是正数: " + isPositive.test(-3));
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### and

- 描述: 使用 `and` 组合条件，判断正偶数
- 断言: 6 是正偶数，-2 不是，3 不是

```java
// 方法体开始
System.out.println("=== and ===");
IntPredicate isPositive = v -> v > 0;
IntPredicate isEven = v -> v % 2 == 0;
IntPredicate positiveEven = isPositive.and(isEven);
assertTrue(positiveEven.test(6));
assertFalse(positiveEven.test(-2));
assertFalse(positiveEven.test(3));
System.out.println("6 是正偶数: " + positiveEven.test(6));
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### or

- 描述: 使用 `or` 组合条件，判断负数或偶数
- 断言: -3、2 满足条件，5 不满足

```java
// 方法体开始
System.out.println("=== or ===");
IntPredicate isNegative = v -> v < 0;
IntPredicate isEven = v -> v % 2 == 0;
IntPredicate negativeOrEven = isNegative.or(isEven);
assertTrue(negativeOrEven.test(-3));
assertTrue(negativeOrEven.test(2));
assertFalse(negativeOrEven.test(5));
System.out.println("-3 是负数或偶数: " + negativeOrEven.test(-3));
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### IntStream.filter

- 描述: 使用 IntPredicate 配合 `IntStream.filter` 过滤流
- 断言: 提取 1-10 中所有偶数

```java
// 方法体开始
System.out.println("=== IntStream.filter ===");
IntPredicate isEven = v -> v % 2 == 0;
int[] evens = IntStream.rangeClosed(1, 10)
        .filter(isEven)
        .toArray();
assertArrayEquals(new int[]{2, 4, 6, 8, 10}, evens);
System.out.println("偶数: " + java.util.Arrays.toString(evens));
System.out.println("=== 测试通过 ===");
// 方法体结束
```
