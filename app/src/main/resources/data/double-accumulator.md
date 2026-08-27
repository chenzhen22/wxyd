---
name: DoubleAccumulator
package: java.util.concurrent.atomic
order: 91
---

## 介绍

`DoubleAccumulator` 是 Java 8 引入的**高性能 double 累加器**，与 `DoubleAdder` 类似但更灵活。与 `LongAccumulator` 对应，它使用 `DoubleBinaryOperator` 自定义运算，而不是固定累加。

DoubleAccumulator 的核心特点：
- **自定义运算**：可指定求和、求最大值、求最小值等
- **高吞吐**：基于 Striped 64 算法分散竞争
- **非阻塞**：基于 CAS 操作

构造方法参数：
- `accumulatorFunction` — 二元运算
- `identity` — 恒等值（求和用 0、求最大值用 -Infinity、求最小值用 Infinity）

## 方法

### 构造方法

```java
public DoubleAccumulator(DoubleBinaryOperator accumulatorFunction, double identity)
```

### accumulate

```java
public void accumulate(double x)
```

累加给定值。

### get

```java
public double get()
```

返回当前结果。

### getThenReset / reset

快照并重置 / 直接重置。

## 测试

### 求和

- 描述: 累加 double 值
- 断言: 1.5+2.5+3.0 = 7.0

```java
// 方法体开始
System.out.println("=== 求和 ===");
DoubleAccumulator acc = new DoubleAccumulator((a, b) -> a + b, 0.0);
acc.accumulate(1.5);
acc.accumulate(2.5);
acc.accumulate(3.0);
assertEquals(7.0, acc.get(), 0.0001);
System.out.println("总和: " + acc.get());
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### 求最大值

- 描述: 使用 DoubleAccumulator 求最大值
- 断言: 最大值为 99.9

```java
// 方法体开始
System.out.println("=== 求最大值 ===");
DoubleAccumulator max = new DoubleAccumulator(Double::max, Double.NEGATIVE_INFINITY);
max.accumulate(10.5);
max.accumulate(99.9);
max.accumulate(50.0);
assertEquals(99.9, max.get(), 0.0001);
System.out.println("最大值: " + max.get());
System.out.println("=== 测试通过 ===");
// 方法体结束
```
