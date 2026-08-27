---
name: Instant
package: java.time
order: 7
---

## 介绍

`java.time.Instant` 是 Java 8 日期时间 API 中表示**时间戳**的不可变类。它精确到纳秒，用于机器时间，表示从 1970-01-01T00:00:00Z 纪元开始的纳秒级时间点。

`Instant` 是不可变且线程安全的，所有修改操作都会返回新实例。

常见用途：
- **时间戳记录**：记录事件发生的精确时刻
- **时间转换**：与 `LocalDateTime`、`Date` 等相互转换
- **时间计算**：加减秒、毫秒、纳秒
- **时间比较**：判断先后顺序、比较大小
- **系统时钟**：获取当前 UTC 时刻

## 方法

### now

```java
public static Instant now()
```

从系统时钟获取当前 UTC 时间戳。

- **返回**: `Instant` — 当前时间戳

### now(Clock)

```java
public static Instant now(Clock clock)
```

从指定时钟获取当前时间戳。

- **参数**: `clock` — 时钟对象
- **返回**: `Instant`

### ofEpochSecond

```java
public static Instant ofEpochSecond(long epochSecond)
```

从纪元秒数创建时间戳。

- **参数**: `epochSecond` — 从 1970-01-01T00:00:00Z 开始的秒数
- **返回**: `Instant`

### ofEpochSecond(long, long)

```java
public static Instant ofEpochSecond(long epochSecond, long nanoAdjustment)
```

从纪元秒数和纳秒调整值创建时间戳。

- **参数**: `epochSecond` — 纪元秒数；`nanoAdjustment` — 纳秒调整值
- **返回**: `Instant`

### ofEpochMilli

```java
public static Instant ofEpochMilli(long epochMilli)
```

从纪元毫秒数创建时间戳。

- **参数**: `epochMilli` — 从 1970-01-01T00:00:00Z 开始的毫秒数
- **返回**: `Instant`

### parse

```java
public static Instant parse(CharSequence text)
```

解析 ISO-8601 格式的时间戳字符串，如 "2026-07-23T10:30:00Z"。

- **参数**: `text` — ISO-8601 格式字符串，如 "2026-07-23T10:30:00Z"
- **返回**: `Instant`
- **抛出**: `DateTimeParseException` — 如果文本格式错误

### getEpochSecond

```java
public long getEpochSecond()
```

获取从纪元开始的秒数。

- **返回**: `long` — 纪元秒数

### getNano

```java
public int getNano()
```

获取纳秒部分（0-999999999）。

- **返回**: `int` — 纳秒数

### toEpochMilli

```java
public long toEpochMilli()
```

将时间戳转换为纪元毫秒数。

- **返回**: `long` — 纪元毫秒数
- **抛出**: `ArithmeticException` — 如果数值溢出 long

### isBefore

```java
public boolean isBefore(Instant otherInstant)
```

判断该时间戳是否在另一个时间戳之前。

- **参数**: `otherInstant` — 另一个时间戳
- **返回**: `boolean`

### isAfter

```java
public boolean isAfter(Instant otherInstant)
```

判断该时间戳是否在另一个时间戳之后。

- **参数**: `otherInstant` — 另一个时间戳
- **返回**: `boolean`

### compareTo

```java
public int compareTo(Instant otherInstant)
```

比较两个时间戳的先后顺序。

- **参数**: `otherInstant` — 另一个时间戳
- **返回**: `int` — 负数表示早于，正数表示晚于，0 表示相等

### plusSeconds

```java
public Instant plusSeconds(long secondsToAdd)
```

增加秒数。

- **参数**: `secondsToAdd` — 要增加的秒数
- **返回**: `Instant`

### plusMillis

```java
public Instant plusMillis(long millisToAdd)
```

增加毫秒数。

- **参数**: `millisToAdd` — 要增加的毫秒数
- **返回**: `Instant`

### plusNanos

```java
public Instant plusNanos(long nanosToAdd)
```

增加纳秒数。

- **参数**: `nanosToAdd` — 要增加的纳秒数
- **返回**: `Instant`

### minusSeconds

```java
public Instant minusSeconds(long secondsToSubtract)
```

减少秒数。

- **参数**: `secondsToSubtract` — 要减少的秒数
- **返回**: `Instant`

### minusMillis

```java
public Instant minusMillis(long millisToSubtract)
```

减少毫秒数。

- **参数**: `millisToSubtract` — 要减少的毫秒数
- **返回**: `Instant`

### minusNanos

```java
public Instant minusNanos(long nanosToSubtract)
```

减少纳秒数。

- **参数**: `nanosToSubtract` — 要减少的纳秒数
- **返回**: `Instant`

