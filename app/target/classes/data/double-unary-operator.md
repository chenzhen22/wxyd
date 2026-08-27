---
name: DoubleUnaryOperator
package: java.util.function
order: 98
---

## 介绍

`DoubleUnaryOperator` 是 Java 8 引入的一个**函数式接口**，代表一个对 `double` 值执行一元运算的操作。它是 `UnaryOperator<Double>` 的原始 double 特化版本。

## 方法

### applyAsDouble

```java
double applyAsDouble(double operand)
```

对给定 double 值执行运算。

### andThen / compose / identity

与 `IntUnaryOperator` 相同的链式组合方法。

## 测试

### 摄氏转华氏

- 描述: 将摄氏度转换为华氏度
- 断言: 0°C = 32°F, 100°C = 212°F

```java
// 方法体开始
System.out.println("=== 摄氏转华氏 ===");
DoubleUnaryOperator cToF = c -> c * 9.0 / 5.0 + 32;
assertEquals(32.0, cToF.applyAsDouble(0), 0.0001);
assertEquals(212.0, cToF.applyAsDouble(100), 0.0001);
assertEquals(98.6, cToF.applyAsDouble(37), 0.0001);
System.out.println("0°C = " + cToF.applyAsDouble(0) + "°F");
System.out.println("100°C = " + cToF.applyAsDouble(100) + "°F");
System.out.println("=== 测试通过 ===");
// 方法体结束
```
