---
name: ChronoUnit
package: java.time.temporal
order: 223
---

## 介绍

`java.time.temporal.ChronoUnit` 是 Java 8 新增的**时间单位枚举**，实现了 `TemporalUnit` 接口。它提供了标准的时间单位，如天、时、分、秒等。

ChronoUnit 的常用常量：
- `DAYS`、`WEEKS`、`MONTHS`、`YEARS`、`DECADES`、`CENTURIES`、`MILLENNIA`
- `HOURS`、`MINUTES`、`SECONDS`、`MILLIS`、`MICROS`、`NANOS`
- `ERAS`、`FOREVER`

## 方法

### between

```java
public long between(Temporal temporal1Inclusive, Temporal temporal2Exclusive)
```

计算两个时间对象之间的时间量。

### addTo / isSupported / getDuration

时间单位的操作方法。

## 测试

### between

- 描述: 计算两个日期之间的天数
- 断言: 天数正确

```java
// 方法体开始
System.out.println("=== between ===");
LocalDate start = LocalDate.of(2024, 1, 1);
LocalDate end = LocalDate.of(2024, 12, 31);
long days = ChronoUnit.DAYS.between(start, end);
assertEquals(365, days);
long months = ChronoUnit.MONTHS.between(start, end);
assertEquals(11, months);
System.out.println("天数: " + days + ", 月数: " + months);
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### 时分秒

- 描述: 计算时间差
- 断言: 时分秒正确

```java
// 方法体开始
System.out.println("=== 时分秒 ===");
LocalTime t1 = LocalTime.of(9, 0);
LocalTime t2 = LocalTime.of(17, 30);
long hours = ChronoUnit.HOURS.between(t1, t2);
long minutes = ChronoUnit.MINUTES.between(t1, t2);
assertEquals(8, hours);
assertEquals(510, minutes);
System.out.println("8.5 小时 = " + hours + " 小时 " + minutes + " 分钟");
System.out.println("=== 测试通过 ===");
// 方法体结束
```
