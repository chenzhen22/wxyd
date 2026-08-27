---
name: LocalDateTime
package: java.time
order: 6
---

## 介绍

`java.time.LocalDateTime` 是 Java 8 日期时间 API 中表示**不含时区的日期和时间**的不可变类。它组合了 `LocalDate` 和 `LocalTime` 的功能，精确到纳秒。

`LocalDateTime` 是不可变且线程安全的，所有修改操作都会返回新实例。

常见用途：
- **完整时间戳**：表示不含时区的日期+时间
- **时间计算**：同时操作日期和时间字段
- **格式解析**：解析和格式化日期时间字符串
- **与日期/时间互转**：拆分为 LocalDate 和 LocalTime

## 方法

### now

```java
public static LocalDateTime now()
```

从系统默认时区获取当前日期时间。

- **返回**: `LocalDateTime` — 当前日期时间

### now(Clock)

```java
public static LocalDateTime now(Clock clock)
```

从指定时钟获取当前日期时间。

- **参数**: `clock` — 时钟对象
- **返回**: `LocalDateTime`

### now(ZoneId)

```java
public static LocalDateTime now(ZoneId zone)
```

从指定时区获取当前日期时间。

- **参数**: `zone` — 时区 ID
- **返回**: `LocalDateTime`

### of

```java
public static LocalDateTime of(int year, int month, int dayOfMonth, int hour, int minute)
```

根据年、月、日、时、分创建日期时间。

- **参数**: `year` — 年；`month` — 月；`dayOfMonth` — 日；`hour` — 时；`minute` — 分
- **返回**: `LocalDateTime`

### of(LocalDate, LocalTime)

```java
public static LocalDateTime of(LocalDate date, LocalTime time)
```

根据 LocalDate 和 LocalTime 创建日期时间。

- **参数**: `date` — 日期；`time` — 时间
- **返回**: `LocalDateTime`

### parse

```java
public static LocalDateTime parse(CharSequence text)
```

解析形如 "2026-07-22T10:30:00" 的日期时间字符串。

- **参数**: `text` — 日期时间字符串
- **返回**: `LocalDateTime`

### parse(DateTimeFormatter)

```java
public static LocalDateTime parse(CharSequence text, DateTimeFormatter formatter)
```

使用指定格式化器解析日期时间字符串。

- **参数**: `text` — 日期时间字符串；`formatter` — 格式化器
- **返回**: `LocalDateTime`

### getYear

```java
public int getYear()
```

获取年份字段。

- **返回**: `int`

### getMonthValue

```java
public int getMonthValue()
```

获取月份字段（1-12）。

- **返回**: `int`

### getMonth

```java
public Month getMonth()
```

获取月份枚举。

- **返回**: `Month`

### getDayOfMonth

```java
public int getDayOfMonth()
```

获取日期（月中的第几天）。

- **返回**: `int`

### getDayOfWeek

```java
public DayOfWeek getDayOfWeek()
```

获取星期枚举。

- **返回**: `DayOfWeek`

### getHour

```java
public int getHour()
```

获取小时字段（0-23）。

- **返回**: `int`

### getMinute

```java
public int getMinute()
```

获取分钟字段（0-59）。

- **返回**: `int`

### getSecond

```java
public int getSecond()
```

获取秒字段（0-59）。

- **返回**: `int`

### getNano

```java
public int getNano()
```

获取纳秒字段（0-999999999）。

- **返回**: `int`

### plusDays

```java
public LocalDateTime plusDays(long days)
```

增加天数。

- **参数**: `days` — 要增加的天数
- **返回**: `LocalDateTime`

### plusHours

```java
public LocalDateTime plusHours(long hours)
```

增加小时数。

- **参数**: `hours` — 要增加的小时数
- **返回**: `LocalDateTime`

### plusMinutes

```java
public LocalDateTime plusMinutes(long minutes)
```

增加分钟数。

- **参数**: `minutes` — 要增加的分钟数
- **返回**: `LocalDateTime`

### minusDays

```java
public LocalDateTime minusDays(long days)
```

减少天数。

- **参数**: `days` — 要减少的天数
- **返回**: `LocalDateTime`

### minusHours

```java
public LocalDateTime minusHours(long hours)
```

减少小时数。

- **参数**: `hours` — 要减少的小时数
- **返回**: `LocalDateTime`

### withHour

```java
public LocalDateTime withHour(int hour)
```

设置小时，返回新实例。

- **参数**: `hour` — 新小时数
- **返回**: `LocalDateTime`

### withMinute

```java
public LocalDateTime withMinute(int minute)
```

设置分钟，返回新实例。

