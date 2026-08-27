---
name: LocalTime
package: java.time
order: 3
---

## 介绍

`java.time.LocalTime` 是 Java 8 日期时间 API 中表示**不含日期和时区的时间**的不可变类。它精确到纳秒，用于表示如 "10:15:30" 这样的时间。

`LocalTime` 是不可变且线程安全的，所有修改操作都会返回新实例。

常见用途：
- **时间处理**：获取/设置/加减时、分、秒、纳秒
- **时间比较**：判断先后顺序、比较大小
- **格式解析**：解析和格式化时间字符串
- **时间计算**：计算两个时间之间的差值

## 方法

### now

```java
public static LocalTime now()
```

从系统默认时区获取当前时间。

- **返回**: `LocalTime` — 当前时间

### now(Clock)

```java
public static LocalTime now(Clock clock)
```

从指定时钟获取当前时间。

- **参数**: `clock` — 时钟对象
- **返回**: `LocalTime`

### now(ZoneId)

```java
public static LocalTime now(ZoneId zone)
```

从指定时区获取当前时间。

- **参数**: `zone` — 时区 ID
- **返回**: `LocalTime`

### of(hour, minute)

```java
public static LocalTime of(int hour, int minute)
```

根据时和分创建时间。

- **参数**: `hour` — 小时（0-23）；`minute` — 分钟（0-59）
- **返回**: `LocalTime`

### of(hour, minute, second)

```java
public static LocalTime of(int hour, int minute, int second)
```

根据时、分、秒创建时间。

- **参数**: `hour` — 小时；`minute` — 分钟；`second` — 秒
- **返回**: `LocalTime`

### of(hour, minute, second, nanoOfSecond)

```java
public static LocalTime of(int hour, int minute, int second, int nanoOfSecond)
```

根据时、分、秒、纳秒创建时间。

- **参数**: `hour` — 小时；`minute` — 分钟；`second` — 秒；`nanoOfSecond` — 纳秒
- **返回**: `LocalTime`

### ofSecondOfDay

```java
public static LocalTime ofSecondOfDay(long secondOfDay)
```

从当天秒数创建时间。

- **参数**: `secondOfDay` — 当天已过的秒数（0-86399）
- **返回**: `LocalTime`

### ofNanoOfDay

```java
public static LocalTime ofNanoOfDay(long nanoOfDay)
```

从当天纳秒数创建时间。

- **参数**: `nanoOfDay` — 当天已过的纳秒数
- **返回**: `LocalTime`

### parse

```java
public static LocalTime parse(CharSequence text)
```

解析形如 "10:15:30" 的时间字符串。

- **参数**: `text` — 时间字符串，如 "10:15" 或 "10:15:30"
- **返回**: `LocalTime`

### parse(DateTimeFormatter)

```java
public static LocalTime parse(CharSequence text, DateTimeFormatter formatter)
```

使用指定格式化器解析时间字符串。

- **参数**: `text` — 时间字符串；`formatter` — 格式化器
- **返回**: `LocalTime`

### getHour

```java
public int getHour()
```

获取小时字段（0-23）。

- **返回**: `int` — 小时数

### getMinute

```java
public int getMinute()
```

获取分钟字段（0-59）。

- **返回**: `int` — 分钟数

### getSecond

```java
public int getSecond()
```

获取秒字段（0-59）。

- **返回**: `int` — 秒数

### getNano

```java
public int getNano()
```

获取纳秒字段（0-999999999）。

- **返回**: `int` — 纳秒数

### withHour

```java
public LocalTime withHour(int hour)
```

设置小时，返回新实例。

- **参数**: `hour` — 新小时数
- **返回**: `LocalTime`

### withMinute

```java
public LocalTime withMinute(int minute)
```

设置分钟，返回新实例。

- **参数**: `minute` — 新分钟数
- **返回**: `LocalTime`

### withSecond

```java
public LocalTime withSecond(int second)
```

设置秒，返回新实例。

- **参数**: `second` — 新秒数
- **返回**: `LocalTime`

### withNano

