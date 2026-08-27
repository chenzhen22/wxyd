---
name: Period
package: java.time
order: 11
---

## 介绍

`java.time.Period` 是 Java 8 日期时间 API 中表示**日期量**的不可变类。它基于年、月、日，用于表示日期之间的时间长度，如 "2 年 6 个月 3 天"。

`Period` 是不可变且线程安全的，所有修改操作都会返回新实例。

常见用途：
- **日期差计算**：计算两个日期之间的年、月、日差值
- **日期量创建**：通过年、月、日、周创建日期量
- **日期量运算**：加减日期量、乘以系数
- **日期量转换**：将日期量转换为总月数
- **日期量比较**：判断正负、是否为零
- **字符串解析**：解析和生成 ISO-8601 格式的日期量字符串（如 "P2Y6M3D"）

## 方法

### ofDays

```java
public static Period ofDays(int days)
```

创建指定天数的 Period。

- **参数**: `days` — 天数
- **返回**: `Period`

### ofMonths

```java
public static Period ofMonths(int months)
```

创建指定月数的 Period。

- **参数**: `months` — 月数
- **返回**: `Period`

### ofYears

```java
public static Period ofYears(int years)
```

创建指定年数的 Period。

- **参数**: `years` — 年数
- **返回**: `Period`

### ofWeeks

```java
public static Period ofWeeks(int weeks)
```

创建指定周数的 Period（1 周 = 7 天）。

- **参数**: `weeks` — 周数
- **返回**: `Period`

### between

```java
public static Period between(LocalDate startDateInclusive, LocalDate endDateExclusive)
```

计算两个日期之间的 Period。

- **参数**: `startDateInclusive` — 起始日期（包含）；`endDateExclusive` — 结束日期（不包含）
- **返回**: `Period`

### getDays

```java
public int getDays()
```

获取 Period 中的天数部分（可能为负）。

- **返回**: `int` — 天数

### getMonths

```java
public int getMonths()
```

获取 Period 中的月数部分（可能为负）。

- **返回**: `int` — 月数

### getYears

```java
public int getYears()
```

获取 Period 中的年数部分（可能为负）。

- **返回**: `int` — 年数

### plus

```java
public Period plus(Period amountToAdd)
```

加上另一个 Period。

- **参数**: `amountToAdd` — 要加的 Period
- **返回**: `Period`

### minus

```java
public Period minus(Period amountToSubtract)
```

减去另一个 Period。

- **参数**: `amountToSubtract` — 要减的 Period
- **返回**: `Period`

### multipliedBy

```java
public Period multipliedBy(int scalar)
```

乘以一个标量值。

- **参数**: `scalar` — 乘数
- **返回**: `Period`

### toTotalMonths

```java
public long toTotalMonths()
```

将 Period 转换为总月数（年数 * 12 + 月数）。

- **返回**: `long` — 总月数

### isNegative

```java
public boolean isNegative()
```

判断 Period 是否为负（年、月、日中任一为负）。

- **返回**: `boolean`

### isZero

```java
public boolean isZero()
```

判断 Period 是否为零（年、月、日都为 0）。

- **返回**: `boolean`

### parse

```java
public static Period parse(CharSequence text)
```

解析 ISO-8601 格式的 Period 字符串，如 "P2Y6M3D"。

- **参数**: `text` — ISO-8601 格式字符串，如 "P1Y"、"P2M"、"P1Y2M3D"
- **返回**: `Period`
- **抛出**: `DateTimeParseException` — 如果文本格式错误

### toString

```java
public String toString()
```

返回 ISO-8601 格式的字符串表示，如 "P2Y6M3D"。

- **返回**: `String` — ISO-8601 格式字符串

## 测试

### ofDays

- 描述: 使用 ofDays 创建天数的 Period
- 断言: 5 天的天数字段为 5

```java
// 方法体开始
System.out.println("=== ofDays ===");
Period period = Period.ofDays(5);
System.out.println("5天: " + period);
assertEquals(5, period.getDays());
assertEquals(0, period.getMonths());
assertEquals(0, period.getYears());
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### ofMonths

- 描述: 使用 ofMonths 创建月数的 Period
- 断言: 3 月的月数字段为 3

```java
// 方法体开始
System.out.println("=== ofMonths ===");
Period period = Period.ofMonths(3);
System.out.println("3个月: " + period);
assertEquals(3, period.getMonths());
assertEquals(0, period.getYears());
assertEquals(0, period.getDays());
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### ofYears

- 描述: 使用 ofYears 创建年数的 Period
- 断言: 2 年的年数字段为 2

```java
// 方法体开始
System.out.println("=== ofYears ===");
Period period = Period.ofYears(2);
System.out.println("2年: " + period);
assertEquals(2, period.getYears());
assertEquals(0, period.getMonths());
assertEquals(0, period.getDays());
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### ofWeeks

- 描述: 使用 ofWeeks 创建周数的 Period
- 断言: 2 周 = 14 天

```java
// 方法体开始
System.out.println("=== ofWeeks ===");
Period period = Period.ofWeeks(2);
System.out.println("2周: " + period);
assertEquals(14, period.getDays());
assertEquals(0, period.getMonths());
assertEquals(0, period.getYears());
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### between

- 描述: 计算两个 LocalDate 之间的 Period
- 断言: 2024-03-01 到 2026-07-15 = P2Y4M14D

