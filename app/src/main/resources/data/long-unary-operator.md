---
name: LongUnaryOperator
package: java.util.function
order: 97
---

## 介绍

`LongUnaryOperator` 是 Java 8 引入的一个**函数式接口**，代表一个对 `long` 值执行一元运算的操作。它是 `UnaryOperator<Long>` 的原始 long 特化版本。

与 `IntUnaryOperator`、`DoubleUnaryOperator` 对应。

## 方法

### applyAsLong

```java
long applyAsLong(long operand)
```

对给定 long 值执行运算。

### andThen / compose / identity

与 `IntUnaryOperator` 相同的链式组合方法。

## 测试

### applyAsLong

- 描述: 计算 long 值的绝对值
- 断言: |-100L| = 100L

```java
// 方法体开始
System.out.println("=== applyAsLong ===");
LongUnaryOperator abs = x -> x < 0 ? -x : x;
assertEquals(100L, abs.applyAsLong(-100L));
assertEquals(0L, abs.applyAsLong(0L));
System.out.println("|-100| = " + abs.applyAsLong(-100L));
System.out.println("=== 测试通过 ===");
// 方法体结束
```