```java
public LocalTime withNano(int nanoOfSecond)
```

设置纳秒，返回新实例。

- **参数**: `nanoOfSecond` — 新纳秒数
- **返回**: `LocalTime`

### plusHours

```java
public LocalTime plusHours(long hoursToAdd)
```

增加小时数。

- **参数**: `hoursToAdd` — 要增加的小时数（可为负）
- **返回**: `LocalTime`

### plusMinutes

```java
public LocalTime plusMinutes(long minutesToAdd)
```

增加分钟数。

- **参数**: `minutesToAdd` — 要增加的分钟数
- **返回**: `LocalTime`

### plusSeconds

```java
public LocalTime plusSeconds(long secondsToAdd)
```

增加秒数。

- **参数**: `secondsToAdd` — 要增加的秒数
- **返回**: `LocalTime`

### plusNanos

```java
public LocalTime plusNanos(long nanosToAdd)
```

增加纳秒数。

- **参数**: `nanosToAdd` — 要增加的纳秒数
- **返回**: `LocalTime`

### minusHours

```java
public LocalTime minusHours(long hoursToSubtract)
```

减少小时数。

- **参数**: `hoursToSubtract` — 要减少的小时数
- **返回**: `LocalTime`

### minusMinutes

```java
public LocalTime minusMinutes(long minutesToSubtract)
```

减少分钟数。

- **参数**: `minutesToSubtract` — 要减少的分钟数
- **返回**: `LocalTime`

### minusSeconds

```java
public LocalTime minusSeconds(long secondsToSubtract)
```

减少秒数。

- **参数**: `secondsToSubtract` — 要减少的秒数
- **返回**: `LocalTime`

### minusNanos

```java
public LocalTime minusNanos(long nanosToSubtract)
```

减少纳秒数。

- **参数**: `nanosToSubtract` — 要减少的纳秒数
- **返回**: `LocalTime`

### isAfter

```java
public boolean isAfter(LocalTime other)
```

判断该时间是否在另一个时间之后。

- **参数**: `other` — 另一个时间
- **返回**: `boolean`

### isBefore

```java
public boolean isBefore(LocalTime other)
```

判断该时间是否在另一个时间之前。

- **参数**: `other` — 另一个时间
- **返回**: `boolean`

### compareTo

```java
public int compareTo(LocalTime other)
```

比较两个时间的先后顺序。

- **参数**: `other` — 另一个时间
- **返回**: `int` — 负数表示早于，正数表示晚于

### equals

```java
public boolean equals(Object obj)
```

判断两个时间是否相等。

- **参数**: `obj` — 比较对象
- **返回**: `boolean`

### toSecondOfDay

```java
public int toSecondOfDay()
```

将时间转换为当天秒数。

- **返回**: `int` — 从午夜到该时间的秒数

### toNanoOfDay

```java
public long toNanoOfDay()
```

将时间转换为当天纳秒数。

- **返回**: `long` — 从午夜到该时间的纳秒数

### atDate

```java
public LocalDateTime atDate(LocalDate date)
```

与指定日期组合，创建 `LocalDateTime`。

- **参数**: `date` — 日期
- **返回**: `LocalDateTime`

### format

```java
public String format(DateTimeFormatter formatter)
```

使用指定格式化器将时间格式化为字符串。

- **参数**: `formatter` — 格式化器
- **返回**: `String` — 格式化后的字符串

### truncatedTo

```java
public LocalTime truncatedTo(TemporalUnit unit)
```

截断时间到指定单位。例如截断到分钟会清零秒和纳秒。

- **参数**: `unit` — 时间单位（如 ChronoUnit.MINUTES）
- **返回**: `LocalTime`

### toString

```java
public String toString()
```

返回时间的字符串表示，如 "10:15" 或 "10:15:30"。

- **返回**: `String`

## 测试

### of

- 描述: 使用 of() 创建时间
- 断言: 10:30 的小时为 10，分钟为 30

