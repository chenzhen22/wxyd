---
name: LocalDate
package: java.time
order: 5
---

## 介绍

`java.time.LocalDate` 是 Java 8 日期时间 API 中表示**不含时间和时区的日期**的不可变类。它精确到天，用于表示如 "2026-07-23" 这样的日期。

`LocalDate` 是不可变且线程安全的，所有修改操作都会返回新实例。

常见用途：
- **日期处理**：获取/设置/加减年、月、日
- **日期比较**：判断先后、比较大小
- **格式解析**：解析和格式化日期字符串
- **日期计算**：计算两个日期之间的天数、月数

## 方法

### now

```java
public static LocalDate now()
```

从系统默认时区获取当前日期。

- **返回**: `LocalDate` — 当前日期

### now(Clock)

```java
public static LocalDate now(Clock clock)
```

从指定时钟获取当前日期。

- **参数**: `clock` — 时钟对象
- **返回**: `LocalDate`

### now(ZoneId)

```java
public static LocalDate now(ZoneId zone)
```

从指定时区获取当前日期。

- **参数**: `zone` — 时区 ID
- **返回**: `LocalDate`

### of

```java
public static LocalDate of(int year, int month, int dayOfMonth)
```

根据年、月、日创建日期。

- **参数**: `year` — 年；`month` — 月；`dayOfMonth` — 日
- **返回**: `LocalDate`

### of(Month)

```java
public static LocalDate of(int year, Month month, int dayOfMonth)
```

根据年、月（Month 枚举）、日创建日期。

- **参数**: `year` — 年；`month` — 月枚举；`dayOfMonth` — 日
- **返回**: `LocalDate`

### ofYearDay

```java
public static LocalDate ofYearDay(int year, int dayOfYear)
```

根据年和当年天数创建日期。

- **参数**: `year` — 年；`dayOfYear` — 当年第几天（1-366）
- **返回**: `LocalDate`

### ofEpochDay

```java
public static LocalDate ofEpochDay(long epochDay)
```

从纪元（1970-01-01）天数创建日期。

- **参数**: `epochDay` — 纪元天数
- **返回**: `LocalDate`

### parse

```java
public static LocalDate parse(CharSequence text)
```

解析形如 "2026-07-23" 的日期字符串。

- **参数**: `text` — 日期字符串，如 "2026-07-23"
- **返回**: `LocalDate`

### parse(DateTimeFormatter)

```java
public static LocalDate parse(CharSequence text, DateTimeFormatter formatter)
```

使用指定格式化器解析日期字符串。

- **参数**: `text` — 日期字符串；`formatter` — 格式化器
- **返回**: `LocalDate`

### getYear

```java
public int getYear()
```

获取年份字段。

- **返回**: `int` — 年份

### getMonthValue

```java
public int getMonthValue()
```

获取月份字段（1-12）。

- **返回**: `int` — 月份

### getMonth

```java
public Month getMonth()
```

获取月份枚举。

- **返回**: `Month` — 月份枚举

### getDayOfMonth

```java
public int getDayOfMonth()
```

获取日期（月中的第几天，1-31）。

- **返回**: `int` — 日

### getDayOfYear

```java
public int getDayOfYear()
```

获取当年第几天（1-366）。

- **返回**: `int` — 当年第几天

### getDayOfWeek

```java
public DayOfWeek getDayOfWeek()
```

获取星期枚举（MONDAY-SUNDAY）。

- **返回**: `DayOfWeek` — 星期枚举

### isLeapYear

```java
public boolean isLeapYear()
```

判断是否为闰年。

- **返回**: `boolean`

### lengthOfMonth

```java
public int lengthOfMonth()
```

返回当月的天数（28-31）。

- **返回**: `int` — 当月天数

### lengthOfYear

```java
public int lengthOfYear()
```

返回当年的天数（365 或 366）。

- **返回**: `int` — 当年天数

### plusDays

