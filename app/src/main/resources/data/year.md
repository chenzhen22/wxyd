---
name: Year
package: java.time
order: 68
---

## 介绍

`java.time.Year` 是 Java 8 日期时间 API 中表示**年份**的不可变类。它代表完整的年份，如 `2024`，可以用于处理与年份相关的操作。

Year 的核心特点：
- **不可变且线程安全**：所有 Year 实例都是不可变的
- **范围**：`MIN_VALUE` = -999,999,999 到 `MAX_VALUE` = 999,999,999
- **丰富的方法**：检查闰年、获取年份长度、与 MonthDay/LocalDate 配合
- **解析友好**：支持 `Year.parse("2024")` 直接解析字符串

Year 的常用方法：
- `now()` — 获取当前年份
- `of(int)` — 从年份值创建
- `parse(CharSequence)` — 从字符串解析
- `isLeap()` — 是否为闰年
- `length()` — 返回年份天数（366 或 365）
- `atMonth(Month)` / `atMonthDay(MonthDay)` — 组合为 YearMonth / LocalDate
- `plusYears(long)` / `minusYears(long)` — 加减年份
- `getValue()` — 获取年份值

## 方法

### now

```java
public static Year now()
```

获取系统默认时区的当前年份。

- **返回**: `Year` — 当前年份

### of

```java
public static Year of(int year)
```

从年份值创建 Year。

- **参数**: `year` — 年份值（支持负数表示公元前）
- **返回**: `Year` — 对应的 Year 对象

### parse

```java
public static Year parse(CharSequence text)
```

从字符串解析 Year，格式必须为 `"2024"`。

- **参数**: `text` — 年份字符串
- **返回**: `Year` — 解析结果

### getValue

```java
public int getValue()
```

获取年份值。

- **返回**: `int` — 年份值

### isLeap

```java
public boolean isLeap()
```

检查当前年份是否为闰年。

- **返回**: `boolean` — 是否为闰年

### length

```java
public int length()
```

返回年份的天数（闰年 366，平年 365）。

- **返回**: `int` — 天数

### atMonth

```java
public YearMonth atMonth(Month month)
public YearMonth atMonth(int month)
```

将年份与月份组合为 `YearMonth`。

- **参数**: `month` — 月份（Month 枚举或 1-12 的整数）
- **返回**: `YearMonth` — 年月对象

### minusYears / plusYears

```java
public Year minusYears(long yearsToSubtract)
public Year plusYears(long yearsToAdd)
```

返回减去/加上指定年数后的 Year。

- **参数**: `yearsToSubtract` / `yearsToAdd` — 年数
- **返回**: `Year` — 运算后的 Year

## 测试

### of 和 getValue

- 描述: 使用 `of` 创建年份并获取年份值
- 断言: 年份值正确

```java
// 方法体开始
System.out.println("=== of / getValue ===");
Year year = Year.of(2024);
assertEquals(2024, year.getValue());
Year bcYear = Year.of(-500);
assertEquals(-500, bcYear.getValue());
System.out.println("年份: " + year + ", 公元前: " + bcYear);
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### isLeap

- 描述: 检查闰年
- 断言: 2024 是闰年，2023 不是

```java
// 方法体开始
System.out.println("=== isLeap ===");
assertTrue(Year.of(2024).isLeap());
assertFalse(Year.of(2023).isLeap());
assertTrue(Year.of(2000).isLeap());  // 世纪闰年
assertFalse(Year.of(1900).isLeap()); // 非闰年
System.out.println("2024 闰年: " + Year.of(2024).isLeap());
System.out.println("2000 闰年: " + Year.of(2000).isLeap());
System.out.println("1900 闰年: " + Year.of(1900).isLeap());
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### length

- 描述: 获取年份天数
- 断言: 2024 闰年 366 天，2023 平年 365 天

```java
// 方法体开始
System.out.println("=== length ===");
assertEquals(366, Year.of(2024).length());
assertEquals(365, Year.of(2023).length());
System.out.println("2024 天数: " + Year.of(2024).length());
System.out.println("2023 天数: " + Year.of(2023).length());
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### atMonth

- 描述: 使用 `atMonth` 将 Year 与 Month 组合为 YearMonth
- 断言: 2024 年 12 月的 YearMonth 正确

```java
// 方法体开始
System.out.println("=== atMonth ===");
YearMonth ym = Year.of(2024).atMonth(Month.DECEMBER);
assertEquals(2024, ym.getYear());
assertEquals(12, ym.getMonthValue());
System.out.println("YearMonth: " + ym);
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### plusYears / minusYears

- 描述: 加减年份
- 断言: 2024 + 1 = 2025, 2024 - 10 = 2014

```java
// 方法体开始
System.out.println("=== plusYears / minusYears ===");
Year year = Year.of(2024);
assertEquals(2025, year.plusYears(1).getValue());
assertEquals(2014, year.minusYears(10).getValue());
assertEquals(2024, year.getValue());  // 不可变，原对象不变
System.out.println("2024 + 1: " + year.plusYears(1));
System.out.println("2024 - 10: " + year.minusYears(10));
System.out.println("=== 测试通过 ===");
// 方法体结束
```
