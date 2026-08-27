---
name: DoubleSummaryStatistics
package: java.util
order: 72
---

## 介绍

`DoubleSummaryStatistics` 是 Java 8 引入的一个**状态对象**，用于收集 double 数据的统计信息，包括**计数、总和、最小值、平均值和最大值**。它是 `LongSummaryStatistics` 的 double 版本，与 `DoubleStream` 配合使用。

DoubleSummaryStatistics 的核心特点：
- **一站式统计**：一次遍历获取 5 个统计指标
- **与 Stream 集成**：`DoubleStream.summaryStatistics()` 直接返回
- **可变累加**：通过 `accept()` 或 `combine()` 动态添加数据
- **高精度累加**：内部使用 Kahan 补偿求和算法减少浮点误差

## 方法

### getCount

```java
public long getCount()
```

返回已记录的元素数量。

- **返回**: `long` — 元素个数

### getSum

```java
public double getSum()
```

返回已记录元素的总和。空集返回 0。

- **返回**: `double` — 总和

### getMin

```java
public double getMin()
```

返回已记录元素的最小值。空集返回 `Double.POSITIVE_INFINITY`。

- **返回**: `double` — 最小值

### getMax

```java
public double getMax()
```

返回已记录元素的最大值。空集返回 `Double.NEGATIVE_INFINITY`。

- **返回**: `double` — 最大值

### getAverage

```java
public double getAverage()
```

返回已记录元素的算术平均值。空集返回 0.0。

- **返回**: `double` — 平均值

### accept

```java
public void accept(double value)
```

添加一个 double 值到统计中。

- **参数**: `value` — 要添加的值
- **返回**: 无

### combine

```java
public void combine(DoubleSummaryStatistics other)
```

将另一个 DoubleSummaryStatistics 的状态合并到当前对象中。

- **参数**: `other` — 另一个 DoubleSummaryStatistics
- **返回**: 无

## 测试

### accept 逐个添加

- 描述: 使用 `accept` 逐个添加数据并获取统计结果
- 断言: 1.5, 2.5, 3.5 的统计值正确

```java
// 方法体开始
System.out.println("=== accept 逐个添加 ===");
DoubleSummaryStatistics stats = new DoubleSummaryStatistics();
stats.accept(1.5);
stats.accept(2.5);
stats.accept(3.5);
assertEquals(3L, stats.getCount());
assertEquals(7.5, stats.getSum(), 0.0001);
assertEquals(1.5, stats.getMin(), 0.0001);
assertEquals(3.5, stats.getMax(), 0.0001);
assertEquals(2.5, stats.getAverage(), 0.0001);
System.out.println("count: " + stats.getCount() + ", sum: " + stats.getSum());
System.out.println("average: " + stats.getAverage());
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### DoubleStream.summaryStatistics

- 描述: 使用 `DoubleStream` 直接获取统计信息
- 断言: 百分制分数统计

```java
// 方法体开始
System.out.println("=== DoubleStream.summaryStatistics ===");
DoubleSummaryStatistics stats = DoubleStream.of(85.5, 90.0, 76.5, 95.5, 88.0).summaryStatistics();
assertEquals(5L, stats.getCount());
assertEquals(435.5, stats.getSum(), 0.0001);
assertEquals(76.5, stats.getMin(), 0.0001);
assertEquals(95.5, stats.getMax(), 0.0001);
System.out.println("成绩统计 - 人数: " + stats.getCount());
System.out.println("平均分: " + stats.getAverage());
System.out.println("最高分: " + stats.getMax());
System.out.println("最低分: " + stats.getMin());
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### combine

- 描述: 使用 `combine` 合并两组统计数据
- 断言: 合并后统计值正确

```java
// 方法体开始
System.out.println("=== combine ===");
DoubleSummaryStatistics group1 = new DoubleSummaryStatistics();
group1.accept(10.0);
group1.accept(20.0);
DoubleSummaryStatistics group2 = new DoubleSummaryStatistics();
group2.accept(30.0);
group2.accept(40.0);
group1.combine(group2);
assertEquals(4L, group1.getCount());
assertEquals(100.0, group1.getSum(), 0.0001);
assertEquals(10.0, group1.getMin(), 0.0001);
assertEquals(40.0, group1.getMax(), 0.0001);
assertEquals(25.0, group1.getAverage(), 0.0001);
System.out.println("合并后 sum: " + group1.getSum());
System.out.println("合并后 average: " + group1.getAverage());
System.out.println("=== 测试通过 ===");
// 方法体结束
```