```java
public LocalDate plusDays(long daysToAdd)
```

增加天数。

- **参数**: `daysToAdd` — 要增加的天数
- **返回**: `LocalDate`

### plusWeeks

```java
public LocalDate plusWeeks(long weeksToAdd)
```

增加周数。

- **参数**: `weeksToAdd` — 要增加的周数
- **返回**: `LocalDate`

### plusMonths

```java
public LocalDate plusMonths(long monthsToAdd)
```

增加月数。

- **参数**: `monthsToAdd` — 要增加的月数
- **返回**: `LocalDate`

### plusYears

```java
public LocalDate plusYears(long yearsToAdd)
```

增加年数。

- **参数**: `yearsToAdd` — 要增加的年数
- **返回**: `LocalDate`

### minusDays

```java
public LocalDate minusDays(long daysToSubtract)
```

减少天数。

- **参数**: `daysToSubtract` — 要减少的天数
- **返回**: `LocalDate`

### minusWeeks

```java
public LocalDate minusWeeks(long weeksToSubtract)
```

减少周数。

- **参数**: `weeksToSubtract` — 要减少的周数
- **返回**: `LocalDate`

### minusMonths

```java
public LocalDate minusMonths(long monthsToSubtract)
```

减少月数。

- **参数**: `monthsToSubtract` — 要减少的月数
- **返回**: `LocalDate`

### minusYears

```java
public LocalDate minusYears(long yearsToSubtract)
```

减少年数。

- **参数**: `yearsToSubtract` — 要减少的年数
- **返回**: `LocalDate`

### withDayOfMonth

```java
public LocalDate withDayOfMonth(int dayOfMonth)
```

设置日期（月中的第几天），返回新实例。

- **参数**: `dayOfMonth` — 新日期
- **返回**: `LocalDate`

### withMonth

```java
public LocalDate withMonth(int month)
```

设置月份，返回新实例。

- **参数**: `month` — 新月份（1-12）
- **返回**: `LocalDate`

### withYear

```java
public LocalDate withYear(int year)
```

设置年份，返回新实例。

- **参数**: `year` — 新年份
- **返回**: `LocalDate`

### isBefore

```java
public boolean isBefore(LocalDate other)
```

判断该日期是否在另一个日期之前。

- **参数**: `other` — 另一个日期
- **返回**: `boolean`

### isAfter

```java
public boolean isAfter(LocalDate other)
```

判断该日期是否在另一个日期之后。

- **参数**: `other` — 另一个日期
- **返回**: `boolean`

### compareTo

```java
public int compareTo(LocalDate other)
```

比较两个日期的先后顺序。

- **参数**: `other` — 另一个日期
- **返回**: `int` — 负数表示早于，正数表示晚于

### equals

```java
public boolean equals(Object obj)
```

判断两个日期是否相等。

- **参数**: `obj` — 比较对象
- **返回**: `boolean`

### isEqual

```java
public boolean isEqual(LocalDate other)
```

判断两个日期是否相等。

- **参数**: `other` — 另一个日期
- **返回**: `boolean`

### atTime

```java
public LocalDateTime atTime(LocalTime time)
```

与指定时间组合，创建 `LocalDateTime`。

- **参数**: `time` — 时间
- **返回**: `LocalDateTime`

### atStartOfDay

```java
public LocalDateTime atStartOfDay()
```

创建该日期开始时刻的 `LocalDateTime`（00:00）。

- **返回**: `LocalDateTime` — 该日期 00:00

### toEpochDay

```java
public long toEpochDay()
```

将日期转换为纪元天数。

- **返回**: `long` — 从 1970-01-01 到该日期的天数

### format

```java
public String format(DateTimeFormatter formatter)
```

使用指定格式化器将日期格式化为字符串。

- **参数**: `formatter` — 格式化器
- **返回**: `String` — 格式化后的字符串

### toString

```java
public String toString()
```

返回日期的字符串表示，如 "2026-07-23"。

