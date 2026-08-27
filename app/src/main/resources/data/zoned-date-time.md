---
name: ZonedDateTime
package: java.time
order: 12
---

## 介绍

`java.time.ZonedDateTime` 是 Java 8 日期时间 API 中表示**带时区的日期时间**的不可变类。它包含 `LocalDateTime` 和 `ZoneId`（时区）以及 `ZoneOffset`（偏移量）的信息。

`ZonedDateTime` 是不可变且线程安全的，所有修改操作都会返回新实例。

常见用途：
- **时区转换**：在不同时区之间转换时间
- **全球时间处理**：处理不同时区的时间数据
- **夏令时处理**：自动处理夏令时调整
- **时间比较**：统一为标准时间进行比较

## 方法

### now

```java
public static ZonedDateTime now()
```

从系统默认时区获取当前日期时间。

- **返回**: `ZonedDateTime` — 当前带时区的日期时间

### now(ZoneId)

```java
public static ZonedDateTime now(ZoneId zone)
```

从指定时区获取当前日期时间。

- **参数**: `zone` — 时区 ID
- **返回**: `ZonedDateTime`

### now(Clock)

```java
public static ZonedDateTime now(Clock clock)
```

从指定时钟获取当前日期时间。

- **参数**: `clock` — 时钟对象
- **返回**: `ZonedDateTime`

### of

```java
public static ZonedDateTime of(int year, int month, int dayOfMonth, int hour, int minute, int second, int nanoOfSecond, ZoneId zone)
```

根据年、月、日、时、分、秒、纳秒和时区创建 ZonedDateTime。

- **参数**: `year` — 年；`month` — 月；`dayOfMonth` — 日；`hour` — 时；`minute` — 分；`second` — 秒；`nanoOfSecond` — 纳秒；`zone` — 时区
- **返回**: `ZonedDateTime`

### parse

```java
public static ZonedDateTime parse(CharSequence text)
```

解析形如 "2026-07-23T10:30:00+08:00[Asia/Shanghai]" 的日期时间字符串。

- **参数**: `text` — 日期时间字符串
- **返回**: `ZonedDateTime`

### getZone

```java
public ZoneId getZone()
```

获取时区信息。

- **返回**: `ZoneId` — 时区 ID

### getOffset

```java
public ZoneOffset getOffset()
```

获取与 UTC 的偏移量。

- **返回**: `ZoneOffset` — 偏移量，如 +08:00

### toLocalDate

```java
public LocalDate toLocalDate()
```

获取日期部分。

- **返回**: `LocalDate` — 日期部分

### toLocalTime

```java
public LocalTime toLocalTime()
```

获取时间部分。

- **返回**: `LocalTime` — 时间部分

### toInstant

```java
public Instant toInstant()
```

转换为 Instant 时间戳（转换为 UTC 时刻）。

- **返回**: `Instant` — 时间戳

### withZoneSameInstant

```java
public ZonedDateTime withZoneSameInstant(ZoneId zone)
```

转换到另一时区，保持同一时刻（时间点不变）。

- **参数**: `zone` — 目标时区
- **返回**: `ZonedDateTime` — 新时区的日期时间

### withZoneSameLocal

```java
public ZonedDateTime withZoneSameLocal(ZoneId zone)
```

转换到另一时区，保持相同的本地时间（日期时间不变，偏移量改变）。

- **参数**: `zone` — 目标时区
- **返回**: `ZonedDateTime` — 新时区的日期时间

### isBefore

```java
public boolean isBefore(ZonedDateTime other)
```

判断该日期时间是否在另一个之前（按时间戳比较）。

- **参数**: `other` — 另一个日期时间
- **返回**: `boolean`

### isAfter

```java
public boolean isAfter(ZonedDateTime other)
```

判断该日期时间是否在另一个之后（按时间戳比较）。

- **参数**: `other` — 另一个日期时间
- **返回**: `boolean`

### compareTo

```java
public int compareTo(ZonedDateTime other)
```

比较两个日期时间的先后顺序（按时间戳比较）。

- **参数**: `other` — 另一个日期时间
- **返回**: `int` — 负数表示早于，正数表示晚于

### plusDays

```java
public ZonedDateTime plusDays(long days)
```

增加天数。

- **参数**: `days` — 要增加的天数
- **返回**: `ZonedDateTime`

### plusHours

```java
public ZonedDateTime plusHours(long hours)
```

增加小时数。

- **参数**: `hours` — 要增加的小时数
- **返回**: `ZonedDateTime`

### minusDays

```java
public ZonedDateTime minusDays(long days)
```

减少天数。

- **参数**: `days` — 要减少的天数
- **返回**: `ZonedDateTime`

### toString

```java
public String toString()
```

返回日期时间的字符串表示，如 "2026-07-23T10:30+08:00[Asia/Shanghai]"。