- **参数**: `minute` — 新分钟数
- **返回**: `LocalDateTime`

### toLocalDate

```java
public LocalDate toLocalDate()
```

获取日期部分。

- **返回**: `LocalDate`

### toLocalTime

```java
public LocalTime toLocalTime()
```

获取时间部分。

- **返回**: `LocalTime`

### isBefore

```java
public boolean isBefore(LocalDateTime other)
```

判断该日期时间是否在另一个之前。

- **参数**: `other` — 另一个日期时间
- **返回**: `boolean`

### isAfter

```java
public boolean isAfter(LocalDateTime other)
```

判断该日期时间是否在另一个之后。

- **参数**: `other` — 另一个日期时间
- **返回**: `boolean`

### compareTo

```java
public int compareTo(LocalDateTime other)
```

比较两个日期时间的先后顺序。

- **参数**: `other` — 另一个日期时间
- **返回**: `int`

### equals

```java
public boolean equals(Object obj)
```

判断两个日期时间是否相等。

- **参数**: `obj` — 比较对象
- **返回**: `boolean`

### format

```java
public String format(DateTimeFormatter formatter)
```

使用指定格式化器将日期时间格式化为字符串。

- **参数**: `formatter` — 格式化器
- **返回**: `String`

### toString

```java
public String toString()
```

返回日期时间的字符串表示，如 "2026-07-22T10:30:00"。

- **返回**: `String`

### truncatedTo

```java
public LocalDateTime truncatedTo(TemporalUnit unit)
```

截断日期时间到指定单位。

- **参数**: `unit` — 时间单位
- **返回**: `LocalDateTime`

## 测试

### of

- 描述: 使用 of() 创建日期时间
- 断言: 2026-07-22T10:30 的各字段正确

```java
// 方法体开始
System.out.println("=== of ===");
LocalDateTime dt = LocalDateTime.of(2026, 7, 22, 10, 30);
System.out.println("创建日期时间: " + dt);
assertEquals(2026, dt.getYear());
assertEquals(7, dt.getMonthValue());
assertEquals(22, dt.getDayOfMonth());
assertEquals(10, dt.getHour());
assertEquals(30, dt.getMinute());
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### ofWithSeconds

- 描述: 使用 of() 创建带秒的日期时间
- 断言: 2026-07-22T10:30:45 的各字段正确

```java
// 方法体开始
System.out.println("=== ofWithSeconds ===");
LocalDateTime dt = LocalDateTime.of(2026, 7, 22, 10, 30, 45);
System.out.println("创建日期时间: " + dt);
assertEquals(45, dt.getSecond());
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### ofDateAndTime

- 描述: 使用 of(LocalDate, LocalTime) 创建
- 断言: 合并后的字段正确

```java
// 方法体开始
System.out.println("=== ofDateAndTime ===");
LocalDate date = LocalDate.of(2026, 7, 22);
LocalTime time = LocalTime.of(15, 30);
LocalDateTime dt = LocalDateTime.of(date, time);
System.out.println("日期+时间合并: " + dt);
assertEquals(22, dt.getDayOfMonth());
assertEquals(15, dt.getHour());
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### now

- 描述: 获取当前日期时间
- 断言: now() 返回不为 null

```java
// 方法体开始
System.out.println("=== now ===");
LocalDateTime now = LocalDateTime.now();
System.out.println("当前日期时间: " + now);
assertNotNull(now);
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### now(Clock)

- 描述: 从指定时钟获取日期时间
- 断言: 固定时钟返回正确值

```java
// 方法体开始
System.out.println("=== now(Clock) ===");
java.time.Clock clock = java.time.Clock.fixed(java.time.Instant.parse("2026-07-22T10:30:00Z"), java.time.ZoneId.of("UTC"));
LocalDateTime dt = LocalDateTime.now(clock);
System.out.println("指定时钟日期时间: " + dt);
assertEquals(2026, dt.getYear());
assertEquals(10, dt.getHour());
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### now(ZoneId)

- 描述: 从指定时区获取日期时间
- 断言: now(ZoneId) 返回不为 null

```java
// 方法体开始
System.out.println("=== now(ZoneId) ===");
LocalDateTime dt = LocalDateTime.now(java.time.ZoneId.of("Asia/Shanghai"));
System.out.println("上海时区当前日期时间: " + dt);
assertNotNull(dt);
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### parse

- 描述: 解析日期时间字符串
- 断言: "2026-07-22T10:30:00" 解析后各字段正确