- **返回**: `String`

### until

```java
public long until(Temporal endDate, TemporalUnit unit)
```

计算到结束日期的时间量。

- **参数**: `endDate` — 结束日期；`unit` — 时间单位
- **返回**: `long`

## 测试

### of

- 描述: 使用 of() 创建日期
- 断言: 2026-07-22 的年为 2026，月为 7，日为 22

```java
// 方法体开始
System.out.println("=== of ===");
LocalDate date = LocalDate.of(2026, 7, 22);
System.out.println("创建日期: " + date);
assertEquals(2026, date.getYear());
assertEquals(7, date.getMonthValue());
assertEquals(22, date.getDayOfMonth());
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### ofWithMonthEnum

- 描述: 使用 of() 和 Month 枚举创建日期
- 断言: 2026-07-22 的 Month 为 JULY

```java
// 方法体开始
System.out.println("=== ofWithMonthEnum ===");
LocalDate date = LocalDate.of(2026, Month.JULY, 22);
System.out.println("创建日期: " + date);
assertEquals(Month.JULY, date.getMonth());
assertEquals(7, date.getMonthValue());
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### ofYearDay

- 描述: 根据年+天数创建日期
- 断言: 2026 年第 200 天是 2026-07-19

```java
// 方法体开始
System.out.println("=== ofYearDay ===");
LocalDate date = LocalDate.ofYearDay(2026, 200);
System.out.println("2026年第200天: " + date);
assertEquals(7, date.getMonthValue());
assertEquals(19, date.getDayOfMonth());
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### ofEpochDay

- 描述: 从纪元天数创建日期
- 断言: 0 天 = 1970-01-01

```java
// 方法体开始
System.out.println("=== ofEpochDay ===");
LocalDate date = LocalDate.ofEpochDay(0);
System.out.println("纪元第一天: " + date);
assertEquals(1970, date.getYear());
assertEquals(1, date.getMonthValue());
assertEquals(1, date.getDayOfMonth());
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### now

- 描述: 获取当前日期
- 断言: now() 返回的 LocalDate 不为 null

```java
// 方法体开始
System.out.println("=== now ===");
LocalDate now = LocalDate.now();
System.out.println("当前日期: " + now);
assertNotNull(now);
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### now(Clock)

- 描述: 从指定时钟获取日期
- 断言: 固定时钟返回日期与时钟一致

```java
// 方法体开始
System.out.println("=== now(Clock) ===");
java.time.Clock clock = java.time.Clock.fixed(java.time.Instant.parse("2026-07-22T10:30:00Z"), java.time.ZoneId.of("UTC"));
LocalDate date = LocalDate.now(clock);
System.out.println("指定时钟日期: " + date);
assertEquals(2026, date.getYear());
assertEquals(22, date.getDayOfMonth());
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### now(ZoneId)

- 描述: 从指定时区获取日期
- 断言: now(ZoneId) 返回的 LocalDate 不为 null

```java
// 方法体开始
System.out.println("=== now(ZoneId) ===");
LocalDate date = LocalDate.now(java.time.ZoneId.of("Asia/Shanghai"));
System.out.println("上海时区当前日期: " + date);
assertNotNull(date);
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### parse

- 描述: 解析日期字符串
- 断言: "2026-07-22" 解析后年为 2026，月为 7，日为 22

```java
// 方法体开始
System.out.println("=== parse ===");
LocalDate date = LocalDate.parse("2026-07-22");
System.out.println("解析结果: " + date);
assertEquals(2026, date.getYear());
assertEquals(7, date.getMonthValue());
assertEquals(22, date.getDayOfMonth());
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### parse(DateTimeFormatter)

- 描述: 使用自定义格式解析日期
- 断言: "2026/07/22" 按 yyyy/MM/dd 格式解析后年为 2026