- **返回**: `String`

### format

```java
public String format(DateTimeFormatter formatter)
```

使用指定格式化器将日期时间格式化为字符串。

- **参数**: `formatter` — 格式化器
- **返回**: `String` — 格式化后的字符串

## 测试

### of

- 描述: 使用 of() 创建 ZonedDateTime
- 断言: 2026-07-23T10:30 Asia/Shanghai 的各字段正确

```java
// 方法体开始
System.out.println("=== of ===");
java.time.ZoneId shanghai = java.time.ZoneId.of("Asia/Shanghai");
java.time.ZonedDateTime zdt = java.time.ZonedDateTime.of(2026, 7, 23, 10, 30, 0, 0, shanghai);
System.out.println("创建 ZonedDateTime: " + zdt);
assertEquals(2026, zdt.getYear());
assertEquals(7, zdt.getMonthValue());
assertEquals(23, zdt.getDayOfMonth());
assertEquals(10, zdt.getHour());
assertEquals(30, zdt.getMinute());
assertEquals(shanghai, zdt.getZone());
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### now

- 描述: 获取当前日期时间
- 断言: now() 返回的 ZonedDateTime 不为 null

```java
// 方法体开始
System.out.println("=== now ===");
java.time.ZonedDateTime now = java.time.ZonedDateTime.now();
System.out.println("当前时间: " + now);
assertNotNull(now);
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### now(ZoneId)

- 描述: 从指定时区获取当前日期时间
- 断言: now(ZoneId) 返回的 ZonedDateTime 不为 null 且时区正确

```java
// 方法体开始
System.out.println("=== now(ZoneId) ===");
java.time.ZoneId shanghai = java.time.ZoneId.of("Asia/Shanghai");
java.time.ZonedDateTime zdt = java.time.ZonedDateTime.now(shanghai);
System.out.println("上海时间: " + zdt);
assertNotNull(zdt);
assertEquals(shanghai, zdt.getZone());
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### now(Clock)

- 描述: 从指定时钟获取当前日期时间
- 断言: 固定时钟返回的日期时间与时钟一致

```java
// 方法体开始
System.out.println("=== now(Clock) ===");
java.time.ZoneId utc = java.time.ZoneId.of("UTC");
java.time.Clock clock = java.time.Clock.fixed(java.time.Instant.parse("2026-07-23T10:30:00Z"), utc);
java.time.ZonedDateTime zdt = java.time.ZonedDateTime.now(clock);
System.out.println("固定时钟的 UTC 时间: " + zdt);
assertEquals(2026, zdt.getYear());
assertEquals(7, zdt.getMonthValue());
assertEquals(23, zdt.getDayOfMonth());
assertEquals(10, zdt.getHour());
assertEquals(30, zdt.getMinute());
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### parse

- 描述: 解析日期时间字符串
- 断言: 解析后的各字段正确

```java
// 方法体开始
System.out.println("=== parse ===");
java.time.ZonedDateTime zdt = java.time.ZonedDateTime.parse("2026-07-23T10:30:00+08:00[Asia/Shanghai]");
System.out.println("解析结果: " + zdt);
assertEquals(2026, zdt.getYear());
assertEquals(7, zdt.getMonthValue());
assertEquals(23, zdt.getDayOfMonth());
assertEquals(10, zdt.getHour());
assertEquals(30, zdt.getMinute());
assertEquals(java.time.ZoneId.of("Asia/Shanghai"), zdt.getZone());
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### getZone

- 描述: 获取时区信息
- 断言: Asia/Shanghai 时区的 ZonedDateTime 返回 ZoneId("Asia/Shanghai")

```java
// 方法体开始
System.out.println("=== getZone ===");
java.time.ZoneId shanghai = java.time.ZoneId.of("Asia/Shanghai");
java.time.ZonedDateTime zdt = java.time.ZonedDateTime.of(2026, 7, 23, 10, 30, 0, 0, shanghai);
java.time.ZoneId zone = zdt.getZone();
System.out.println("时区: " + zone);
assertEquals(shanghai, zone);
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### getOffset

- 描述: 获取与 UTC 的偏移量
- 断言: Asia/Shanghai 的偏移量为 +08:00

```java
// 方法体开始
System.out.println("=== getOffset ===");
java.time.ZoneId shanghai = java.time.ZoneId.of("Asia/Shanghai");
java.time.ZonedDateTime zdt = java.time.ZonedDateTime.of(2026, 7, 23, 10, 30, 0, 0, shanghai);
java.time.ZoneOffset offset = zdt.getOffset();
System.out.println("偏移量: " + offset);
assertEquals(java.time.ZoneOffset.ofHours(8), offset);
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### toLocalDate

- 描述: 获取日期部分
- 断言: 2026-07-23T10:30 Asia/Shanghai 的日期为 2026-07-23

```java
// 方法体开始
System.out.println("=== toLocalDate ===");
java.time.ZonedDateTime zdt = java.time.ZonedDateTime.of(2026, 7, 23, 10, 30, 0, 0, java.time.ZoneId.of("Asia/Shanghai"));
java.time.LocalDate date = zdt.toLocalDate();
System.out.println("日期部分: " + date);
assertEquals(2026, date.getYear());
assertEquals(7, date.getMonthValue());
assertEquals(23, date.getDayOfMonth());
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### toLocalTime