```java
// 方法体开始
System.out.println("=== parse ===");
LocalDateTime dt = LocalDateTime.parse("2026-07-22T10:30:00");
System.out.println("解析结果: " + dt);
assertEquals(2026, dt.getYear());
assertEquals(7, dt.getMonthValue());
assertEquals(22, dt.getDayOfMonth());
assertEquals(10, dt.getHour());
assertEquals(30, dt.getMinute());
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### parse(DateTimeFormatter)

- 描述: 使用自定义格式解析
- 断言: "2026/07/22 10:30" 按格式解析正确

```java
// 方法体开始
System.out.println("=== parse(DateTimeFormatter) ===");
java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm");
LocalDateTime dt = LocalDateTime.parse("2026/07/22 10:30", formatter);
System.out.println("自定义格式解析: " + dt);
assertEquals(2026, dt.getYear());
assertEquals(10, dt.getHour());
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### getFields

- 描述: 获取各字段
- 断言: 各 get 方法返回值正确

```java
// 方法体开始
System.out.println("=== getFields ===");
LocalDateTime dt = LocalDateTime.of(2026, 7, 22, 10, 30, 45, 123456789);
System.out.println("日期时间: " + dt);
assertEquals(2026, dt.getYear());
assertEquals(7, dt.getMonthValue());
assertEquals(Month.JULY, dt.getMonth());
assertEquals(22, dt.getDayOfMonth());
assertEquals(DayOfWeek.WEDNESDAY, dt.getDayOfWeek());
assertEquals(10, dt.getHour());
assertEquals(30, dt.getMinute());
assertEquals(45, dt.getSecond());
assertEquals(123456789, dt.getNano());
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### getYear

- 描述: 获取年份
- 断言: 2026-07-22T10:30 的年份为 2026

```java
// 方法体开始
System.out.println("=== getYear ===");
LocalDateTime dt = LocalDateTime.of(2026, 7, 22, 10, 30);
assertEquals(2026, dt.getYear());
System.out.println("年份: " + dt.getYear());
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### getMonthValue

- 描述: 获取月份数值
- 断言: 2026-07-22T10:30 的月份为 7

```java
// 方法体开始
System.out.println("=== getMonthValue ===");
LocalDateTime dt = LocalDateTime.of(2026, 7, 22, 10, 30);
assertEquals(7, dt.getMonthValue());
System.out.println("月份: " + dt.getMonthValue());
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### getMonth

- 描述: 获取月份枚举
- 断言: 7月的 Month 枚举为 JULY

```java
// 方法体开始
System.out.println("=== getMonth ===");
LocalDateTime dt = LocalDateTime.of(2026, 7, 22, 10, 30);
assertEquals(Month.JULY, dt.getMonth());
System.out.println("月份枚举: " + dt.getMonth());
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### getDayOfMonth

- 描述: 获取日期（月中的第几天）
- 断言: 2026-07-22T10:30 的日期为 22

```java
// 方法体开始
System.out.println("=== getDayOfMonth ===");
LocalDateTime dt = LocalDateTime.of(2026, 7, 22, 10, 30);
assertEquals(22, dt.getDayOfMonth());
System.out.println("日: " + dt.getDayOfMonth());
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### getDayOfWeek

- 描述: 获取星期枚举
- 断言: 2026-07-22 是星期三

```java
// 方法体开始
System.out.println("=== getDayOfWeek ===");
LocalDateTime dt = LocalDateTime.of(2026, 7, 22, 10, 30);
assertEquals(DayOfWeek.WEDNESDAY, dt.getDayOfWeek());
System.out.println("星期: " + dt.getDayOfWeek());
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### getHour

- 描述: 获取小时字段
- 断言: 2026-07-22T10:30 的小时为 10

```java
// 方法体开始
System.out.println("=== getHour ===");
LocalDateTime dt = LocalDateTime.of(2026, 7, 22, 10, 30);
assertEquals(10, dt.getHour());
System.out.println("小时: " + dt.getHour());
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### getMinute

- 描述: 获取分钟字段
- 断言: 2026-07-22T10:30 的分钟为 30

```java
// 方法体开始
System.out.println("=== getMinute ===");
LocalDateTime dt = LocalDateTime.of(2026, 7, 22, 10, 30);
assertEquals(30, dt.getMinute());
System.out.println("分钟: " + dt.getMinute());
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### getSecond

- 描述: 获取秒字段
- 断言: 2026-07-22T10:30:45 的秒为 45

