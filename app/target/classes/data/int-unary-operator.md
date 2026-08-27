---
name: IntUnaryOperator
package: java.util.function
order: 83
---

## 介绍

`IntUnaryOperator` 是 Java 8 引入的一个**函数式接口**，代表一个对 `int` 值执行一元运算的操作。它是 `UnaryOperator<Integer>` 的原始 int 特化版本，输入和输出都是 int 类型。

IntUnaryOperator 的核心特点：
- **避免装箱**：直接操作原始 int 值
- **与 IntStream 集成**：`IntStream.map()` 的常用参数
- **链式组合**：通过 `andThen` 和 `compose` 组合运算

IntUnaryOperator 的四个方法：
- `applyAsInt(int operand)` — 核心方法，对 int 值执行运算
- `andThen(IntUnaryOperator)` — 先执行当前运算，再执行 after
- `compose(IntUnaryOperator)` — 先执行 before，再执行当前运算
- `identity()` — 返回输入本身的恒等运算

## 方法

### applyAsInt

```java
int applyAsInt(int operand)
```

对给定 int 值执行运算。

- **参数**: `operand` — 输入值
- **返回**: `int` — 运算结果

### andThen / compose

```java
default IntUnaryOperator andThen(IntUnaryOperator after)
default IntUnaryOperator compose(IntUnaryOperator before)
```

组合运算。`andThen` 先执行当前再执行 after；`compose` 先执行 before 再执行当前。

- **返回**: `IntUnaryOperator` — 组合后的运算符

### identity

```java
static IntUnaryOperator identity()
```

返回输入本身的恒等运算。

## 测试

### applyAsInt

- 描述: 使用 `applyAsInt` 计算绝对值
- 断言: |-5| = 5

```java
// 方法体开始
System.out.println("=== applyAsInt ===");
IntUnaryOperator abs = x -> x < 0 ? -x : x;
assertEquals(5, abs.applyAsInt(-5));
assertEquals(0, abs.applyAsInt(0));
assertEquals(3, abs.applyAsInt(3));
System.out.println("|-5| = " + abs.applyAsInt(-5));
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### andThen

- 描述: 使用 `andThen` 组合：先平方再取绝对值（虽然没必要，但演示组合）
- 断言: (-5)² = 25

```java
// 方法体开始
System.out.println("=== andThen ===");
IntUnaryOperator square = x -> x * x;
IntUnaryOperator negate = x -> -x;
IntUnaryOperator composed = square.andThen(negate);
assertEquals(-25, composed.applyAsInt(5));
assertEquals(-4, composed.applyAsInt(-2));
System.out.println("5 平方再取反: " + composed.applyAsInt(5));
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### identity

- 描述: 使用 `identity` 返回恒等运算
- 断言: 输入什么返回什么

```java
// 方法体开始
System.out.println("=== identity ===");
IntUnaryOperator id = IntUnaryOperator.identity();
assertEquals(42, id.applyAsInt(42));
assertEquals(-1, id.applyAsInt(-1));
System.out.println("identity(42) = " + id.applyAsInt(42));
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### IntStream.map

- 描述: 使用 IntUnaryOperator 配合 `IntStream.map`
- 断言: 将 1-5 每个数平方

```java
// 方法体开始
System.out.println("=== IntStream.map ===");
IntUnaryOperator square = x -> x * x;
int[] result = IntStream.rangeClosed(1, 5)
        .map(square)
        .toArray();
assertArrayEquals(new int[]{1, 4, 9, 16, 25}, result);
System.out.println("平方: " + java.util.Arrays.toString(result));
System.out.println("=== 测试通过 ===");
// 方法体结束
```
