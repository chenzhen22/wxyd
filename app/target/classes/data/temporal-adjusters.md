---
name: TemporalAdjusters
package: java.time.temporal
order: 67
---

## 介绍

`java.time.temporal.TemporalAdjusters` 是 Java 8 提供的一个**工具类**，包含大量预定义的 `TemporalAdjuster` 实现。`TemporalAdjuster` 是一个函数式接口，用于修改时间对象，而 `TemporalAdjusters` 提供了常见的日期调整策略。

TemporalAdjusters 的核心特点：
- **便捷的日期调整**：一行代码完成复杂的日期计算
- **与 Temporal 集成**：配合 `LocalDate.with()`、`LocalDateTime.with()` 使用
- **丰富的预定义策略**：下周一、当月最后一天、下个月第一天等

常用调整器方法：
- `next(DayOfWeek)` / `previous(DayOfWeek)` — 下一个/上一个星期几
- `firstDayOfMonth()` / `lastDayOfMonth()` — 当月第一天/最后一天
- `firstDayOfYear()` / `lastDayOfYear()` — 当年第一天/最后一天
- `firstInMonth(DayOfWeek)` / `lastInMonth(DayOfWeek)` — 当月第一个/最后一个星期几
- `dayOfWeekInMonth(int, DayOfWeek)` — 当月第 N 个星期几

## 方法

### next

```java
public static TemporalAdjuster next(DayOfWeek dayOfWeek)
```

返回调整到**下一个指定星期几**的调整器（不包含当天）。

- **参数**: `dayOfWeek` — 目标星期几
- **返回**: `TemporalAdjuster` — 调整器

### previous

```java
public static TemporalAdjuster previous(DayOfWeek dayOfWeek)
```

返回调整到**上一个指定星期几**的调整器（不包含当天）。

- **参数**: `dayOfWeek` — 目标星期几
- **返回**: `TemporalAdjuster` — 调整器

### firstDayOfMonth

```java
public static TemporalAdjuster firstDayOfMonth()
```

返回调整为**当月第一天**的调整器。

- **返回**: `TemporalAdjuster` — 调整器

### lastDayOfMonth

```java
public static TemporalAdjuster lastDayOfMonth()
```

返回调整为**当月最后一天**的调整器。

- **返回**: `TemporalAdjuster` — 调整器

### firstDayOfNextMonth

```java
public static TemporalAdjuster firstDayOfNextMonth()
```

返回调整为**下个月第一天**的调整器。

- **返回**: `TemporalAdjuster` — 调整器

### firstDayOfYear

```java
public static TemporalAdjuster firstDayOfYear()
```

返回调整为**当年第一天**的调整器。

- **返回**: `TemporalAdjuster` — 调整器

### lastDayOfYear

```java
public static TemporalAdjuster lastDayOfYear()
```

返回调整为**当年最后一天**的调整器。

- **返回**: `TemporalAdjuster` — 调整器

### firstInMonth

```java
public static TemporalAdjuster firstInMonth(DayOfWeek dayOfWeek)
```

返回调整为**当月第一个指定星期几**的调整器。

- **参数**: `dayOfWeek` — 目标星期几
- **返回**: `TemporalAdjuster` — 调整器

### lastInMonth

```java
public static TemporalAdjuster lastInMonth(DayOfWeek dayOfWeek)
```

返回调整为**当月最后一个指定星期几**的调整器。

- **参数**: `dayOfWeek` — 目标星期几
- **返回**: `TemporalAdjuster` — 调整器

## 测试

### next 和 previous

- 描述: 使用 `next` 和 `previous` 找到下一个/上一个周一
- 断言: 从周三到下一个周一和上一个周一

```java
// 方法体开始
System.out.println("=== next / previous ===");
LocalDate wednesday = LocalDate.of(2024, 1, 17); // 周三
LocalDate nextMonday = wednesday.with(TemporalAdjusters.next(DayOfWeek.MONDAY));
LocalDate prevMonday = wednesday.with(TemporalAdjusters.previous(DayOfWeek.MONDAY));
assertEquals(DayOfWeek.MONDAY, nextMonday.getDayOfWeek());
assertEquals(DayOfWeek.MONDAY, prevMonday.getDayOfWeek());
assertTrue(nextMonday.isAfter(wednesday));
assertTrue(prevMonday.isBefore(wednesday));
System.out.println("周三: " + wednesday);
System.out.println("下周一: " + nextMonday);
System.out.println("上周一: " + prevMonday);
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### firstDayOfMonth / lastDayOfMonth

- 描述: 获取当月的第一天和最后一天
- 断言: 2024 年 2 月（闰年）第一天为 1 日，最后一天为 29 日

```java
// 方法体开始
System.out.println("=== 月首月末 ===");
LocalDate febDate = LocalDate.of(2024, 2, 15);
LocalDate firstDay = febDate.with(TemporalAdjusters.firstDayOfMonth());
LocalDate lastDay = febDate.with(TemporalAdjusters.lastDayOfMonth());
assertEquals(1, firstDay.getDayOfMonth());
assertEquals(LocalDate.of(2024, 2, 29), lastDay);
System.out.println("2024-02 第一天: " + firstDay);
System.out.println("2024-02 最后一天: " + lastDay);
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### firstDayOfYear / lastDayOfYear

- 描述: 获取当年的第一天和最后一天
- 断言: 2024 年（闰年）最后一天为 12 月 31 日

```java
// 方法体开始
System.out.println("=== 年首年末 ===");
LocalDate midYear = LocalDate.of(2024, 6, 15);
LocalDate firstDay = midYear.with(TemporalAdjusters.firstDayOfYear());
LocalDate lastDay = midYear.with(TemporalAdjusters.lastDayOfYear());
assertEquals(LocalDate.of(2024, 1, 1), firstDay);
assertEquals(LocalDate.of(2024, 12, 31), lastDay);
System.out.println("2024 第一天: " + firstDay);
System.out.println("2024 最后一天: " + lastDay);
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### firstInMonth / lastInMonth

- 描述: 获取当月第一个和最后一个周五
- 断言: 2024 年 1 月第一个周五为 5 日，最后一个周五为 26 日

```java
// 方法体开始
System.out.println("=== 月内星期几 ===");
LocalDate jan2024 = LocalDate.of(2024, 1, 15);
LocalDate firstFri = jan2024.with(TemporalAdjusters.firstInMonth(DayOfWeek.FRIDAY));
LocalDate lastFri = jan2024.with(TemporalAdjusters.lastInMonth(DayOfWeek.FRIDAY));
assertEquals(LocalDate.of(2024, 1, 5), firstFri);
assertEquals(LocalDate.of(2024, 1, 26), lastFri);
System.out.println("2024-01 第一个周五: " + firstFri);
System.out.println("2024-01 最后一个周五: " + lastFri);
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### firstDayOfNextMonth

- 描述: 获取下个月第一天
- 断言: 1 月 15 日的下个月第一天是 2 月 1 日

```java
// 方法体开始
System.out.println("=== 下个月第一天 ===");
LocalDate janMid = LocalDate.of(2024, 1, 15);
LocalDate nextMonth = janMid.with(TemporalAdjusters.firstDayOfNextMonth());
assertEquals(LocalDate.of(2024, 2, 1), nextMonth);
System.out.println(janMid + " 的下个月第一天: " + nextMonth);
System.out.println("=== 测试通过 ===");
// 方法体结束
```
