---
name: IntToDoubleFunction
package: java.util.function
order: 92
---

## 介绍

`IntToDoubleFunction` 是 Java 8 引入的一个**函数式接口**，代表一个接受 `int` 参数并返回 `double` 值的函数。它用于 int 到 double 的转换，避免自动装箱开销。

对应的原始类型转换函数：
- `IntToLongFunction` — int → long
- `LongToIntFunction` — long → int
- `LongToDoubleFunction` — long → double
- `DoubleToIntFunction` — double → int
- `DoubleToLongFunction` — double → long

## 方法

### applyAsDouble

```java
double applyAsDouble(int value)
```

对给定 int 值执行转换并返回 double。

## 测试

### 基本转换

- 描述: 将 int 值除以 100 转换为 double 百分比
- 断言: 75 → 0.75

```java
// 方法体开始
System.out.println("=== 基本转换 ===");
IntToDoubleFunction toPercent = v -> v / 100.0;
assertEquals(0.75, toPercent.applyAsDouble(75), 0.0001);
assertEquals(1.0, toPercent.applyAsDouble(100), 0.0001);
assertEquals(0.0, toPercent.applyAsDouble(0), 0.0001);
System.out.println("75 → " + toPercent.applyAsDouble(75));
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### IntStream.mapToDouble

- 描述: 使用 `IntStream.mapToDouble` 转换
- 断言: 1-5 的平均值为 3.0

```java
// 方法体开始
System.out.println("=== IntStream.mapToDouble ===");
IntToDoubleFunction toDouble = v -> (double) v;
double avg = IntStream.rangeClosed(1, 5)
        .mapToDouble(toDouble)
        .average()
        .orElse(0.0);
assertEquals(3.0, avg, 0.0001);
System.out.println("平均值: " + avg);
System.out.println("=== 测试通过 ===");
// 方法体结束
```
