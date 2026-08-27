---
name: Duration
package: java.time
order: 10
---

## 介绍

`java.time.Duration` 是 Java 8 日期时间 API 中表示**时间量**的不可变类。它基于秒和纳秒，用于表示时分秒纳秒级的时间长度，如 "2 小时 30 分钟"。

`Duration` 是不可变且线程安全的，所有修改操作都会返回新实例。

常见用途：
- **时间差计算**：计算两个时间戳之间的差值
- **时间量创建**：通过天数、小时、分钟、秒、纳秒创建时间量
- **时间量转换**：将时间量转换为不同单位（天、小时、分钟、秒等）
- **时间量运算**：加减时间量、乘以系数、取绝对值
- **时间量比较**：判断正负、是否为零、比较大小
- **字符串解析**：解析和生成 ISO-8601 格式的时间量字符串（如 "PT2H30M"）

## 方法

### ofDays

```java
public static Duration ofDays(long days)
```

创建指定天数的 Duration（1 天 = 24 小时）。

- **参数**: `days` — 天数
- **返回**: `Duration`

### ofHours

```java
public static Duration ofHours(long hours)
```

创建指定小时数的 Duration。

- **参数**: `hours` — 小时数
- **返回**: `Duration`

### ofMinutes

```java
public static Duration ofMinutes(long minutes)
```

创建指定分钟数的 Duration。

- **参数**: `minutes` — 分钟数
- **返回**: `Duration`

### ofSeconds

```java
public static Duration ofSeconds(long seconds)
```

创建指定秒数的 Duration。

- **参数**: `seconds` — 秒数
- **返回**: `Duration`

### ofNanos

```java
public static Duration ofNanos(long nanos)
```

创建指定纳秒数的 Duration。纳秒数超过 999999999 时会自动进位到秒。

- **参数**: `nanos` — 纳秒数
- **返回**: `Duration`

### between

```java
public static Duration between(Temporal startInclusive, Temporal endExclusive)
```

计算两个时间对象之间的 Duration。

- **参数**: `startInclusive` — 起始时间（包含）；`endExclusive` — 结束时间（不包含）
- **返回**: `Duration`
- **抛出**: `DateTimeException` — 如果无法计算 Duration

### toDays

```java
public long toDays()
```

将时间量转换为总天数（截断）。

- **返回**: `long` — 总天数

### toHours

```java
public long toHours()
```

将时间量转换为总小时数（截断）。

- **返回**: `long` — 总小时数

### toMinutes

```java
public long toMinutes()
```

将时间量转换为总分钟数（截断）。

- **返回**: `long` — 总分钟数

### toSeconds

```java
public long toSeconds()
```

将时间量转换为总秒数（截断）。

- **返回**: `long` — 总秒数

### plus

```java
public Duration plus(Duration duration)
```

加上另一个 Duration。

- **参数**: `duration` — 要加的 Duration
- **返回**: `Duration`

### minus

```java
public Duration minus(Duration duration)
```

减去另一个 Duration。

- **参数**: `duration` — 要减的 Duration
- **返回**: `Duration`

### multipliedBy

```java
public Duration multipliedBy(long multiplicand)
```

乘以一个标量值。

- **参数**: `multiplicand` — 乘数
- **返回**: `Duration`

### abs

```java
public Duration abs()
```

返回时间量的绝对值（如果为负则取反，否则返回自身）。

- **返回**: `Duration` — 非负的时间量
- **抛出**: `ArithmeticException` — 如果数值溢出

### isNegative

```java
public boolean isNegative()
```

判断时间量是否为负（小于零）。

- **返回**: `boolean`

### isZero

```java
public boolean isZero()
```

判断时间量是否为零。

- **返回**: `boolean`

### parse

```java
public static Duration parse(CharSequence text)
```

解析 ISO-8601 格式的 Duration 字符串，如 "PT2H30M"。

- **参数**: `text` — ISO-8601 格式字符串，如 "PT1H"、"PT30M"、"PT2H30M"
- **返回**: `Duration`
- **抛出**: `DateTimeParseException` — 如果文本格式错误

### toString

```java
public String toString()
```

返回 ISO-8601 格式的字符串表示，如 "PT2H30M"。

- **返回**: `String` — ISO-8601 格式字符串

## 测试