### toString

```java
public String toString()
```

返回 ISO-8601 格式的字符串表示，如 "2026-07-23T10:30:00Z"。

- **返回**: `String` — ISO-8601 格式字符串

## 测试

### now

- 描述: 获取当前时间戳
- 断言: now() 返回的 Instant 不为 null

```java
// 方法体开始
System.out.println("=== now ===");
Instant now = Instant.now();
System.out.println("当前时间戳: " + now);
assertNotNull(now);
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### now(Clock)

- 描述: 从指定时钟获取时间戳
- 断言: 固定时钟返回的时间戳与时钟一致

```java
// 方法体开始
System.out.println("=== now(Clock) ===");
java.time.Clock clock = java.time.Clock.fixed(Instant.parse("2026-07-23T10:30:00Z"), java.time.ZoneId.of("UTC"));
Instant instant = Instant.now(clock);
System.out.println("指定时钟时间戳: " + instant);
assertEquals(Instant.parse("2026-07-23T10:30:00Z"), instant);
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### ofEpochSecond

- 描述: 从纪元秒数创建时间戳
- 断言: 0 秒 = 1970-01-01T00:00:00Z

```java
// 方法体开始
System.out.println("=== ofEpochSecond ===");
Instant instant = Instant.ofEpochSecond(0);
System.out.println("纪元零点: " + instant);
assertEquals(0, instant.getEpochSecond());
assertEquals("1970-01-01T00:00:00Z", instant.toString());
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### ofEpochSecondWithNanos

- 描述: 从纪元秒数和纳秒调整值创建时间戳
- 断言: 1000 秒 + 500000000 纳秒 = 1000.5 秒

```java
// 方法体开始
System.out.println("=== ofEpochSecondWithNanos ===");
Instant instant = Instant.ofEpochSecond(1000, 500000000);
System.out.println("1000秒 + 500000000纳秒: " + instant);
assertEquals(1000, instant.getEpochSecond());
assertEquals(500000000, instant.getNano());
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### ofEpochMilli

- 描述: 从纪元毫秒数创建时间戳
- 断言: 1500 毫秒 = 1 秒 + 500000000 纳秒

```java
// 方法体开始
System.out.println("=== ofEpochMilli ===");
Instant instant = Instant.ofEpochMilli(1500);
System.out.println("1500毫秒: " + instant);
assertEquals(1, instant.getEpochSecond());
assertEquals(500000000, instant.getNano());
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### parse

- 描述: 解析 ISO-8601 时间戳字符串
- 断言: "2026-07-23T10:30:00Z" 解析后秒数为 1723271400

```java
// 方法体开始
System.out.println("=== parse ===");
Instant instant = Instant.parse("2026-07-23T10:30:00Z");
System.out.println("解析结果: " + instant);
assertEquals("2026-07-23T10:30:00Z", instant.toString());
assertTrue(instant.getEpochSecond() > 0);
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### getEpochSecond

- 描述: 获取纪元秒数
- 断言: ofEpochSecond 设置的秒数与 getEpochSecond 一致

```java
// 方法体开始
System.out.println("=== getEpochSecond ===");
Instant instant = Instant.ofEpochSecond(1000000L);
long epochSecond = instant.getEpochSecond();
System.out.println("纪元秒数: " + epochSecond);
assertEquals(1000000L, epochSecond);
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### getNano

- 描述: 获取纳秒部分
- 断言: 带纳秒的时间戳获取纳秒正确

```java
// 方法体开始
System.out.println("=== getNano ===");
Instant instant = Instant.ofEpochSecond(1000, 123456789);
System.out.println("纳秒部分: " + instant.getNano());
assertEquals(123456789, instant.getNano());
assertEquals(1000, instant.getEpochSecond());
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### toEpochMilli

- 描述: 转换为纪元毫秒数
- 断言: 1 秒 = 1000 毫秒

```java
// 方法体开始
System.out.println("=== toEpochMilli ===");
Instant instant = Instant.ofEpochSecond(1);
long millis = instant.toEpochMilli();
System.out.println("1秒 = " + millis + " 毫秒");
assertEquals(1000, millis);
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### isBefore

- 描述: 判断时间戳先后
- 断言: 较早的时间戳在较晚之前

```java
// 方法体开始
System.out.println("=== isBefore ===");
Instant early = Instant.ofEpochSecond(1000);
Instant late = Instant.ofEpochSecond(2000);
System.out.println(early + " isBefore " + late + " ? " + early.isBefore(late));
assertTrue(early.isBefore(late));
assertFalse(late.isBefore(early));
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### isAfter

- 描述: 判断时间戳先后
- 断言: 较晚的时间戳在较早之后