```java
// 方法体开始
System.out.println("=== of ===");
LocalTime time = LocalTime.of(10, 30);
System.out.println("创建时间: " + time);
assertEquals(10, time.getHour());
assertEquals(30, time.getMinute());
assertEquals(0, time.getSecond());
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### ofWithSeconds

- 描述: 使用 of() 创建带秒的时间
- 断言: 10:30:45 的各字段正确

```java
// 方法体开始
System.out.println("=== ofWithSeconds ===");
LocalTime time = LocalTime.of(10, 30, 45);
System.out.println("创建时间: " + time);
assertEquals(10, time.getHour());
assertEquals(30, time.getMinute());
assertEquals(45, time.getSecond());
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### ofWithNanos

- 描述: 使用 of() 创建带纳秒的时间
- 断言: of(10, 30, 45, 123456789) 的各字段正确

```java
// 方法体开始
System.out.println("=== ofWithNanos ===");
LocalTime time = LocalTime.of(10, 30, 45, 123456789);
System.out.println("创建带纳秒的时间: " + time);
assertEquals(10, time.getHour());
assertEquals(30, time.getMinute());
assertEquals(45, time.getSecond());
assertEquals(123456789, time.getNano());
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### now

- 描述: 获取当前时间
- 断言: now() 返回的 LocalTime 不为 null

```java
// 方法体开始
System.out.println("=== now ===");
LocalTime now = LocalTime.now();
System.out.println("当前时间: " + now);
assertNotNull(now);
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### now(Clock)

- 描述: 从指定时钟获取时间
- 断言: 固定时钟返回的时间与时钟一致

```java
// 方法体开始
System.out.println("=== now(Clock) ===");
java.time.Clock clock = java.time.Clock.fixed(java.time.Instant.parse("2026-07-23T10:30:00Z"), java.time.ZoneId.of("UTC"));
LocalTime time = LocalTime.now(clock);
System.out.println("指定时钟的时间: " + time);
assertEquals(10, time.getHour());
assertEquals(30, time.getMinute());
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### now(ZoneId)

- 描述: 从指定时区获取时间
- 断言: now(ZoneId) 返回的 LocalTime 不为 null

```java
// 方法体开始
System.out.println("=== now(ZoneId) ===");
LocalTime time = LocalTime.now(java.time.ZoneId.of("Asia/Shanghai"));
System.out.println("上海时区当前时间: " + time);
assertNotNull(time);
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### ofSecondOfDay

- 描述: 从秒数创建时间
- 断言: 86399 秒 = 23:59:59

```java
// 方法体开始
System.out.println("=== ofSecondOfDay ===");
LocalTime time = LocalTime.ofSecondOfDay(86399);
System.out.println("86399秒 = " + time);
assertEquals(23, time.getHour());
assertEquals(59, time.getMinute());
assertEquals(59, time.getSecond());
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### ofNanoOfDay

- 描述: 从纳秒数创建时间
- 断言: 0 纳秒 = 00:00:00，86399000000000 纳秒 = 23:59:50

```java
// 方法体开始
System.out.println("=== ofNanoOfDay ===");
LocalTime time1 = LocalTime.ofNanoOfDay(0);
System.out.println("0纳秒 = " + time1);
assertEquals(0, time1.getHour());
assertEquals(0, time1.getMinute());
assertEquals(0, time1.getSecond());
LocalTime time2 = LocalTime.ofNanoOfDay(86399000000000L);
System.out.println("86399000000000纳秒 = " + time2);
assertEquals(23, time2.getHour());
assertEquals(59, time2.getMinute());
assertEquals(59, time2.getSecond());
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### parse

- 描述: 解析时间字符串
- 断言: "14:30:00" 解析后小时为 14，分钟为 30

```java
// 方法体开始
System.out.println("=== parse ===");
LocalTime time = LocalTime.parse("14:30:00");
System.out.println("解析结果: " + time);
assertEquals(14, time.getHour());
assertEquals(30, time.getMinute());
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### parse(DateTimeFormatter)

- 描述: 使用指定格式化器解析时间字符串
- 断言: "14:30:00" 按 HH:mm:ss 格式解析后小时为 14

```java
// 方法体开始
System.out.println("=== parse(DateTimeFormatter) ===");
java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss");
LocalTime time = LocalTime.parse("14:30:00", formatter);
System.out.println("解析结果: " + time);
assertEquals(14, time.getHour());
assertEquals(30, time.getMinute());
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### getHour