### ofDays

- 描述: 使用 ofDays 创建天数的 Duration
- 断言: 2 天 = 48 小时

```java
// 方法体开始
System.out.println("=== ofDays ===");
Duration duration = Duration.ofDays(2);
System.out.println("2天: " + duration);
assertEquals(2, duration.toDays());
assertEquals(48, duration.toHours());
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### ofHours

- 描述: 使用 ofHours 创建小时数的 Duration
- 断言: 3 小时 = 180 分钟

```java
// 方法体开始
System.out.println("=== ofHours ===");
Duration duration = Duration.ofHours(3);
System.out.println("3小时: " + duration);
assertEquals(3, duration.toHours());
assertEquals(180, duration.toMinutes());
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### ofMinutes

- 描述: 使用 ofMinutes 创建分钟数的 Duration
- 断言: 30 分钟 = 1800 秒

```java
// 方法体开始
System.out.println("=== ofMinutes ===");
Duration duration = Duration.ofMinutes(30);
System.out.println("30分钟: " + duration);
assertEquals(30, duration.toMinutes());
assertEquals(1800, duration.toSeconds());
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### ofSeconds

- 描述: 使用 ofSeconds 创建秒数的 Duration
- 断言: 65 秒 = 1 分 5 秒

```java
// 方法体开始
System.out.println("=== ofSeconds ===");
Duration duration = Duration.ofSeconds(65);
System.out.println("65秒: " + duration);
assertEquals(65, duration.getSeconds());
assertEquals(1, duration.toMinutes());
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### ofNanos

- 描述: 使用 ofNanos 创建纳秒数的 Duration
- 断言: 2000000000 纳秒 = 2 秒

```java
// 方法体开始
System.out.println("=== ofNanos ===");
Duration duration = Duration.ofNanos(2000000000L);
System.out.println("2000000000纳秒: " + duration);
assertEquals(2, duration.getSeconds());
assertEquals(0, duration.getNano());
assertEquals(2000000000L, duration.toNanos());
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### between

- 描述: 使用 between 计算两个 Instant 之间的 Duration
- 断言: 1970-01-01T00:00:00Z 到 1970-01-01T01:01:01Z 间隔 3661 秒

```java
// 方法体开始
System.out.println("=== between ===");
java.time.Instant start = java.time.Instant.ofEpochSecond(0);
java.time.Instant end = java.time.Instant.ofEpochSecond(3661);
Duration duration = Duration.between(start, end);
System.out.println(start + " 到 " + end + " = " + duration);
assertEquals(3661, duration.getSeconds());
assertEquals(1, duration.toHours());
assertEquals(61, duration.toMinutes());
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### toDays

- 描述: 将 Duration 转换为天数
- 断言: PT30H = 1 天

```java
// 方法体开始
System.out.println("=== toDays ===");
Duration duration = Duration.ofHours(30);
long days = duration.toDays();
System.out.println("30小时 = " + days + " 天");
assertEquals(1, days);
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### toHours

- 描述: 将 Duration 转换为总小时数
- 断言: PT150M = 2 小时

```java
// 方法体开始
System.out.println("=== toHours ===");
Duration duration = Duration.ofMinutes(150);
long hours = duration.toHours();
System.out.println("150分钟 = " + hours + " 小时");
assertEquals(2, hours);
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### toMinutes

- 描述: 将 Duration 转换为总分钟数
- 断言: PT2H30M = 150 分钟

```java
// 方法体开始
System.out.println("=== toMinutes ===");
Duration duration = Duration.ofHours(2).plusMinutes(30);
long minutes = duration.toMinutes();
System.out.println("2小时30分 = " + minutes + " 分钟");
assertEquals(150, minutes);
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### toSeconds

- 描述: 将 Duration 转换为总秒数
- 断言: PT5M = 300 秒

```java
// 方法体开始
System.out.println("=== toSeconds ===");
Duration duration = Duration.ofMinutes(5);
long seconds = duration.toSeconds();
System.out.println("5分钟 = " + seconds + " 秒");
assertEquals(300, seconds);
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### plus

- 描述: Duration 相加
- 断言: PT1H + PT30M = PT1H30M