```java
// 方法体开始
System.out.println("=== parse(DateTimeFormatter) ===");
java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("yyyy/MM/dd");
LocalDate date = LocalDate.parse("2026/07/22", formatter);
System.out.println("解析结果: " + date);
assertEquals(2026, date.getYear());
assertEquals(7, date.getMonthValue());
assertEquals(22, date.getDayOfMonth());
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### getYear

- 描述: 获取年份
- 断言: 2026-07-22 的年份是 2026

```java
// 方法体开始
System.out.println("=== getYear ===");
LocalDate date = LocalDate.of(2026, 7, 22);
System.out.println("年份: " + date.getYear());
assertEquals(2026, date.getYear());
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### getMonth

- 描述: 获取月份枚举和值
- 断言: 7月22日的月份值为 7，枚举为 JULY

```java
// 方法体开始
System.out.println("=== getMonth ===");
LocalDate date = LocalDate.of(2026, 7, 22);
assertEquals(7, date.getMonthValue());
assertEquals(Month.JULY, date.getMonth());
System.out.println("月份值: " + date.getMonthValue() + ", 枚举: " + date.getMonth());
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### getMonthValue

- 描述: 仅获取月份数值
- 断言: 7月22日的月份值为 7

```java
// 方法体开始
System.out.println("=== getMonthValue ===");
LocalDate date = LocalDate.of(2026, 7, 22);
assertEquals(7, date.getMonthValue());
System.out.println("月份数值: " + date.getMonthValue());
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### getDayOfMonth

- 描述: 获取日期（月中的第几天）
- 断言: 2026-07-22 的 dayOfMonth 为 22

```java
// 方法体开始
System.out.println("=== getDayOfMonth ===");
LocalDate date = LocalDate.of(2026, 7, 22);
assertEquals(22, date.getDayOfMonth());
System.out.println("日: " + date.getDayOfMonth());
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### getDayOfYear

- 描述: 获取当年第几天
- 断言: 2026-07-22 是当年的第 203 天

```java
// 方法体开始
System.out.println("=== getDayOfYear ===");
LocalDate date = LocalDate.of(2026, 7, 22);
System.out.println("2026-07-22 是当年第 " + date.getDayOfYear() + " 天");
assertEquals(203, date.getDayOfYear());
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### getDayOfWeek

- 描述: 获取星期
- 断言: 2026-07-22 是星期三

```java
// 方法体开始
System.out.println("=== getDayOfWeek ===");
LocalDate date = LocalDate.of(2026, 7, 22);
System.out.println("2026-07-22 是: " + date.getDayOfWeek());
assertEquals(DayOfWeek.WEDNESDAY, date.getDayOfWeek());
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### isLeapYear

- 描述: 判断闰年
- 断言: 2024 是闰年，2025 不是

```java
// 方法体开始
System.out.println("=== isLeapYear ===");
LocalDate leap = LocalDate.of(2024, 2, 1);
LocalDate nonLeap = LocalDate.of(2025, 2, 1);
System.out.println("2024 是闰年? " + leap.isLeapYear());
System.out.println("2025 是闰年? " + nonLeap.isLeapYear());
assertTrue(leap.isLeapYear());
assertFalse(nonLeap.isLeapYear());
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### lengthOfMonth

- 描述: 当月天数
- 断言: 2026年7月有31天，2025年2月有28天，2024年2月有29天

```java
// 方法体开始
System.out.println("=== lengthOfMonth ===");
LocalDate july = LocalDate.of(2026, 7, 1);
LocalDate feb2025 = LocalDate.of(2025, 2, 1);
LocalDate feb2024 = LocalDate.of(2024, 2, 1);
System.out.println("7月: " + july.lengthOfMonth() + "天, 2025年2月: " + feb2025.lengthOfMonth() + "天, 2024年2月: " + feb2024.lengthOfMonth() + "天");
assertEquals(31, july.lengthOfMonth());
assertEquals(28, feb2025.lengthOfMonth());
assertEquals(29, feb2024.lengthOfMonth());
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### lengthOfYear

- 描述: 当年天数
- 断言: 2024年有366天（闰年），2025年有365天

```java
// 方法体开始
System.out.println("=== lengthOfYear ===");
LocalDate leap = LocalDate.of(2024, 1, 1);
LocalDate nonLeap = LocalDate.of(2025, 1, 1);
System.out.println("2024年: " + leap.lengthOfYear() + "天, 2025年: " + nonLeap.lengthOfYear() + "天");
assertEquals(366, leap.lengthOfYear());
assertEquals(365, nonLeap.lengthOfYear());
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### plusDays

