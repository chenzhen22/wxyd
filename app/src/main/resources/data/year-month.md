---
name: YearMonth
package: java.time
order: 79
---

## 介绍

`java.time.YearMonth` 是 Java 8 日期时间 API 中表示**年月**的不可变类，如 `2024-07`。它表示年份和月份的组合，不包含日、时区等信息。

YearMonth 的核心特点：
- **不可变且线程安全**
- **范围合理**：年份从 -999,999,999 到 999,999,999，月份 1-12
- **实用场景**：信用卡有效期、月度报表、分期付款等按月的业务
- **配合 TemporalAdjusters** 可以获取该月的第一天、最后一天等

YearMonth 的常用方法：
- `now()` — 获取当前年月
- `of(int year, int month)` — 从年份和月份创建（month 可以是 Month 枚举或 1-12 整数）
- `parse(CharSequence)` — 从 `"2024-07"` 格式解析
- `getYear()` / `getMonth()` / `getMonthValue()` — 获取年份和月份
- `lengthOfMonth()` / `lengthOfYear()` — 当月/当年天数
- `isLeapYear()` — 是否闰年
- `minusMonths(long)` / `plusMonths(long)` — 加减月份
- `atDay(int)` — 组合为 LocalDate
- `isBefore(YearMonth)` / `isAfter(YearMonth)` — 比较

## 方法

### now

```java
public static YearMonth now()
```

获取系统默认时区的当前年月。

- **返回**: `YearMonth` — 当前年月

### of

```java
public static YearMonth of(int year, Month month)
public static YearMonth of(int year, int month)
```

从年份和月份创建 YearMonth。

- **参数**: `year` — 年份；`month` — 月份（Month 枚举或 1-12）
- **返回**: `YearMonth` — 对应的 YearMonth 对象

### parse

```java
public static YearMonth parse(CharSequence text)
```

从字符串解析，格式为 `"2024-07"`。

- **参数**: `text` — 年月字符串
- **返回**: `YearMonth` — 解析结果

### getYear / getMonth / getMonthValue

```java
public int getYear()
public Month getMonth()
public int getMonthValue()
```

获取年份、月份（枚举）、月份值（1-12）。

### lengthOfMonth

```java
public int lengthOfMonth()
```

返回当月天数（考虑闰年）。

- **返回**: `int` — 当月天数

### lengthOfYear

```java
public int lengthOfYear()
```

返回当年天数（366 或 365）。

- **返回**: `int` — 当年天数

### isLeapYear

```java
public boolean isLeapYear()
```

检查当前年份是否为闰年。

- **返回**: `boolean` — 是否闰年

### minusMonths / plusMonths

```java
public YearMonth minusMonths(long monthsToSubtract)
public YearMonth plusMonths(long monthsToAdd)
```

返回减去/加上指定月数后的 YearMonth。跨年自动处理。

- **参数**: `monthsToSubtract` / `monthsToAdd` — 月数
- **返回**: `YearMonth` — 运算后的 YearMonth

## 测试

### of 和获取属性

- 描述: 使用 `of` 创建年月并获取属性
- 断言: 年月值正确

```java
// 方法体开始
System.out.println("=== of / get ===");
YearMonth ym = YearMonth.of(2024, 7);
assertEquals(2024, ym.getYear());
assertEquals(7, ym.getMonthValue());
assertEquals(Month.JULY, ym.getMonth());
System.out.println("YearMonth: " + ym);
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### lengthOfMonth

- 描述: 获取不同月份的天数
- 断言: 2024 年 2 月 29 天（闰年），2023 年 2 月 28 天

```java
// 方法体开始
System.out.println("=== lengthOfMonth ===");
assertEquals(29, YearMonth.of(2024, 2).lengthOfMonth());  // 闰年
assertEquals(28, YearMonth.of(2023, 2).lengthOfMonth());  // 平年
assertEquals(31, YearMonth.of(2024, 1).lengthOfMonth());
assertEquals(30, YearMonth.of(2024, 4).lengthOfMonth());
System.out.println("2024-02 天数: " + YearMonth.of(2024, 2).lengthOfMonth());
System.out.println("2023-02 天数: " + YearMonth.of(2023, 2).lengthOfMonth());
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### plusMonths 跨年

- 描述: 使用 `plusMonths` 跨年
- 断言: 2024-11 + 3 个月 = 2025-02

```java
// 方法体开始
System.out.println("=== plusMonths 跨年 ===");
YearMonth ym = YearMonth.of(2024, 11);
YearMonth result = ym.plusMonths(3);
assertEquals(2025, result.getYear());
assertEquals(2, result.getMonthValue());
System.out.println("2024-11 + 3 个月 = " + result);
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### isLeapYear

- 描述: 使用 `isLeapYear` 检查闰年
- 断言: 2024 年 2 月是闰年

```java
// 方法体开始
System.out.println("=== isLeapYear ===");
assertTrue(YearMonth.of(2024, 2).isLeapYear());
assertFalse(YearMonth.of(2023, 2).isLeapYear());
System.out.println("2024 闰年: " + YearMonth.of(2024, 2).isLeapYear());
System.out.println("=== 测试通过 ===");
// 方法体结束
```