- 描述: 获取时间部分
- 断言: 2026-07-23T10:30 Asia/Shanghai 的时间为 10:30

```java
// 方法体开始
System.out.println("=== toLocalTime ===");
java.time.ZonedDateTime zdt = java.time.ZonedDateTime.of(2026, 7, 23, 10, 30, 0, 0, java.time.ZoneId.of("Asia/Shanghai"));
java.time.LocalTime time = zdt.toLocalTime();
System.out.println("时间部分: " + time);
assertEquals(10, time.getHour());
assertEquals(30, time.getMinute());
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### toInstant

- 描述: 转换为 Instant
- 断言: 2026-07-23T10:30 Asia/Shanghai 对应的 Instant 为 2026-07-23T02:30:00Z

```java
// 方法体开始
System.out.println("=== toInstant ===");
java.time.ZonedDateTime zdt = java.time.ZonedDateTime.of(2026, 7, 23, 10, 30, 0, 0, java.time.ZoneId.of("Asia/Shanghai"));
java.time.Instant instant = zdt.toInstant();
System.out.println("对应的 Instant: " + instant);
assertEquals(java.time.Instant.parse("2026-07-23T02:30:00Z"), instant);
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### withZoneSameInstant

- 描述: 转换到另一时区（保持同一时刻）
- 断言: Asia/Shanghai 的 10:30 转换到 UTC 为 02:30

```java
// 方法体开始
System.out.println("=== withZoneSameInstant ===");
java.time.ZoneId shanghai = java.time.ZoneId.of("Asia/Shanghai");
java.time.ZoneId utc = java.time.ZoneId.of("UTC");
java.time.ZonedDateTime shanghaiTime = java.time.ZonedDateTime.of(2026, 7, 23, 10, 30, 0, 0, shanghai);
java.time.ZonedDateTime utcTime = shanghaiTime.withZoneSameInstant(utc);
System.out.println(shanghaiTime + " 转换为 UTC: " + utcTime);
assertEquals(2, utcTime.getHour());
assertEquals(30, utcTime.getMinute());
assertEquals(23, utcTime.getDayOfMonth());
assertEquals(utc, utcTime.getZone());
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### withZoneSameLocal

- 描述: 转换到另一时区（保持本地时间不变）
- 断言: Asia/Shanghai 的 10:30 转换到 UTC 后本地时间仍为 10:30 但偏移量变为 Z

```java
// 方法体开始
System.out.println("=== withZoneSameLocal ===");
java.time.ZoneId shanghai = java.time.ZoneId.of("Asia/Shanghai");
java.time.ZoneId utc = java.time.ZoneId.of("UTC");
java.time.ZonedDateTime shanghaiTime = java.time.ZonedDateTime.of(2026, 7, 23, 10, 30, 0, 0, shanghai);
java.time.ZonedDateTime sameLocal = shanghaiTime.withZoneSameLocal(utc);
System.out.println(shanghaiTime + " 保持本地时间转 UTC: " + sameLocal);
assertEquals(10, sameLocal.getHour());
assertEquals(30, sameLocal.getMinute());
assertEquals(utc, sameLocal.getZone());
assertEquals(java.time.ZoneOffset.UTC, sameLocal.getOffset());
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### isBefore

- 描述: 判断日期时间是否在另一个之前
- 断言: 2026-07-23T08:00 Asia/Shanghai 在 2026-07-23T10:30 Asia/Shanghai 之前

```java
// 方法体开始
System.out.println("=== isBefore ===");
java.time.ZoneId shanghai = java.time.ZoneId.of("Asia/Shanghai");
java.time.ZonedDateTime early = java.time.ZonedDateTime.of(2026, 7, 23, 8, 0, 0, 0, shanghai);
java.time.ZonedDateTime late = java.time.ZonedDateTime.of(2026, 7, 23, 10, 30, 0, 0, shanghai);
System.out.println(early + " isBefore " + late + " ? " + early.isBefore(late));
assertTrue(early.isBefore(late));
assertFalse(late.isBefore(early));
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### isAfter

- 描述: 判断日期时间是否在另一个之后
- 断言: 2026-07-23T10:30 Asia/Shanghai 在 2026-07-23T08:00 Asia/Shanghai 之后

```java
// 方法体开始
System.out.println("=== isAfter ===");
java.time.ZoneId shanghai = java.time.ZoneId.of("Asia/Shanghai");
java.time.ZonedDateTime early = java.time.ZonedDateTime.of(2026, 7, 23, 8, 0, 0, 0, shanghai);
java.time.ZonedDateTime late = java.time.ZonedDateTime.of(2026, 7, 23, 10, 30, 0, 0, shanghai);
System.out.println(late + " isAfter " + early + " ? " + late.isAfter(early));
assertTrue(late.isAfter(early));
assertFalse(early.isAfter(late));
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### compareTo

