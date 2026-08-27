---
name: MonthDay
package: java.time
order: 80
---

## 介绍

`java.time.MonthDay` 是 Java 8 日期时间 API 中表示**月日**的不可变类，如 `--12-25`（圣诞节）。它只包含月份和日期，不包含年份，非常适合表示生日、节日、纪念日等每年重复的日期。

MonthDay 的核心特点：
- **不可变且线程安全**
- **无年份**：只关注月和日，适合周期性事件
- **与 Year 配合**：`Year.atMonthDay(MonthDay)` 可以组合为完整的 LocalDate
- **范围验证**：自动验证月份和日期的有效性（如 2 月 29 日虽然允许，但组合到平年时会报错）

MonthDay 的常用方法：
- `now()` — 获取当前月日
- `of(Month, int)` / `of(int, int)` — 从月份和日期创建
- `parse(CharSequence)` — 从 `"--12-25"` 格式解析
- `getMonth()` / `getMonthValue()` / `getDayOfMonth()` — 获取月份和日期
- `isValidYear(int)` — 检查指定年份是否有效（如 2 月 29 日只在闰年有效）
- `atYear(int)` — 与年份组合为 LocalDate
- `isBefore(MonthDay)` / `isAfter(MonthDay)` — 比较

## 方法

### now

```java
public static MonthDay now()
```

获取系统默认时区的当前月日。

- **返回**: `MonthDay` — 当前月日

### of

```java
public static MonthDay of(Month month, int dayOfMonth)
public static MonthDay of(int month, int dayOfMonth)
```

从月份和日期创建 MonthDay。

- **参数**: `month` — 月份；`dayOfMonth` — 日（1-31）
- **返回**: `MonthDay` — 对应的 MonthDay 对象

### parse

```java
public static MonthDay parse(CharSequence text)
```

从字符串解析，格式为 `"--12-25"`。

- **参数**: `text` — 月日字符串
- **返回**: `MonthDay` — 解析结果

### getMonth / getMonthValue / getDayOfMonth

```java
public Month getMonth()
public int getMonthValue()
public int getDayOfMonth()
```

获取月份（枚举）、月份值（1-12）、日（1-31）。

### isValidYear

```java
public boolean isValidYear(int year)
```

检查指定年份是否能使当前 MonthDay 有效（主要用于 2 月 29 日只在闰年有效）。

- **参数**: `year` — 要检查的年份
- **返回**: `boolean` — 是否有效

### atYear

```java
public LocalDate atYear(int year)
```

将 MonthDay 与年份组合为 LocalDate。如果当前为 2 月 29 日且指定年份不是闰年，抛出异常。

- **参数**: `year` — 年份
- **返回**: `LocalDate` — 组合后的日期

## 测试

### of 和获取属性

- 描述: 使用 `of` 创建月日并获取属性
- 断言: 月日值正确

```java
// 方法体开始
System.out.println("=== of / get ===");
MonthDay md = MonthDay.of(12, 25);
assertEquals(12, md.getMonthValue());
assertEquals(25, md.getDayOfMonth());
assertEquals(Month.DECEMBER, md.getMonth());
System.out.println("圣诞节: " + md);
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### parse

- 描述: 使用 `parse` 从字符串解析月日
- 断言: 解析正确

```java
// 方法体开始
System.out.println("=== parse ===");
MonthDay md = MonthDay.parse("--01-01");
assertEquals(1, md.getMonthValue());
assertEquals(1, md.getDayOfMonth());
System.out.println("元旦: " + md);
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### atYear

- 描述: 使用 `atYear` 将生日与年份组合为 LocalDate
- 断言: 组合后的日期正确

```java
// 方法体开始
System.out.println("=== atYear ===");
MonthDay birthday = MonthDay.of(5, 20);
LocalDate date = birthday.atYear(2024);
assertEquals(2024, date.getYear());
assertEquals(5, date.getMonthValue());
assertEquals(20, date.getDayOfMonth());
System.out.println("2024 年生日: " + date);
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### isValidYear 闰年

- 描述: 检查 2 月 29 日在不同年份的有效性
- 断言: 2024 年有效（闰年），2023 年无效

```java
// 方法体开始
System.out.println("=== isValidYear ===");
MonthDay leapDay = MonthDay.of(2, 29);
assertTrue(leapDay.isValidYear(2024));  // 闰年
assertFalse(leapDay.isValidYear(2023)); // 平年
System.out.println("2月29日 在 2024 年有效: " + leapDay.isValidYear(2024));
System.out.println("2月29日 在 2023 年有效: " + leapDay.isValidYear(2023));
System.out.println("=== 测试通过 ===");
// 方法体结束
```
