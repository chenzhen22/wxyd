---
name: LongSummaryStatistics
package: java.util
order: 71
---

## 介绍

`LongSummaryStatistics` 是 Java 8 引入的一个**状态对象**，用于收集 long 数据的统计信息，包括**计数、总和、最小值、平均值和最大值**。它是 `IntSummaryStatistics` 的 long 版本，与 `LongStream` 配合使用。

LongSummaryStatistics 的核心特点：
- **一站式统计**：一次遍历获取 5 个统计指标
- **与 Stream 集成**：`LongStream.summaryStatistics()` 直接返回
- **可变累加**：通过 `accept()` 或 `combine()` 动态添加数据
- **大数值支持**：`getSum()` 返回 `long`，适合大数累加

## 方法

### getCount

```java
public long getCount()
```

返回已记录的元素数量。

- **返回**: `long` — 元素个数

### getSum

```java
public long getSum()
```

返回已记录元素的总和。空集返回 0。

- **返回**: `long` — 总和

### getMin

```java
public long getMin()
```

返回已记录元素的最小值。空集返回 `Long.MAX_VALUE`。

- **返回**: `long` — 最小值

### getMax

```java
public long getMax()
```

返回已记录元素的最大值。空集返回 `Long.MIN_VALUE`。

- **返回**: `long` — 最大值

### getAverage

```java
public double getAverage()
```

返回已记录元素的算术平均值。空集返回 0.0。

- **返回**: `double` — 平均值

### accept

```java
public void accept(long value)
```

添加一个 long 值到统计中。

- **参数**: `value` — 要添加的值
- **返回**: 无

### combine

```java
public void combine(LongSummaryStatistics other)
```

将另一个 LongSummaryStatistics 的状态合并到当前对象中。

- **参数**: `other` — 另一个 LongSummaryStatistics
- **返回**: 无

## 测试

### accept 逐个添加

- 描述: 使用 `accept` 逐个添加数据并获取统计结果
- 断言: 100L, 200L, 300L 的统计值正确

```java
// 方法体开始
System.out.println("=== accept 逐个添加 ===");
LongSummaryStatistics stats = new LongSummaryStatistics();
stats.accept(100L);
stats.accept(200L);
stats.accept(300L);
assertEquals(3L, stats.getCount());
assertEquals(600L, stats.getSum());
assertEquals(100L, stats.getMin());
assertEquals(300L, stats.getMax());
assertEquals(200.0, stats.getAverage(), 0.0001);
System.out.println("count: " + stats.getCount());
System.out.println("sum: " + stats.getSum());
System.out.println("average: " + stats.getAverage());
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### LongStream.summaryStatistics

- 描述: 使用 `LongStream` 直接获取统计信息
- 断言: 1L 到 5L 的统计值正确

```java
// 方法体开始
System.out.println("=== LongStream.summaryStatistics ===");
LongSummaryStatistics stats = LongStream.of(1L, 2L, 3L, 4L, 5L).summaryStatistics();
assertEquals(5L, stats.getCount());
assertEquals(15L, stats.getSum());
assertEquals(1L, stats.getMin());
assertEquals(5L, stats.getMax());
assertEquals(3.0, stats.getAverage(), 0.0001);
System.out.println("count: " + stats.getCount() + ", sum: " + stats.getSum());
System.out.println("min: " + stats.getMin() + ", max: " + stats.getMax());
System.out.println("average: " + stats.getAverage());
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### combine

- 描述: 使用 `combine` 合并两个 LongSummaryStatistics
- 断言: 合并后统计值正确

```java
// 方法体开始
System.out.println("=== combine ===");
LongSummaryStatistics stats1 = new LongSummaryStatistics();
stats1.accept(10L);
stats1.accept(20L);
LongSummaryStatistics stats2 = new LongSummaryStatistics();
stats2.accept(30L);
stats2.accept(40L);
stats1.combine(stats2);
assertEquals(4L, stats1.getCount());
assertEquals(100L, stats1.getSum());
assertEquals(10L, stats1.getMin());
assertEquals(40L, stats1.getMax());
System.out.println("合并后 count: " + stats1.getCount());
System.out.println("合并后 sum: " + stats1.getSum());
System.out.println("=== 测试通过 ===");
// 方法体结束
```