- 描述: 比较两个日期时间的先后顺序
- 断言: 相同时返回 0，早于返回负数，晚于返回正数

```java
// 方法体开始
System.out.println("=== compareTo ===");
java.time.ZoneId shanghai = java.time.ZoneId.of("Asia/Shanghai");
java.time.ZonedDateTime t1 = java.time.ZonedDateTime.of(2026, 7, 23, 10, 30, 0, 0, shanghai);
java.time.ZonedDateTime t2 = java.time.ZonedDateTime.of(2026, 7, 23, 10, 30, 0, 0, shanghai);
java.time.ZonedDateTime t3 = java.time.ZonedDateTime.of(2026, 7, 24, 10, 30, 0, 0, shanghai);
System.out.println(t1 + " compareTo " + t2 + " = " + t1.compareTo(t2));
assertEquals(0, t1.compareTo(t2));
assertTrue(t1.compareTo(t3) < 0);
assertTrue(t3.compareTo(t1) > 0);
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### plusDays

- 描述: 增加天数
- 断言: 2026-07-23 加 7 天后为 2026-07-30

```java
// 方法体开始
System.out.println("=== plusDays ===");
java.time.ZonedDateTime zdt = java.time.ZonedDateTime.of(2026, 7, 23, 10, 30, 0, 0, java.time.ZoneId.of("Asia/Shanghai"));
java.time.ZonedDateTime result = zdt.plusDays(7);
System.out.println(zdt.toLocalDate() + " + 7天 = " + result.toLocalDate());
assertEquals(30, result.getDayOfMonth());
assertEquals(7, result.getMonthValue());
assertEquals(10, result.getHour());
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### plusHours

- 描述: 增加小时数
- 断言: 2026-07-23T10:30 Asia/Shanghai 加 5 小时后为 15:30

```java
// 方法体开始
System.out.println("=== plusHours ===");
java.time.ZonedDateTime zdt = java.time.ZonedDateTime.of(2026, 7, 23, 10, 30, 0, 0, java.time.ZoneId.of("Asia/Shanghai"));
java.time.ZonedDateTime result = zdt.plusHours(5);
System.out.println(zdt.getHour() + ":30 + 5小时 = " + result.getHour() + ":30");
assertEquals(15, result.getHour());
assertEquals(30, result.getMinute());
assertEquals(23, result.getDayOfMonth());
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### minusDays

- 描述: 减少天数
- 断言: 2026-07-23 减 3 天后为 2026-07-20

```java
// 方法体开始
System.out.println("=== minusDays ===");
java.time.ZonedDateTime zdt = java.time.ZonedDateTime.of(2026, 7, 23, 10, 30, 0, 0, java.time.ZoneId.of("Asia/Shanghai"));
java.time.ZonedDateTime result = zdt.minusDays(3);
System.out.println(zdt.toLocalDate() + " - 3天 = " + result.toLocalDate());
assertEquals(20, result.getDayOfMonth());
assertEquals(7, result.getMonthValue());
assertEquals(10, result.getHour());
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### toString

- 描述: 测试 toString 输出格式
- 断言: toString 包含日期、时间和时区信息

```java
// 方法体开始
System.out.println("=== toString ===");
java.time.ZonedDateTime zdt = java.time.ZonedDateTime.of(2026, 7, 23, 10, 30, 0, 0, java.time.ZoneId.of("Asia/Shanghai"));
String str = zdt.toString();
System.out.println("toString: " + str);
assertTrue(str.contains("2026-07-23"));
assertTrue(str.contains("10:30"));
assertTrue(str.contains("Asia/Shanghai"));
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### format

- 描述: 使用格式化器格式化日期时间
- 断言: 2026-07-23T10:30 Asia/Shanghai 按 "yyyy-MM-dd HH:mm z" 格式化为 "2026-07-23 10:30 CST"

```java
// 方法体开始
System.out.println("=== format ===");
java.time.ZonedDateTime zdt = java.time.ZonedDateTime.of(2026, 7, 23, 10, 30, 0, 0, java.time.ZoneId.of("Asia/Shanghai"));
java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm z");
String formatted = zdt.format(formatter);
System.out.println("格式化结果: " + formatted);
assertTrue(formatted.startsWith("2026-07-23 10:30"));
System.out.println("=== 测试通过 ===");
// 方法体结束