- 描述: 加天数
- 断言: 2026-07-22 + 10天 = 2026-08-01

```java
// 方法体开始
System.out.println("=== plusDays ===");
LocalDate date = LocalDate.of(2026, 7, 22);
LocalDate result = date.plusDays(10);
System.out.println(date + " + 10天 = " + result);
assertEquals(8, result.getMonthValue());
assertEquals(1, result.getDayOfMonth());
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### plusWeeks

- 描述: 加周数
- 断言: 2026-07-22 + 2周 = 2026-08-05

```java
// 方法体开始
System.out.println("=== plusWeeks ===");
LocalDate date = LocalDate.of(2026, 7, 22);
LocalDate result = date.plusWeeks(2);
System.out.println(date + " + 2周 = " + result);
assertEquals(8, result.getMonthValue());
assertEquals(5, result.getDayOfMonth());
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### plusMonths

- 描述: 加月数
- 断言: 2026-07-22 + 3月 = 2026-10-22

```java
// 方法体开始
System.out.println("=== plusMonths ===");
LocalDate date = LocalDate.of(2026, 7, 22);
LocalDate result = date.plusMonths(3);
System.out.println(date + " + 3月 = " + result);
assertEquals(10, result.getMonthValue());
assertEquals(22, result.getDayOfMonth());
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### plusYears

- 描述: 加年数
- 断言: 2026-07-22 + 1年 = 2027-07-22

```java
// 方法体开始
System.out.println("=== plusYears ===");
LocalDate date = LocalDate.of(2026, 7, 22);
LocalDate result = date.plusYears(1);
System.out.println(date + " + 1年 = " + result);
assertEquals(2027, result.getYear());
assertEquals(7, result.getMonthValue());
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### minusDays

- 描述: 减天数
- 断言: 2026-07-22 - 22天 = 2026-06-30

```java
// 方法体开始
System.out.println("=== minusDays ===");
LocalDate date = LocalDate.of(2026, 7, 22);
LocalDate result = date.minusDays(22);
System.out.println(date + " - 22天 = " + result);
assertEquals(6, result.getMonthValue());
assertEquals(30, result.getDayOfMonth());
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### minusMonths

- 描述: 减月数
- 断言: 2026-07-22 - 6月 = 2026-01-22

```java
// 方法体开始
System.out.println("=== minusMonths ===");
LocalDate date = LocalDate.of(2026, 7, 22);
LocalDate result = date.minusMonths(6);
System.out.println(date + " - 6月 = " + result);
assertEquals(1, result.getMonthValue());
assertEquals(22, result.getDayOfMonth());
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### minusWeeks

- 描述: 减周数
- 断言: 2026-07-22 - 2周 = 2026-07-08

```java
// 方法体开始
System.out.println("=== minusWeeks ===");
LocalDate date = LocalDate.of(2026, 7, 22);
LocalDate result = date.minusWeeks(2);
System.out.println(date + " - 2周 = " + result);
assertEquals(8, result.getDayOfMonth());
assertEquals(7, result.getMonthValue());
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### minusYears

- 描述: 减年数
- 断言: 2026-07-22 - 10年 = 2016-07-22

```java
// 方法体开始
System.out.println("=== minusYears ===");
LocalDate date = LocalDate.of(2026, 7, 22);
LocalDate result = date.minusYears(10);
System.out.println(date + " - 10年 = " + result);
assertEquals(2016, result.getYear());
assertEquals(7, result.getMonthValue());
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### withDayOfMonth