- 描述: 获取小时字段
- 断言: 08:15:30 的小时为 8

```java
// 方法体开始
System.out.println("=== getHour ===");
LocalTime time = LocalTime.of(8, 15, 30);
System.out.println("时间: " + time + " 小时: " + time.getHour());
assertEquals(8, time.getHour());
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### getMinute

- 描述: 获取分钟字段
- 断言: 10:45:30 的分钟为 45

```java
// 方法体开始
System.out.println("=== getMinute ===");
LocalTime time = LocalTime.of(10, 45, 30);
System.out.println("时间: " + time + " 分钟: " + time.getMinute());
assertEquals(45, time.getMinute());
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### getSecond

- 描述: 获取秒字段
- 断言: 10:45:30 的秒为 30

```java
// 方法体开始
System.out.println("=== getSecond ===");
LocalTime time = LocalTime.of(10, 45, 30);
System.out.println("时间: " + time + " 秒: " + time.getSecond());
assertEquals(30, time.getSecond());
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### getNano

- 描述: 获取纳秒字段
- 断言: 10:45:30.123456789 的纳秒为 123456789

```java
// 方法体开始
System.out.println("=== getNano ===");
LocalTime time = LocalTime.of(10, 45, 30, 123456789);
System.out.println("时间: " + time + " 纳秒: " + time.getNano());
assertEquals(123456789, time.getNano());
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### plusHours

- 描述: 增加小时
- 断言: 10:00 加 3 小时后为 13:00

```java
// 方法体开始
System.out.println("=== plusHours ===");
LocalTime time = LocalTime.of(10, 0);
LocalTime result = time.plusHours(3);
System.out.println(time + " + 3小时 = " + result);
assertEquals(13, result.getHour());
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### plusMinutes

- 描述: 增加分钟
- 断言: 10:30 加 15 分钟后为 10:45

```java
// 方法体开始
System.out.println("=== plusMinutes ===");
LocalTime time = LocalTime.of(10, 30);
LocalTime result = time.plusMinutes(15);
System.out.println(time + " + 15分钟 = " + result);
assertEquals(10, result.getHour());
assertEquals(45, result.getMinute());
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### plusSeconds

- 描述: 增加秒数
- 断言: 10:30:15 加 30 秒后为 10:30:45

```java
// 方法体开始
System.out.println("=== plusSeconds ===");
LocalTime time = LocalTime.of(10, 30, 15);
LocalTime result = time.plusSeconds(30);
System.out.println(time + " + 30秒 = " + result);
assertEquals(45, result.getSecond());
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### plusNanos

- 描述: 增加纳秒数
- 断言: 10:30:15.0 加 500000000 纳秒后纳秒为 500000000

```java
// 方法体开始
System.out.println("=== plusNanos ===");
LocalTime time = LocalTime.of(10, 30, 15, 0);
LocalTime result = time.plusNanos(500000000);
System.out.println(time + " + 500000000纳秒 = " + result);
assertEquals(500000000, result.getNano());
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### minusHours

- 描述: 减少小时
- 断言: 10:00 减 3 小时后为 07:00

```java
// 方法体开始
System.out.println("=== minusHours ===");
LocalTime time = LocalTime.of(10, 0);
LocalTime result = time.minusHours(3);
System.out.println(time + " - 3小时 = " + result);
assertEquals(7, result.getHour());
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### minusMinutes

- 描述: 减少分钟
- 断言: 10:30 减 15 分钟后为 10:15

```java
// 方法体开始
System.out.println("=== minusMinutes ===");
LocalTime time = LocalTime.of(10, 30);
LocalTime result = time.minusMinutes(15);
System.out.println(time + " - 15分钟 = " + result);
assertEquals(10, result.getHour());
assertEquals(15, result.getMinute());
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### minusSeconds