```java
// 方法体开始
System.out.println("=== getSecond ===");
LocalDateTime dt = LocalDateTime.of(2026, 7, 22, 10, 30, 45);
assertEquals(45, dt.getSecond());
System.out.println("秒: " + dt.getSecond());
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### getNano

- 描述: 获取纳秒字段
- 断言: 纳秒为 123456789

```java
// 方法体开始
System.out.println("=== getNano ===");
LocalDateTime dt = LocalDateTime.of(2026, 7, 22, 10, 30, 45, 123456789);
assertEquals(123456789, dt.getNano());
System.out.println("纳秒: " + dt.getNano());
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### isAfter

- 描述: 判断日期时间先后
- 断言: 较晚的日期时间 isAfter 返回 true

```java
// 方法体开始
System.out.println("=== isAfter ===");
LocalDateTime dt1 = LocalDateTime.of(2026, 7, 22, 10, 0);
LocalDateTime dt2 = LocalDateTime.of(2026, 7, 22, 11, 0);
assertTrue(dt2.isAfter(dt1));
assertFalse(dt1.isAfter(dt2));
System.out.println(dt2 + " isAfter " + dt1 + " ? " + dt2.isAfter(dt1));
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### equals

- 描述: 判断两个日期时间是否相等
- 断言: 相同日期时间返回 true

```java
// 方法体开始
System.out.println("=== equals ===");
LocalDateTime dt1 = LocalDateTime.of(2026, 7, 22, 10, 0);
LocalDateTime dt2 = LocalDateTime.of(2026, 7, 22, 10, 0);
LocalDateTime dt3 = LocalDateTime.of(2026, 7, 22, 11, 0);
assertTrue(dt1.equals(dt2));
assertFalse(dt1.equals(dt3));
System.out.println(dt1 + " equals " + dt2 + " ? " + dt1.equals(dt2));
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### plusDays

- 描述: 加天数
- 断言: 2026-07-22T10:30 + 10天 = 2026-08-01T10:30

```java
// 方法体开始
System.out.println("=== plusDays ===");
LocalDateTime dt = LocalDateTime.of(2026, 7, 22, 10, 30);
LocalDateTime result = dt.plusDays(10);
System.out.println(dt + " + 10天 = " + result);
assertEquals(8, result.getMonthValue());
assertEquals(1, result.getDayOfMonth());
assertEquals(10, result.getHour());
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### plusHours

- 描述: 加小时数
- 断言: 2026-07-22T10:30 + 15小时 = 2026-07-23T01:30

```java
// 方法体开始
System.out.println("=== plusHours ===");
LocalDateTime dt = LocalDateTime.of(2026, 7, 22, 10, 30);
LocalDateTime result = dt.plusHours(15);
System.out.println(dt + " + 15小时 = " + result);
assertEquals(23, result.getDayOfMonth());
assertEquals(1, result.getHour());
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### plusMinutes

- 描述: 加分钟数
- 断言: 10:30 + 90分钟 = 12:00

```java
// 方法体开始
System.out.println("=== plusMinutes ===");
LocalDateTime dt = LocalDateTime.of(2026, 7, 22, 10, 30);
LocalDateTime result = dt.plusMinutes(90);
System.out.println(dt + " + 90分钟 = " + result);
assertEquals(12, result.getHour());
assertEquals(0, result.getMinute());
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### minusDays

- 描述: 减天数
- 断言: 2026-07-22 - 22天 = 2026-06-30

```java
// 方法体开始
System.out.println("=== minusDays ===");
LocalDateTime dt = LocalDateTime.of(2026, 7, 22, 10, 30);
LocalDateTime result = dt.minusDays(22);
System.out.println(dt + " - 22天 = " + result);
assertEquals(6, result.getMonthValue());
assertEquals(30, result.getDayOfMonth());
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### minusHours

- 描述: 减小时数
- 断言: 10:30 - 15小时 = 前一天的19:30

```java
// 方法体开始
System.out.println("=== minusHours ===");
LocalDateTime dt = LocalDateTime.of(2026, 7, 22, 10, 30);
LocalDateTime result = dt.minusHours(15);
System.out.println(dt + " - 15小时 = " + result);
assertEquals(21, result.getDayOfMonth());
assertEquals(19, result.getHour());
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### withHour

- 描述: 设置小时
- 断言: 10:30 设置小时为 20 后为 20:30

```java
// 方法体开始
System.out.println("=== withHour ===");
LocalDateTime dt = LocalDateTime.of(2026, 7, 22, 10, 30);
LocalDateTime result = dt.withHour(20);
assertEquals(20, result.getHour());
assertEquals(30, result.getMinute());
System.out.println(dt + " withHour(20) = " + result);
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### withMinute

- 描述: 设置分钟
- 断言: 10:15 设置分钟为 45 后为 10:45