- 描述: 设置日期（日）
- 断言: 2026-07-22 设置日为 1 后为 2026-07-01

```java
// 方法体开始
System.out.println("=== withDayOfMonth ===");
LocalDate date = LocalDate.of(2026, 7, 22);
LocalDate result = date.withDayOfMonth(1);
System.out.println(date + " withDayOfMonth(1) = " + result);
assertEquals(1, result.getDayOfMonth());
assertEquals(7, result.getMonthValue());
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### withMonth

- 描述: 设置月份
- 断言: 2026-07-22 设置月为 12 后为 2026-12-22

```java
// 方法体开始
System.out.println("=== withMonth ===");
LocalDate date = LocalDate.of(2026, 7, 22);
LocalDate result = date.withMonth(12);
System.out.println(date + " withMonth(12) = " + result);
assertEquals(12, result.getMonthValue());
assertEquals(22, result.getDayOfMonth());
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### withYear

- 描述: 设置年份
- 断言: 2026-07-22 设置年为 2028 后为 2028-07-22

```java
// 方法体开始
System.out.println("=== withYear ===");
LocalDate date = LocalDate.of(2026, 7, 22);
LocalDate result = date.withYear(2028);
System.out.println(date + " withYear(2028) = " + result);
assertEquals(2028, result.getYear());
assertEquals(7, result.getMonthValue());
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### isBefore

- 描述: 判断日期先后
- 断言: 2026-07-22 在 2026-07-23 之前

```java
// 方法体开始
System.out.println("=== isBefore ===");
LocalDate d1 = LocalDate.of(2026, 7, 22);
LocalDate d2 = LocalDate.of(2026, 7, 23);
assertTrue(d1.isBefore(d2));
assertFalse(d2.isBefore(d1));
assertTrue(d2.isAfter(d1));
System.out.println(d1 + " isBefore " + d2 + " ? " + d1.isBefore(d2));
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### isAfter

- 描述: 判断日期是否在另一个日期之后
- 断言: 2026-07-23 在 2026-07-22 之后

```java
// 方法体开始
System.out.println("=== isAfter ===");
LocalDate d1 = LocalDate.of(2026, 7, 22);
LocalDate d2 = LocalDate.of(2026, 7, 23);
assertTrue(d2.isAfter(d1));
assertFalse(d1.isAfter(d2));
System.out.println(d2 + " isAfter " + d1 + " ? " + d2.isAfter(d1));
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### compareTo

- 描述: 比较两个日期
- 断言: 相同日期 compareTo 为 0

```java
// 方法体开始
System.out.println("=== compareTo ===");
LocalDate d1 = LocalDate.of(2026, 7, 22);
LocalDate d2 = LocalDate.of(2026, 7, 22);
LocalDate d3 = LocalDate.of(2026, 7, 23);
assertEquals(0, d1.compareTo(d2));
assertTrue(d1.compareTo(d3) < 0);
assertTrue(d3.compareTo(d1) > 0);
assertTrue(d1.equals(d2));
assertTrue(d1.isEqual(d2));
assertFalse(d1.equals(d3));
System.out.println(d1 + " compareTo " + d2 + " = " + d1.compareTo(d2));
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### equals

- 描述: 判断两个日期是否相等
- 断言: 相同日期返回 true，不同日期返回 false