```java
// 方法体开始
System.out.println("=== plus ===");
Duration d1 = Duration.ofHours(1);
Duration d2 = Duration.ofMinutes(30);
Duration result = d1.plus(d2);
System.out.println(d1 + " + " + d2 + " = " + result);
assertEquals(90, result.toMinutes());
assertEquals(5400, result.toSeconds());
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### minus

- 描述: Duration 相减
- 断言: PT2H - PT30M = PT1H30M

```java
// 方法体开始
System.out.println("=== minus ===");
Duration d1 = Duration.ofHours(2);
Duration d2 = Duration.ofMinutes(30);
Duration result = d1.minus(d2);
System.out.println(d1 + " - " + d2 + " = " + result);
assertEquals(90, result.toMinutes());
assertEquals(1, result.toHours());
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### multipliedBy

- 描述: Duration 乘以标量
- 断言: PT2H * 3 = PT6H

```java
// 方法体开始
System.out.println("=== multipliedBy ===");
Duration duration = Duration.ofHours(2);
Duration result = duration.multipliedBy(3);
System.out.println(duration + " * 3 = " + result);
assertEquals(6, result.toHours());
assertEquals(360, result.toMinutes());
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### abs

- 描述: Duration 取绝对值
- 断言: PT-3H.abs() = PT3H

```java
// 方法体开始
System.out.println("=== abs ===");
Duration negative = Duration.ofHours(-3);
System.out.println("负值: " + negative + ", isNegative: " + negative.isNegative());
Duration absolute = negative.abs();
System.out.println("绝对值: " + absolute);
assertTrue(negative.isNegative());
assertEquals(3, absolute.toHours());
assertFalse(absolute.isNegative());
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### isNegative

- 描述: 判断 Duration 是否为负
- 断言: 正 Duration 不是负的，负 Duration 是负的，ZERO 不是负的

```java
// 方法体开始
System.out.println("=== isNegative ===");
Duration positive = Duration.ofHours(1);
Duration negative = Duration.ofHours(-1);
Duration zero = Duration.ZERO;
System.out.println(positive + " isNegative: " + positive.isNegative());
System.out.println(negative + " isNegative: " + negative.isNegative());
System.out.println(zero + " isNegative: " + zero.isNegative());
assertFalse(positive.isNegative());
assertTrue(negative.isNegative());
assertFalse(zero.isNegative());
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### isZero

- 描述: 判断 Duration 是否为零
- 断言: Duration.ZERO 是零，PT1H 不是零

```java
// 方法体开始
System.out.println("=== isZero ===");
Duration zero = Duration.ZERO;
Duration nonZero = Duration.ofHours(1);
System.out.println(zero + " isZero: " + zero.isZero());
System.out.println(nonZero + " isZero: " + nonZero.isZero());
assertTrue(zero.isZero());
assertFalse(nonZero.isZero());
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### parse

- 描述: 解析 ISO-8601 Duration 字符串
- 断言: "PT1H30M" 解析后为 90 分钟

```java
// 方法体开始
System.out.println("=== parse ===");
Duration duration = Duration.parse("PT1H30M");
System.out.println("解析 PT1H30M: " + duration);
assertEquals(90, duration.toMinutes());
assertEquals(5400, duration.toSeconds());
assertEquals("PT1H30M", duration.toString());
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### toString

- 描述: Duration 的字符串表示
- 断言: PT2H 的 toString 为 "PT2H"

```java
// 方法体开始
System.out.println("=== toString ===");
Duration d1 = Duration.ofHours(2);
Duration d2 = Duration.ofMinutes(5);
Duration d3 = Duration.ofSeconds(45);
System.out.println("toString: " + d1 + ", " + d2 + ", " + d3);
assertEquals("PT2H", d1.toString());
assertEquals("PT5M", d2.toString());
assertEquals("PT45S", d3.toString());
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### immutable

- 描述: Duration 的不可变性
- 断言: plus/minus/multipliedBy 返回新实例，原对象不变

```java
// 方法体开始
System.out.println("=== immutable ===");
Duration original = Duration.ofHours(2);
Duration result = original.plus(Duration.ofHours(3));
System.out.println("原对象: " + original + ", 新对象: " + result);
assertNotSame(original, result);
assertEquals(2, original.toHours());
assertEquals(5, result.toHours());
System.out.println("=== 测试通过 ===");
// 方法体结束
```