```java
// 方法体开始
System.out.println("=== between ===");
LocalDate start = LocalDate.of(2024, 3, 1);
LocalDate end = LocalDate.of(2026, 7, 15);
Period period = Period.between(start, end);
System.out.println(start + " 到 " + end + " = " + period);
assertEquals(2, period.getYears());
assertEquals(4, period.getMonths());
assertEquals(14, period.getDays());
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### getDays

- 描述: 获取 Period 的天数部分
- 断言: P1Y2M3D 的天数为 3

```java
// 方法体开始
System.out.println("=== getDays ===");
Period period = Period.of(1, 2, 3);
System.out.println(period + " 天数: " + period.getDays());
assertEquals(3, period.getDays());
assertEquals(2, period.getMonths());
assertEquals(1, period.getYears());
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### getMonths

- 描述: 获取 Period 的月数部分
- 断言: P1Y2M3D 的月数为 2

```java
// 方法体开始
System.out.println("=== getMonths ===");
Period period = Period.of(1, 2, 3);
System.out.println(period + " 月数: " + period.getMonths());
assertEquals(2, period.getMonths());
assertEquals(1, period.getYears());
assertEquals(3, period.getDays());
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### getYears

- 描述: 获取 Period 的年数部分
- 断言: P1Y2M3D 的年数为 1

```java
// 方法体开始
System.out.println("=== getYears ===");
Period period = Period.of(1, 2, 3);
System.out.println(period + " 年数: " + period.getYears());
assertEquals(1, period.getYears());
assertEquals(2, period.getMonths());
assertEquals(3, period.getDays());
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### plus

- 描述: Period 相加
- 断言: P1Y2M + P3M = P1Y5M

```java
// 方法体开始
System.out.println("=== plus ===");
Period p1 = Period.of(1, 2, 0);
Period p2 = Period.ofMonths(3);
Period result = p1.plus(p2);
System.out.println(p1 + " + " + p2 + " = " + result);
assertEquals(1, result.getYears());
assertEquals(5, result.getMonths());
assertEquals(0, result.getDays());
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### minus

- 描述: Period 相减
- 断言: P1Y6M - P3M = P1Y3M

```java
// 方法体开始
System.out.println("=== minus ===");
Period p1 = Period.of(1, 6, 0);
Period p2 = Period.ofMonths(3);
Period result = p1.minus(p2);
System.out.println(p1 + " - " + p2 + " = " + result);
assertEquals(1, result.getYears());
assertEquals(3, result.getMonths());
assertEquals(0, result.getDays());
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### multipliedBy

- 描述: Period 乘以标量
- 断言: P1Y2M * 3 = P3Y6M

```java
// 方法体开始
System.out.println("=== multipliedBy ===");
Period period = Period.of(1, 2, 0);
Period result = period.multipliedBy(3);
System.out.println(period + " * 3 = " + result);
assertEquals(3, result.getYears());
assertEquals(6, result.getMonths());
assertEquals(0, result.getDays());
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### toTotalMonths

- 描述: 将 Period 转换为总月数
- 断言: P1Y2M = 14 个月

```java
// 方法体开始
System.out.println("=== toTotalMonths ===");
Period period = Period.of(1, 2, 0);
long totalMonths = period.toTotalMonths();
System.out.println(period + " = " + totalMonths + " 个月");
assertEquals(14, totalMonths);
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### isNegative

- 描述: 判断 Period 是否为负
- 断言: 正 Period 不是负的，负 Period 是负的

```java
// 方法体开始
System.out.println("=== isNegative ===");
Period positive = Period.ofDays(1);
Period negative = Period.ofDays(-1);
System.out.println(positive + " isNegative: " + positive.isNegative());
System.out.println(negative + " isNegative: " + negative.isNegative());
assertFalse(positive.isNegative());
assertTrue(negative.isNegative());
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### isZero

- 描述: 判断 Period 是否为零
- 断言: Period.ZERO 是零，P1D 不是零

```java
// 方法体开始
System.out.println("=== isZero ===");
Period zero = Period.ZERO;
Period nonZero = Period.ofDays(1);
System.out.println(zero + " isZero: " + zero.isZero());
System.out.println(nonZero + " isZero: " + nonZero.isZero());
assertTrue(zero.isZero());
assertFalse(nonZero.isZero());
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### parse

- 描述: 解析 ISO-8601 Period 字符串
- 断言: "P1Y2M3D" 解析后年、月、日正确

```java
// 方法体开始
System.out.println("=== parse ===");
Period period = Period.parse("P1Y2M3D");
System.out.println("解析 P1Y2M3D: " + period);
assertEquals(1, period.getYears());
assertEquals(2, period.getMonths());
assertEquals(3, period.getDays());
assertEquals("P1Y2M3D", period.toString());
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### toString

- 描述: Period 的字符串表示
- 断言: Period.of(2, 6, 3) 的 toString 为 "P2Y6M3D"

```java
// 方法体开始
System.out.println("=== toString ===");
Period p1 = Period.of(2, 6, 3);
Period p2 = Period.ofMonths(5);
Period p3 = Period.ofWeeks(1);
System.out.println("toString: " + p1 + ", " + p2 + ", " + p3);
assertEquals("P2Y6M3D", p1.toString());
assertEquals("P5M", p2.toString());
assertEquals("P7D", p3.toString());
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### immutable

- 描述: Period 的不可变性
- 断言: plus/minus/multipliedBy 返回新实例，原对象不变

```java
// 方法体开始
System.out.println("=== immutable ===");
Period original = Period.of(1, 0, 0);
Period result = original.plus(Period.ofMonths(3));
System.out.println("原对象: " + original + ", 新对象: " + result);
assertNotSame(original, result);
assertEquals(1, original.getYears());
assertEquals(0, original.getMonths());
assertEquals(1, result.getYears());
assertEquals(3, result.getMonths());
System.out.println("=== 测试通过 ===");
// 方法体结束
```
