---
name: IntSummaryStatistics
package: java.util
order: 60
---

## 介绍

`IntSummaryStatistics` 是 Java 8 引入的一个**状态对象**，用于收集 int 数据的统计信息，包括**计数、总和、最小值、平均值和最大值**。它通常与 `IntStream` 配合使用，也可以作为 `Stream.collect()` 的可变累加器。

IntSummaryStatistics 的核心特点：
- **一站式统计**：一次遍历获取 5 个统计指标
- **与 Stream 集成**：`IntStream.summaryStatistics()` 直接返回
- **可变累加**：通过 `accept()` 或 `combine()` 动态添加数据
- **线程不安全**：应在单线程中使用，或使用外部同步

在实际开发中，`IntSummaryStatistics` 可以优雅地替代手动维护多个统计变量的繁琐代码，特别适合数据分析、报表生成和性能监控等场景。

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
public int getMin()
```

返回已记录元素的最小值。空集返回 `Integer.MAX_VALUE`。

- **返回**: `int` — 最小值

### getMax

```java
public int getMax()
```

返回已记录元素的最大值。空集返回 `Integer.MIN_VALUE`。

- **返回**: `int` — 最大值

### getAverage

```java
public double getAverage()
```

返回已记录元素的算术平均值。空集返回 0.0。

- **返回**: `double` — 平均值

### accept

```java
public void accept(int value)
```

添加一个 int 值到统计中。

- **参数**: `value` — 要添加的值
- **返回**: 无

### combine

```java
public void combine(IntSummaryStatistics other)
```

将另一个 IntSummaryStatistics 的状态合并到当前对象中。

- **参数**: `other` — 另一个 IntSummaryStatistics
- **返回**: 无

## 测试

### accept 逐个添加

- 描述: 使用 `accept` 逐个添加数据并获取统计结果
- 断言: 1, 2, 3, 4, 5 的统计值正确

```java
// 方法体开始
System.out.println("=== accept 逐个添加 ===");
IntSummaryStatistics stats = new IntSummaryStatistics();
stats.accept(1);
stats.accept(2);
stats.accept(3);
stats.accept(4);
stats.accept(5);
assertEquals(5L, stats.getCount());
assertEquals(15L, stats.getSum());
assertEquals(1, stats.getMin());
assertEquals(5, stats.getMax());
assertEquals(3.0, stats.getAverage(), 0.0001);
System.out.println("count: " + stats.getCount());
System.out.println("sum: " + stats.getSum());
System.out.println("min: " + stats.getMin());
System.out.println("max: " + stats.getMax());
System.out.println("average: " + stats.getAverage());
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### IntStream.summaryStatistics

- 描述: 使用 `IntStream` 直接获取统计信息
- 断言: 10 到 50 范围内（步长 10）的统计值正确

```java
// 方法体开始
System.out.println("=== IntStream.summaryStatistics ===");
IntSummaryStatistics stats = IntStream.of(10, 20, 30, 40, 50).summaryStatistics();
assertEquals(5L, stats.getCount());
assertEquals(150L, stats.getSum());
assertEquals(10, stats.getMin());
assertEquals(50, stats.getMax());
assertEquals(30.0, stats.getAverage(), 0.0001);
System.out.println("count: " + stats.getCount());
System.out.println("sum: " + stats.getSum());
System.out.println("average: " + stats.getAverage());
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### combine

- 描述: 使用 `combine` 合并两个 IntSummaryStatistics
- 断言: 合并后统计值等于最终结果

```java
// 方法体开始
System.out.println("=== combine ===");
IntSummaryStatistics stats1 = new IntSummaryStatistics();
stats1.accept(1);
stats1.accept(2);
stats1.accept(3);
IntSummaryStatistics stats2 = new IntSummaryStatistics();
stats2.accept(4);
stats2.accept(5);
stats1.combine(stats2);
assertEquals(5L, stats1.getCount());
assertEquals(15L, stats1.getSum());
assertEquals(1, stats1.getMin());
assertEquals(5, stats1.getMax());
assertEquals(3.0, stats1.getAverage(), 0.0001);
System.out.println("合并后 count: " + stats1.getCount());
System.out.println("合并后 sum: " + stats1.getSum());
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### 空统计

- 描述: 空 IntSummaryStatistics 的默认值
- 断言: count=0, sum=0, average=0.0, min=MAX_VALUE, max=MIN_VALUE

```java
// 方法体开始
System.out.println("=== 空统计 ===");
IntSummaryStatistics stats = new IntSummaryStatistics();
assertEquals(0L, stats.getCount());
assertEquals(0L, stats.getSum());
assertEquals(Integer.MAX_VALUE, stats.getMin());
assertEquals(Integer.MIN_VALUE, stats.getMax());
assertEquals(0.0, stats.getAverage(), 0.0001);
System.out.println("空统计默认值验证通过");
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### toString 输出

- 描述: `toString` 方法直接格式化的统计信息
- 断言: 输出包含所有统计字段

```java
// 方法体开始
System.out.println("=== toString ===");
IntSummaryStatistics stats = IntStream.rangeClosed(1, 5).summaryStatistics();
String output = stats.toString();
System.out.println(output);
assertTrue(output.contains("count=5"));
assertTrue(output.contains("sum=15"));
assertTrue(output.contains("average=3.0"));
System.out.println("=== 测试通过 ===");
// 方法体结束
```
