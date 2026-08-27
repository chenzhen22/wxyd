---
name: OffsetDateTime
package: java.time
order: 112
---

## 介绍

`java.time.OffsetDateTime` 是 Java 8 日期时间 API 中表示**带偏移量的日期时间**的不可变类，如 `2024-01-15T10:30:00+08:00`。它包含日期、时间和与 UTC 的偏移量，常用于 REST API 和数据库中的时间戳表示。

OffsetDateTime 的核心特点：
- **不可变且线程安全**
- **日期 + 时间 + 偏移**：完整的带时区偏移的时间表示
- **与 Instant 互转**：`toInstant()` / `from(Instant)` 轻松转换
- **标准化序列化**：ISO-8601 格式 `2024-01-15T10:30:00+08:00`，是 REST API 中最常用的时间格式之一

## 方法

### now

```java
public static OffsetDateTime now()
public static OffsetDateTime now(ZoneId zone)
```

获取当前日期时间或指定时区的当前日期时间。

### of

```java
public static OffsetDateTime of(LocalDateTime dateTime, ZoneOffset offset)
public static OffsetDateTime of(LocalDate date, LocalTime time, ZoneOffset offset)
```

从 LocalDateTime + ZoneOffset 创建 OffsetDateTime。

### toInstant / toZonedDateTime

```java
public Instant toInstant()
public ZonedDateTime toZonedDateTime()
```

转换为 Instant 或 ZonedDateTime。

### getOffset / withOffsetSameInstant

```java
public ZoneOffset getOffset()
public OffsetDateTime withOffsetSameInstant(ZoneOffset offset)
```

获取偏移量 / 在不同时区同一时刻的 OffsetDateTime。

### format

```java
public String format(DateTimeFormatter formatter)
```

使用指定格式器格式化。

## 测试

### of

- 描述: 创建带偏移的日期时间
- 断言: 时分秒和偏移正确

```java
// 方法体开始
System.out.println("=== of ===");
LocalDateTime ldt = LocalDateTime.of(2024, 1, 15, 10, 30);
ZoneOffset offset = ZoneOffset.ofHours(8);
OffsetDateTime odt = OffsetDateTime.of(ldt, offset);
assertEquals("2024-01-15T10:30+08:00", odt.toString());
assertEquals(10, odt.getHour());
assertEquals("+08:00", odt.getOffset().getId());
System.out.println("OffsetDateTime: " + odt);
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### toInstant

- 描述: 转换为 Instant
- 断言: +08:00 的 10:30 等于 UTC 的 02:30

```java
// 方法体开始
System.out.println("=== toInstant ===");
OffsetDateTime odt = OffsetDateTime.of(
        LocalDateTime.of(2024, 1, 15, 10, 30),
        ZoneOffset.ofHours(8));
Instant instant = odt.toInstant();
assertEquals("2024-01-15T02:30:00Z", instant.toString());
System.out.println(odt + " → Instant: " + instant);
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### withOffsetSameInstant

- 描述: 在不同时区查看同一时刻
- 断言: 上海 10:30 = 东京 11:30（同 Instant）

```java
// 方法体开始
System.out.println("=== withOffsetSameInstant ===");
OffsetDateTime shanghai = OffsetDateTime.of(
        LocalDateTime.of(2024, 1, 15, 10, 30),
        ZoneOffset.ofHours(8));
OffsetDateTime tokyo = shanghai.withOffsetSameInstant(ZoneOffset.ofHours(9));
assertEquals(11, tokyo.getHour());
assertEquals("+09:00", tokyo.getOffset().getId());
System.out.println("上海: " + shanghai + " → 东京: " + tokyo);
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### parse

- 描述: 解析 ISO-8601 字符串
- 断言: 正确解析

```java
// 方法体开始
System.out.println("=== parse ===");
OffsetDateTime odt = OffsetDateTime.parse("2024-06-15T14:30:00+08:00");
assertEquals(2024, odt.getYear());
assertEquals(6, odt.getMonthValue());
assertEquals(15, odt.getDayOfMonth());
assertEquals(14, odt.getHour());
assertEquals("+08:00", odt.getOffset().getId());
System.out.println("解析: " + odt);
System.out.println("=== 测试通过 ===");
// 方法体结束
```
