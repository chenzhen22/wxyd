---
name: OffsetTime
package: java.time
order: 99
---

## 介绍

`java.time.OffsetTime` 是 Java 8 日期时间 API 中表示**带偏移量的时间**的不可变类，如 `10:30:00+08:00`。它包含时间（时:分:秒）和与 UTC 的偏移量，但不包含日期。

OffsetTime 的核心特点：
- **不可变且线程安全**
- **时间 + 偏移**：表示特定时区下的时间
- **与 OffsetDateTime 互补**：OffsetTime = LocalTime + ZoneOffset

## 方法

### now

```java
public static OffsetTime now()
public static OffsetTime now(ZoneId zone)
```

获取当前时间或指定时区的当前时间。

### of

```java
public static OffsetTime of(LocalTime time, ZoneOffset offset)
public static OffsetTime of(int hour, int minute, int second, int nanoOfSecond, ZoneOffset offset)
```

创建 OffsetTime。

### toLocalTime / getOffset

```java
public LocalTime toLocalTime()
public ZoneOffset getOffset()
```

### withOffsetSameInstant

```java
public OffsetTime withOffsetSameInstant(ZoneOffset offset)
```

在不同时区同一时刻的时间。

## 测试

### of

- 描述: 创建带偏移的时间
- 断言: 10:30:00+08:00

```java
// 方法体开始
System.out.println("=== of ===");
OffsetTime ot = OffsetTime.of(10, 30, 0, 0, ZoneOffset.ofHours(8));
assertEquals(10, ot.getHour());
assertEquals(30, ot.getMinute());
assertEquals("+08:00", ot.getOffset().getId());
System.out.println("OffsetTime: " + ot);
System.out.println("=== 测试通过 ===");
// 方法体结束
```
