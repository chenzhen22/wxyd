---
name: IntSupplier
package: java.util.function
order: 75
---

## 介绍

`IntSupplier` 是 Java 8 引入的一个**函数式接口**，代表一个不接受参数但返回 `int` 值的供应者。它是 `Supplier<Integer>` 的原始 int 特化版本，用于生成或提供 int 值。

IntSupplier 的核心特点：
- **无参有返回值**：不接受参数，返回一个 int 值
- **与 IntStream 集成**：`IntStream.generate(IntSupplier)` 的常用参数
- **惰性求值**：每次调用 `getAsInt()` 才实际计算

IntSupplier 的单个方法：
- `getAsInt()` — 核心方法，供应一个 int 值

对应的特化 Supplier：
- `LongSupplier` — 返回 long
- `DoubleSupplier` — 返回 double
- `BooleanSupplier` — 返回 boolean

## 方法

### getAsInt

```java
int getAsInt()
```

供应一个 int 值。

- **返回**: `int` — 供应的 int 值

## 测试

### getAsInt 常量供应

- 描述: 使用 `getAsInt` 返回常量值
- 断言: 始终返回 42

```java
// 方法体开始
System.out.println("=== 常量供应 ===");
IntSupplier answer = () -> 42;
assertEquals(42, answer.getAsInt());
assertEquals(42, answer.getAsInt());
System.out.println("答案: " + answer.getAsInt());
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### getAsInt 随机数

- 描述: 使用 IntSupplier 生成随机数
- 断言: 生成的随机数在范围内

```java
// 方法体开始
System.out.println("=== 随机数 ===");
IntSupplier dice = () -> ThreadLocalRandom.current().nextInt(1, 7);
int roll = dice.getAsInt();
assertTrue(roll >= 1 && roll <= 6);
System.out.println("掷骰子: " + roll);
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### IntStream.generate

- 描述: 使用 IntSupplier 配合 `IntStream.generate` 生成序列
- 断言: 生成前 5 个偶数

```java
// 方法体开始
System.out.println("=== IntStream.generate ===");
int[] start = {0};
IntSupplier evenGen = () -> {
    int val = start[0];
    start[0] += 2;
    return val;
};
int[] evens = IntStream.generate(evenGen)
        .limit(5)
        .toArray();
assertArrayEquals(new int[]{0, 2, 4, 6, 8}, evens);
System.out.println("前 5 个偶数: " + java.util.Arrays.toString(evens));
System.out.println("=== 测试通过 ===");
// 方法体结束
```