```java
// 方法体开始
System.out.println("=== withMinute ===");
LocalDateTime dt = LocalDateTime.of(2026, 7, 22, 10, 15);
LocalDateTime result = dt.withMinute(45);
assertEquals(45, result.getMinute());
System.out.println(dt + " withMinute(45) = " + result);
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### toLocalDate

- 描述: 获取日期部分
- 断言: toLocalDate 返回正确日期

```java
// 方法体开始
System.out.println("=== toLocalDate ===");
LocalDateTime dt = LocalDateTime.of(2026, 7, 22, 10, 30);
LocalDate date = dt.toLocalDate();
System.out.println("日期部分: " + date);
assertEquals(2026, date.getYear());
assertEquals(7, date.getMonthValue());
assertEquals(22, date.getDayOfMonth());
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### toLocalTime

- 描述: 获取时间部分
- 断言: toLocalTime 返回正确时间

```java
// 方法体开始
System.out.println("=== toLocalTime ===");
LocalDateTime dt = LocalDateTime.of(2026, 7, 22, 10, 30, 45);
LocalTime time = dt.toLocalTime();
System.out.println("时间部分: " + time);
assertEquals(10, time.getHour());
assertEquals(30, time.getMinute());
assertEquals(45, time.getSecond());
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### isBefore

- 描述: 比较日期时间先后
- 断言: 较早的日期时间 isBefore 返回 true

```java
// 方法体开始
System.out.println("=== isBefore ===");
LocalDateTime dt1 = LocalDateTime.of(2026, 7, 22, 10, 0);
LocalDateTime dt2 = LocalDateTime.of(2026, 7, 22, 11, 0);
assertTrue(dt1.isBefore(dt2));
assertTrue(dt2.isAfter(dt1));
System.out.println(dt1 + " isBefore " + dt2 + " ? " + dt1.isBefore(dt2));
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### compareTo

- 描述: 比较两个日期时间
- 断言: 相同时间 compareTo 为 0

```java
// 方法体开始
System.out.println("=== compareTo ===");
LocalDateTime dt1 = LocalDateTime.of(2026, 7, 22, 10, 0);
LocalDateTime dt2 = LocalDateTime.of(2026, 7, 22, 10, 0);
LocalDateTime dt3 = LocalDateTime.of(2026, 7, 22, 11, 0);
assertEquals(0, dt1.compareTo(dt2));
assertTrue(dt1.compareTo(dt3) < 0);
assertTrue(dt3.compareTo(dt1) > 0);
assertTrue(dt1.equals(dt2));
assertFalse(dt1.equals(dt3));
System.out.println(dt1 + " compareTo " + dt2 + " = " + dt1.compareTo(dt2));
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### format

- 描述: 格式化日期时间
- 断言: 格式化为 "yyyy-MM-dd HH:mm:ss" 正确

```java
// 方法体开始
System.out.println("=== format ===");
LocalDateTime dt = LocalDateTime.of(2026, 7, 22, 10, 30, 45);
java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
String formatted = dt.format(formatter);
System.out.println("格式化结果: " + formatted);
assertEquals("2026-07-22 10:30:45", formatted);
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### toString

- 描述: 字符串表示
- 断言: 2026-07-22T10:30 输出为 "2026-07-22T10:30"

```java
// 方法体开始
System.out.println("=== toString ===");
LocalDateTime dt = LocalDateTime.of(2026, 7, 22, 10, 30);
System.out.println("toString: " + dt.toString());
assertEquals("2026-07-22T10:30", dt.toString());
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### truncatedTo

- 描述: 截断到小时
- 断言: 10:30:45 截断到小时后为 10:00

```java
// 方法体开始
System.out.println("=== truncatedTo ===");
LocalDateTime dt = LocalDateTime.of(2026, 7, 22, 10, 30, 45);
LocalDateTime truncated = dt.truncatedTo(java.time.temporal.ChronoUnit.HOURS);
System.out.println(dt + " truncatedTo HOURS = " + truncated);
assertEquals(0, truncated.getMinute());
assertEquals(0, truncated.getSecond());
assertEquals(10, truncated.getHour());
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### immutable

- 描述: 不可变性
- 断言: plus 操作返回新实例，原对象不变

```java
// 方法体开始
System.out.println("=== immutable ===");
LocalDateTime dt = LocalDateTime.of(2026, 7, 22, 10, 0);
LocalDateTime result = dt.plusHours(5);
assertNotSame(dt, result);
assertEquals(10, dt.getHour());
assertEquals(15, result.getHour());
System.out.println("原对象: " + dt + ", 新对象: " + result);
System.out.println("=== 测试通过 ===");
// 方法体结束
```