- 描述: 减少秒数
- 断言: 10:30:45 减 30 秒后为 10:30:15

```java
// 方法体开始
System.out.println("=== minusSeconds ===");
LocalTime time = LocalTime.of(10, 30, 45);
LocalTime result = time.minusSeconds(30);
System.out.println(time + " - 30秒 = " + result);
assertEquals(15, result.getSecond());
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### minusNanos

- 描述: 减少纳秒数
- 断言: 10:30:15.500000000 减 200000000 纳秒后纳秒为 300000000

```java
// 方法体开始
System.out.println("=== minusNanos ===");
LocalTime time = LocalTime.of(10, 30, 15, 500000000);
LocalTime result = time.minusNanos(200000000);
System.out.println(time + " - 200000000纳秒 = " + result);
assertEquals(300000000, result.getNano());
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### withHour

- 描述: 设置小时
- 断言: 10:30 设置小时为 20 后为 20:30

```java
// 方法体开始
System.out.println("=== withHour ===");
LocalTime time = LocalTime.of(10, 30);
LocalTime result = time.withHour(20);
System.out.println(time + " 设置 hour=20 -> " + result);
assertEquals(20, result.getHour());
assertEquals(30, result.getMinute());
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### withMinute

- 描述: 设置分钟
- 断言: 10:15 设置分钟为 45 后为 10:45

```java
// 方法体开始
System.out.println("=== withMinute ===");
LocalTime time = LocalTime.of(10, 15);
LocalTime result = time.withMinute(45);
System.out.println(time + " 设置 minute=45 -> " + result);
assertEquals(10, result.getHour());
assertEquals(45, result.getMinute());
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### withSecond

- 描述: 设置秒
- 断言: 10:30:00 设置秒为 45 后为 10:30:45

```java
// 方法体开始
System.out.println("=== withSecond ===");
LocalTime time = LocalTime.of(10, 30, 0);
LocalTime result = time.withSecond(45);
System.out.println(time + " 设置 second=45 -> " + result);
assertEquals(45, result.getSecond());
assertEquals(10, result.getHour());
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### withNano

- 描述: 设置纳秒
- 断言: 10:30:15 设置纳秒为 987654321 后纳秒为 987654321

```java
// 方法体开始
System.out.println("=== withNano ===");
LocalTime time = LocalTime.of(10, 30, 15, 0);
LocalTime result = time.withNano(987654321);
System.out.println(time + " 设置 nano=987654321 -> " + result);
assertEquals(987654321, result.getNano());
assertEquals(15, result.getSecond());
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### isAfter

- 描述: 判断时间是否在另一个时间之后
- 断言: 10:00 在 09:00 之后

```java
// 方法体开始
System.out.println("=== isAfter ===");
LocalTime early = LocalTime.of(9, 0);
LocalTime late = LocalTime.of(10, 0);
System.out.println(late + " isAfter " + early + " ? " + late.isAfter(early));
assertTrue(late.isAfter(early));
assertFalse(early.isAfter(late));
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### isBefore

- 描述: 判断时间是否在另一个时间之前
- 断言: 09:00 在 10:00 之前

```java
// 方法体开始
System.out.println("=== isBefore ===");
LocalTime early = LocalTime.of(9, 0);
LocalTime late = LocalTime.of(10, 0);
System.out.println(early + " isBefore " + late + " ? " + early.isBefore(late));
assertTrue(early.isBefore(late));
assertFalse(late.isBefore(early));
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### toSecondOfDay

- 描述: 转换为秒
- 断言: 01:30:00 = 5400 秒

```java
// 方法体开始
System.out.println("=== toSecondOfDay ===");
LocalTime time = LocalTime.of(1, 30, 0);
int seconds = time.toSecondOfDay();
System.out.println(time + " toSecondOfDay = " + seconds);
assertEquals(5400, seconds);
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### toNanoOfDay

- 描述: 转换为纳秒
- 断言: 01:30:00.0 = 5400000000000 纳秒

```java
// 方法体开始
System.out.println("=== toNanoOfDay ===");
LocalTime time = LocalTime.of(1, 30, 0, 0);
long nanos = time.toNanoOfDay();
System.out.println(time + " toNanoOfDay = " + nanos);
assertEquals(5400000000000L, nanos);
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### atDate