```java
// 方法体开始
System.out.println("=== isAfter ===");
Instant early = Instant.ofEpochSecond(1000);
Instant late = Instant.ofEpochSecond(2000);
System.out.println(late + " isAfter " + early + " ? " + late.isAfter(early));
assertTrue(late.isAfter(early));
assertFalse(early.isAfter(late));
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### compareTo

- 描述: 比较两个时间戳
- 断言: 相同时间戳 compareTo 为 0

```java
// 方法体开始
System.out.println("=== compareTo ===");
Instant i1 = Instant.ofEpochSecond(1000);
Instant i2 = Instant.ofEpochSecond(1000);
Instant i3 = Instant.ofEpochSecond(2000);
assertEquals(0, i1.compareTo(i2));
assertTrue(i1.compareTo(i3) < 0);
assertTrue(i3.compareTo(i1) > 0);
assertTrue(i1.equals(i2));
assertFalse(i1.equals(i3));
System.out.println(i1 + " compareTo " + i2 + " = " + i1.compareTo(i2));
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### plusSeconds

- 描述: 增加秒数
- 断言: 纪元零点 + 3600 秒 = 1 小时后

```java
// 方法体开始
System.out.println("=== plusSeconds ===");
Instant instant = Instant.ofEpochSecond(0);
Instant result = instant.plusSeconds(3600);
System.out.println(instant + " + 3600秒 = " + result);
assertEquals(3600, result.getEpochSecond());
assertEquals(0, result.getNano());
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### plusMillis

- 描述: 增加毫秒数
- 断言: 纪元零点 + 2500 毫秒 = 2.5 秒

```java
// 方法体开始
System.out.println("=== plusMillis ===");
Instant instant = Instant.ofEpochSecond(0);
Instant result = instant.plusMillis(2500);
System.out.println(instant + " + 2500毫秒 = " + result);
assertEquals(2, result.getEpochSecond());
assertEquals(500000000, result.getNano());
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### plusNanos

- 描述: 增加纳秒数
- 断言: 纪元零点 + 1000000000 纳秒 = 1 秒

```java
// 方法体开始
System.out.println("=== plusNanos ===");
Instant instant = Instant.ofEpochSecond(0);
Instant result = instant.plusNanos(1000000000);
System.out.println(instant + " + 1000000000纳秒 = " + result);
assertEquals(1, result.getEpochSecond());
assertEquals(0, result.getNano());
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### minusSeconds

- 描述: 减少秒数
- 断言: 纪元零点 - 3600 秒 = -3600 秒

```java
// 方法体开始
System.out.println("=== minusSeconds ===");
Instant instant = Instant.ofEpochSecond(3600);
Instant result = instant.minusSeconds(3600);
System.out.println(instant + " - 3600秒 = " + result);
assertEquals(0, result.getEpochSecond());
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### minusMillis

- 描述: 减少毫秒数
- 断言: 2500 毫秒 - 2500 毫秒 = 0

```java
// 方法体开始
System.out.println("=== minusMillis ===");
Instant instant = Instant.ofEpochMilli(2500);
Instant result = instant.minusMillis(2500);
System.out.println(instant + " - 2500毫秒 = " + result);
assertEquals(0, result.getEpochSecond());
assertEquals(0, result.getNano());
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### minusNanos

- 描述: 减少纳秒数
- 断言: 1 秒 - 500000000 纳秒 = 0.5 秒

```java
// 方法体开始
System.out.println("=== minusNanos ===");
Instant instant = Instant.ofEpochSecond(1);
Instant result = instant.minusNanos(500000000);
System.out.println(instant + " - 500000000纳秒 = " + result);
assertEquals(0, result.getEpochSecond());
assertEquals(500000000, result.getNano());
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### toString

- 描述: 时间戳字符串表示
- 断言: toString 返回 ISO-8601 格式

```java
// 方法体开始
System.out.println("=== toString ===");
Instant instant = Instant.parse("2026-07-23T10:30:00Z");
System.out.println("toString: " + instant.toString());
assertEquals("2026-07-23T10:30:00Z", instant.toString());
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### immutable

- 描述: 不可变性
- 断言: plus/minus 返回新实例，原对象不变

```java
// 方法体开始
System.out.println("=== immutable ===");
Instant instant = Instant.ofEpochSecond(1000);
Instant result = instant.plusSeconds(500);
assertNotSame(instant, result);
assertEquals(1000, instant.getEpochSecond());
assertEquals(1500, result.getEpochSecond());
System.out.println("原时间戳: " + instant + ", 新时间戳: " + result);
System.out.println("=== 测试通过 ===");
// 方法体结束
```
