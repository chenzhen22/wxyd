---
name: Clock
package: java.time
order: 66
---

## 介绍

`java.time.Clock` 是 Java 8 日期时间 API 中的**时间源抽象**。它提供了获取当前时刻、日期和时间的能力，是 `Instant`、`LocalDate`、`LocalDateTime`、`ZonedDateTime` 等类获取当前时间的底层来源。

Clock 的核心特点：
- **可替换的时间源**：生产环境用系统时钟，测试环境用固定时钟
- **不可变且线程安全**：所有 Clock 实例都是不可变的
- **时区感知**：不同时区的时钟返回不同的本地时间
- **测试友好**：通过 `fixed`、`offset` 等方法创建测试用时钟

Clock 的常用静态工厂方法：
- `systemDefaultZone()` — 系统默认时区时钟
- `systemUTC()` — UTC 时区时钟
- `fixed(Instant, ZoneId)` — 固定时刻时钟（测试用）
- `offset(Clock, Duration)` — 偏移时钟（测试用）
- `tickMillis(ZoneId)` — 以毫秒为最小单位滴答的时钟

## 方法

### systemDefaultZone

```java
public static Clock systemDefaultZone()
```

返回系统默认时区的时钟。

- **返回**: `Clock` — 系统默认时区时钟

### systemUTC

```java
public static Clock systemUTC()
```

返回 UTC 时区的时钟。

- **返回**: `Clock` — UTC 时钟

### fixed

```java
public static Clock fixed(Instant fixedInstant, ZoneId zone)
```

返回一个始终返回同一时刻的固定时钟。非常适合测试。

- **参数**: `fixedInstant` — 固定的时刻；`zone` — 时区
- **返回**: `Clock` — 固定时钟

### offset

```java
public static Clock offset(Clock baseClock, Duration offsetDuration)
```

返回相对基准时钟偏移指定时长的时钟。

- **参数**: `baseClock` — 基准时钟；`offsetDuration` — 偏移时长
- **返回**: `Clock` — 偏移后的时钟

### instant

```java
public Instant instant()
```

获取当前时刻。

- **返回**: `Instant` — 当前时刻

### millis

```java
public long millis()
```

获取当前毫秒时间戳。

- **返回**: `long` — 毫秒时间戳

### getZone

```java
public abstract ZoneId getZone()
```

获取时钟的时区。

- **返回**: `ZoneId` — 时区

### withZone

```java
public abstract Clock withZone(ZoneId zone)
```

返回一个指定时区的时钟副本。

- **参数**: `zone` — 新时区
- **返回**: `Clock` — 新时区的时钟

## 测试

### systemDefaultZone

- 描述: 获取系统默认时区时钟并获取当前时刻
- 断言: 当前时刻不为 null

```java
// 方法体开始
System.out.println("=== systemDefaultZone ===");
Clock clock = Clock.systemDefaultZone();
Instant now = clock.instant();
assertNotNull(now);
assertNotNull(clock.getZone());
System.out.println("当前时刻: " + now);
System.out.println("时区: " + clock.getZone());
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### fixed

- 描述: 使用固定时钟模拟特定时间
- 断言: 固定时刻始终不变

```java
// 方法体开始
System.out.println("=== fixed ===");
Instant fixedInstant = Instant.parse("2024-01-15T10:00:00Z");
Clock fixedClock = Clock.fixed(fixedInstant, ZoneId.of("UTC"));
assertEquals(fixedInstant, fixedClock.instant());
// 多次读取始终返回相同时间
assertEquals(fixedInstant, fixedClock.instant());
System.out.println("固定时刻: " + fixedClock.instant());
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### offset

- 描述: 使用偏移时钟模拟未来时间
- 断言: 偏移时钟比基准时钟晚 1 小时

```java
// 方法体开始
System.out.println("=== offset ===");
Instant now = Instant.parse("2024-01-15T10:00:00Z");
Clock baseClock = Clock.fixed(now, ZoneId.of("UTC"));
Clock futureClock = Clock.offset(baseClock, Duration.ofHours(1));
assertEquals(now.plusSeconds(3600), futureClock.instant());
System.out.println("当前: " + baseClock.instant());
System.out.println("1小时后: " + futureClock.instant());
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### withZone

- 描述: 使用 `withZone` 获取不同时区的时钟
- 断言: 不同时区时钟的时区不同

```java
// 方法体开始
System.out.println("=== withZone ===");
Clock utcClock = Clock.systemUTC();
Clock shanghaiClock = utcClock.withZone(ZoneId.of("Asia/Shanghai"));
assertEquals("UTC", utcClock.getZone().getId());
assertEquals("Asia/Shanghai", shanghaiClock.getZone().getId());
System.out.println("UTC 时区: " + utcClock.getZone());
System.out.println("上海时区: " + shanghaiClock.getZone());
System.out.println("=== 测试通过 ===");
// 方法体结束
```