- 描述: 时间和日期组合
- 断言: 时间 + 日期 = LocalDateTime

```java
// 方法体开始
System.out.println("=== atDate ===");
LocalTime time = LocalTime.of(15, 30);
LocalDate date = LocalDate.of(2026, 7, 22);
LocalDateTime dateTime = time.atDate(date);
System.out.println(time + " atDate " + date + " = " + dateTime);
assertEquals(15, dateTime.getHour());
assertEquals(7, dateTime.getMonthValue());
assertEquals(22, dateTime.getDayOfMonth());
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### format

- 描述: 使用格式化器格式化时间
- 断言: 14:30:15 按 HH:mm:ss 格式化为 "14:30:15"

```java
// 方法体开始
System.out.println("=== format ===");
LocalTime time = LocalTime.of(14, 30, 15);
java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss");
String formatted = time.format(formatter);
System.out.println("格式化结果: " + formatted);
assertEquals("14:30:15", formatted);
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### truncatedTo

- 描述: 截断时间到分钟
- 断言: 10:30:45 截断到分钟后为 10:30:00

```java
// 方法体开始
System.out.println("=== truncatedTo ===");
LocalTime time = LocalTime.of(10, 30, 45);
LocalTime truncated = time.truncatedTo(java.time.temporal.ChronoUnit.MINUTES);
System.out.println(time + " truncatedTo MINUTES = " + truncated);
assertEquals(0, truncated.getSecond());
assertEquals(30, truncated.getMinute());
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### compareTo

- 描述: 比较两个时间，同时测试 equals
- 断言: 10:00 等于 10:00，小于 11:00

```java
// 方法体开始
System.out.println("=== compareTo ===");
LocalTime t1 = LocalTime.of(10, 0);
LocalTime t2 = LocalTime.of(10, 0);
LocalTime t3 = LocalTime.of(11, 0);
System.out.println(t1 + " compareTo " + t2 + " = " + t1.compareTo(t2));
assertEquals(0, t1.compareTo(t2));
assertTrue(t1.compareTo(t3) < 0);
assertTrue(t3.compareTo(t1) > 0);
assertTrue(t1.equals(t2));
assertFalse(t1.equals(t3));
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### equals

- 描述: 判断两个时间是否相等
- 断言: 相同时间返回 true，不同时间返回 false

```java
// 方法体开始
System.out.println("=== equals ===");
LocalTime t1 = LocalTime.of(10, 0);
LocalTime t2 = LocalTime.of(10, 0);
LocalTime t3 = LocalTime.of(11, 0);
System.out.println(t1 + " equals " + t2 + " ? " + t1.equals(t2));
assertTrue(t1.equals(t2));
assertFalse(t1.equals(t3));
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### toString

- 描述: 测试 toString 输出
- 断言: 10:30 输出为 "10:30"，10:30:45 输出为 "10:30:45"

```java
// 方法体开始
System.out.println("=== toString ===");
LocalTime t1 = LocalTime.of(10, 30);
LocalTime t2 = LocalTime.of(10, 30, 45);
System.out.println("toString: " + t1.toString() + ", " + t2.toString());
assertEquals("10:30", t1.toString());
assertEquals("10:30:45", t2.toString());
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### immutable

- 描述: 不可变性
- 断言: plus/minus 返回新实例，原对象不变

```java
// 方法体开始
System.out.println("=== immutable ===");
LocalTime time = LocalTime.of(10, 0);
LocalTime result = time.plusHours(5);
System.out.println("原时间: " + time + ", 新时间: " + result);
assertNotSame(time, result);
assertEquals(10, time.getHour());
assertEquals(15, result.getHour());
System.out.println("=== 测试通过 ===");
// 方法体结束
```