```java
// 方法体开始
System.out.println("=== equals ===");
LocalDate d1 = LocalDate.of(2026, 7, 22);
LocalDate d2 = LocalDate.of(2026, 7, 22);
LocalDate d3 = LocalDate.of(2026, 7, 23);
assertTrue(d1.equals(d2));
assertFalse(d1.equals(d3));
System.out.println(d1 + " equals " + d2 + " ? " + d1.equals(d2));
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### isEqual

- 描述: 使用 isEqual 方法判断日期相等
- 断言: 相同日期 isEqual 返回 true

```java
// 方法体开始
System.out.println("=== isEqual ===");
LocalDate d1 = LocalDate.of(2026, 7, 22);
LocalDate d2 = LocalDate.of(2026, 7, 22);
LocalDate d3 = LocalDate.of(2026, 7, 23);
assertTrue(d1.isEqual(d2));
assertFalse(d1.isEqual(d3));
System.out.println(d1 + " isEqual " + d2 + " ? " + d1.isEqual(d2));
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### atTime

- 描述: 日期和时间组合
- 断言: 日期 + 时间 = LocalDateTime

```java
// 方法体开始
System.out.println("=== atTime ===");
LocalDate date = LocalDate.of(2026, 7, 22);
LocalDateTime dateTime = date.atTime(15, 30);
System.out.println(date + " atTime(15:30) = " + dateTime);
assertEquals(15, dateTime.getHour());
assertEquals(30, dateTime.getMinute());
assertEquals(22, dateTime.getDayOfMonth());
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### atStartOfDay

- 描述: 获取日期开始时刻
- 断言: atStartOfDay 返回该日 00:00

```java
// 方法体开始
System.out.println("=== atStartOfDay ===");
LocalDate date = LocalDate.of(2026, 7, 22);
LocalDateTime start = date.atStartOfDay();
System.out.println(date + " atStartOfDay = " + start);
assertEquals(0, start.getHour());
assertEquals(0, start.getMinute());
assertEquals(22, start.getDayOfMonth());
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### toEpochDay

- 描述: 转换为纪元天数
- 断言: 1970-01-01 的纪元天数为 0

```java
// 方法体开始
System.out.println("=== toEpochDay ===");
LocalDate epoch = LocalDate.of(1970, 1, 1);
assertEquals(0, epoch.toEpochDay());
LocalDate later = LocalDate.of(2026, 7, 22);
System.out.println(epoch + " 纪元天数: " + epoch.toEpochDay());
System.out.println(later + " 纪元天数: " + later.toEpochDay());
assertTrue(later.toEpochDay() > 0);
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### format

- 描述: 格式化日期
- 断言: 2026-07-22 按 yyyy/MM/dd 格式化为 "2026/07/22"

```java
// 方法体开始
System.out.println("=== format ===");
LocalDate date = LocalDate.of(2026, 7, 22);
java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("yyyy/MM/dd");
String formatted = date.format(formatter);
System.out.println("格式化结果: " + formatted);
assertEquals("2026/07/22", formatted);
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### toString

- 描述: 日期字符串表示
- 断言: 2026-07-22 输出为 "2026-07-22"

```java
// 方法体开始
System.out.println("=== toString ===");
LocalDate date = LocalDate.of(2026, 7, 22);
assertEquals("2026-07-22", date.toString());
System.out.println("toString: " + date.toString());
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### until

- 描述: 计算两个日期之间的时间量
- 断言: 2026-07-22 到 2026-08-01 相差 10 天

```java
// 方法体开始
System.out.println("=== until ===");
LocalDate start = LocalDate.of(2026, 7, 22);
LocalDate end = LocalDate.of(2026, 8, 1);
long days = start.until(end, java.time.temporal.ChronoUnit.DAYS);
System.out.println(start + " 到 " + end + " 相差 " + days + " 天");
assertEquals(10, days);
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### immutable

- 描述: 不可变性
- 断言: plus/minus/with 返回新实例，原对象不变

```java
// 方法体开始
System.out.println("=== immutable ===");
LocalDate date = LocalDate.of(2026, 7, 22);
LocalDate result = date.plusDays(5);
assertNotSame(date, result);
assertEquals(22, date.getDayOfMonth());
assertEquals(27, result.getDayOfMonth());
System.out.println("原日期: " + date + ", 新日期: " + result);
System.out.println("=== 测试通过 ===");
// 方法体结束
```
