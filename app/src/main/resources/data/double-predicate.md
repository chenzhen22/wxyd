---
name: DoublePredicate
package: java.util.function
order: 85
---

## 介绍

`DoublePredicate` 是 Java 8 引入的一个**函数式接口**，代表一个接受 `double` 参数并返回 `boolean` 的条件判断。它是 `Predicate<Double>` 的原始 double 特化版本。

DoublePredicate 的四个方法：
- `test(double value)` — 核心判断方法
- `and(DoublePredicate)` — 且条件组合
- `or(DoublePredicate)` — 或条件组合
- `negate()` — 取反

## 方法

### test

```java
boolean test(double value)
```

对给定 double 值执行条件判断。

- **返回**: `boolean` — 判断结果

### and / or / negate

与 IntPredicate 相同的链式组合方法。

## 测试

### test

- 描述: 判断 double 值是否为正数
- 断言: 3.14 为正数，-1.5 不是

```java
// 方法体开始
System.out.println("=== test ===");
DoublePredicate isPositive = v -> v > 0;
assertTrue(isPositive.test(3.14));
assertFalse(isPositive.test(-1.5));
assertFalse(isPositive.test(0.0));
System.out.println("3.14 是正数: " + isPositive.test(3.14));
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### DoubleStream.filter

- 描述: 使用 DoublePredicate 配合 DoubleStream.filter 过滤成绩
- 断言: 筛选出及格（>= 60）的成绩

```java
// 方法体开始
System.out.println("=== DoubleStream.filter ===");
DoublePredicate passed = v -> v >= 60.0;
double[] scores = {85.5, 45.0, 90.0, 59.5, 72.0};
double[] passedScores = DoubleStream.of(scores)
        .filter(passed)
        .toArray();
assertArrayEquals(new double[]{85.5, 90.0, 72.0}, passedScores, 0.0001);
System.out.println("及格成绩: " + java.util.Arrays.toString(passedScores));
System.out.println("=== 测试通过 ===");
// 方法体结束
```
