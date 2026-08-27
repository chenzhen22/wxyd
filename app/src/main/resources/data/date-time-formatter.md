---
name: DateTimeFormatter
package: java.time.format
order: 8
---

## 介绍

`java.time.format.DateTimeFormatter` 是 Java 8 中用于**格式化和解析日期时间**的类，线程安全。

它提供了大量的预定义格式化常量（如 `ISO_LOCAL_DATE`），也支持通过模式字符串（如 `"yyyy-MM-dd HH:mm"`）自定义格式。

`DateTimeFormatter` 是不可变且线程安全的，可以在整个应用范围内共享使用。

常见用途：
- **格式化日期时间**：将 `LocalDate`、`LocalTime`、`LocalDateTime` 等转换为字符串
- **解析日期时间**：将字符串解析为日期时间对象
- **国际化**：结合 `Locale` 实现本地化格式
- **自定义模式**：通过模式字符串定义任意日期时间格式

## 方法

### ofPattern(String)

```java
public static DateTimeFormatter ofPattern(String pattern)
```

从模式字符串创建格式化器。

- **参数**: `pattern` — 模式字符串，如 "yyyy-MM-dd HH:mm"
- **返回**: `DateTimeFormatter`
- **说明**: 模式字母含义：y=年、M=月、d=日、H=时、m=分、s=秒、S=毫秒/纳秒

### ofPattern(String, Locale)

```java
public static DateTimeFormatter ofPattern(String pattern, Locale locale)
```

从模式字符串和区域设置创建格式化器。

- **参数**: `pattern` — 模式字符串；`locale` — 区域设置
- **返回**: `DateTimeFormatter`

### ISO_LOCAL_DATE

```java
public static final DateTimeFormatter ISO_LOCAL_DATE
```

ISO 日期格式常量，格式为 "yyyy-MM-dd"。

- **说明**: 等价于 `ofPattern("yyyy-MM-dd")`

### ISO_LOCAL_TIME

```java
public static final DateTimeFormatter ISO_LOCAL_TIME
```

ISO 时间格式常量，格式如 "10:30:45" 或 "10:30:45.123456789"。

- **说明**: 纳秒部分非零时会显示

### ISO_LOCAL_DATE_TIME

```java
public static final DateTimeFormatter ISO_LOCAL_DATE_TIME
```

ISO 日期时间格式常量，格式如 "2026-07-22T10:30:45"。

- **说明**: 日期和时间之间用 T 分隔

### ISO_DATE

```java
public static final DateTimeFormatter ISO_DATE
```

ISO 带可选时区的日期格式，格式如 "2026-07-22" 或 "2026-07-22+08:00"。

- **说明**: 解析时接受带时区偏移的日期字符串

### ISO_DATE_TIME

```java
public static final DateTimeFormatter ISO_DATE_TIME
```

ISO 带时区的日期时间格式，格式如 "2026-07-22T10:30:45+08:00"。

- **说明**: 解析时接受带时区偏移的日期时间字符串

### ISO_INSTANT

```java
public static final DateTimeFormatter ISO_INSTANT
```

ISO 瞬时格式，格式如 "2026-07-22T10:30:45Z"。

- **说明**: 专用于 `Instant` 的格式化和解析

### format

```java
public String format(TemporalAccessor temporal)
```

格式化日期时间对象为字符串。

- **参数**: `temporal` — 实现 `TemporalAccessor` 接口的对象，如 `LocalDate`、`LocalDateTime`、`Instant` 等
- **返回**: `String` — 格式化后的字符串
- **抛出**: `DateTimeException` — 如果 temporal 无法被格式化

### parse(CharSequence)

```java
public TemporalAccessor parse(CharSequence text)
```

解析文本，返回 `TemporalAccessor`。

- **参数**: `text` — 待解析的文本
- **返回**: `TemporalAccessor` — 解析后的临时访问器
- **抛出**: `DateTimeParseException` — 如果文本无法解析

### parse(CharSequence, TemporalQuery)

```java
public <T> T parse(CharSequence text, TemporalQuery<T> query)
```

解析文本并直接转换为目标类型。

- **参数**: `text` — 待解析的文本；`query` — 查询转换器，如 `LocalDate::from`、`LocalDateTime::from`
- **返回**: `T` — 解析并转换后的目标对象
- **抛出**: `DateTimeParseException` — 如果文本无法解析

### parseBest

```java
public TemporalAccessor parseBest(CharSequence text, TemporalQuery<?>... queries)
```

解析文本为最佳匹配类型。

- **参数**: `text` — 待解析的文本；`queries` — 按优先级排列的查询转换器列表
- **返回**: `TemporalAccessor` — 解析后的最佳匹配类型
- **抛出**: `DateTimeParseException` — 如果所有类型都匹配失败

### toFormat

```java
public Format toFormat()
```

将 `DateTimeFormatter` 转换为 `java.text.Format`，便于集成旧 API。

- **返回**: `Format` — 等价的 `java.text.Format` 实例

## 测试

### ofPattern

- 描述: 使用 ofPattern 创建格式化器
- 断言: 格式化为 "yyyy-MM-dd" 格式正确

```java
// 方法体开始
System.out.println("=== ofPattern ===");
java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd");
LocalDate date = LocalDate.of(2026, 7, 22);
String result = date.format(formatter);
System.out.println("格式化结果: " + result);
assertEquals("2026-07-22", result);
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### ofPatternWithLocale

- 描述: 使用 ofPattern 并指定 Locale
- 断言: 中文区域月份格式正确

```java
// 方法体开始
System.out.println("=== ofPatternWithLocale ===");
java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("yyyy-MMMM-dd", java.util.Locale.CHINESE);
LocalDate date = LocalDate.of(2026, 7, 22);
String result = date.format(formatter);
System.out.println("本地化格式结果: " + result);
assertTrue(result.contains("2026"));
assertTrue(result.contains("22"));
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### ISO_LOCAL_DATE

- 描述: 使用 ISO_LOCAL_DATE 常量格式化日期
- 断言: 2026-07-22 格式化为 "2026-07-22"

```java
// 方法体开始
System.out.println("=== ISO_LOCAL_DATE ===");
LocalDate date = LocalDate.of(2026, 7, 22);
String result = date.format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE);
System.out.println("ISO_LOCAL_DATE: " + result);
assertEquals("2026-07-22", result);
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### ISO_LOCAL_TIME

- 描述: 使用 ISO_LOCAL_TIME 常量格式化时间
- 断言: 10:30:45 格式化为 "10:30:45"

```java
// 方法体开始
System.out.println("=== ISO_LOCAL_TIME ===");
LocalTime time = LocalTime.of(10, 30, 45);
String result = time.format(java.time.format.DateTimeFormatter.ISO_LOCAL_TIME);
System.out.println("ISO_LOCAL_TIME: " + result);
assertEquals("10:30:45", result);
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### ISO_LOCAL_DATE_TIME

- 描述: 使用 ISO_LOCAL_DATE_TIME 常量格式化日期时间
- 断言: 2026-07-22T10:30:45 格式正确

```java
// 方法体开始
System.out.println("=== ISO_LOCAL_DATE_TIME ===");
LocalDateTime dt = LocalDateTime.of(2026, 7, 22, 10, 30, 45);
String result = dt.format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME);
System.out.println("ISO_LOCAL_DATE_TIME: " + result);
assertEquals("2026-07-22T10:30:45", result);
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### ISO_DATE

- 描述: 使用 ISO_DATE 常量格式化
- 断言: 2026-07-22 格式化为 "2026-07-22"

```java
// 方法体开始
System.out.println("=== ISO_DATE ===");
LocalDate date = LocalDate.of(2026, 7, 22);
String result = date.format(java.time.format.DateTimeFormatter.ISO_DATE);
System.out.println("ISO_DATE: " + result);
assertEquals("2026-07-22", result);
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### ISO_DATE_TIME

- 描述: 使用 ISO_DATE_TIME 常量格式化
- 断言: 带 ZoneId 的日期时间格式化后包含时区信息

```java
// 方法体开始
System.out.println("=== ISO_DATE_TIME ===");
java.time.ZonedDateTime zdt = java.time.ZonedDateTime.of(2026, 7, 22, 10, 30, 45, 0, java.time.ZoneId.of("Asia/Shanghai"));
String result = zdt.format(java.time.format.DateTimeFormatter.ISO_DATE_TIME);
System.out.println("ISO_DATE_TIME: " + result);
assertTrue(result.contains("2026-07-22T10:30:45"));
assertTrue(result.contains("+08:00") || result.contains("Asia/Shanghai"));
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### ISO_INSTANT

- 描述: 使用 ISO_INSTANT 常量格式化 Instant
- 断言: Instant 格式化结果为 UTC 字符串

```java
// 方法体开始
System.out.println("=== ISO_INSTANT ===");
Instant instant = Instant.ofEpochSecond(0);
String result = java.time.format.DateTimeFormatter.ISO_INSTANT.format(instant);
System.out.println("ISO_INSTANT: " + result);
assertTrue(result.endsWith("Z"));
assertEquals("1970-01-01T00:00:00Z", result);
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### format

- 描述: 使用自定义格式化器格式化 LocalDate
- 断言: 2026-07-22 格式化为 "2026年07月22日"

```java
// 方法体开始
System.out.println("=== formatLocalDate ===");
java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("yyyy'年'MM'月'dd'日'");
LocalDate date = LocalDate.of(2026, 7, 22);
String result = formatter.format(date);
System.out.println("格式化结果: " + result);
assertEquals("2026年07月22日", result);
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### formatLocalDateTime

- 描述: 使用自定义格式化器格式化 LocalDateTime
- 断言: 格式化为 "2026/07/22 10:30:45"

```java
// 方法体开始
System.out.println("=== formatLocalDateTime ===");
java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");
LocalDateTime dt = LocalDateTime.of(2026, 7, 22, 10, 30, 45);
String result = formatter.format(dt);
System.out.println("格式化结果: " + result);
assertEquals("2026/07/22 10:30:45", result);
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### formatLocalTime

- 描述: 使用自定义格式化器格式化 LocalTime
- 断言: 14:05:09 格式化为 "14:05:09"

```java
// 方法体开始
System.out.println("=== formatLocalTime ===");
java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss");
LocalTime time = LocalTime.of(14, 5, 9);
String result = formatter.format(time);
System.out.println("格式化结果: " + result);
assertEquals("14:05:09", result);
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### parse

- 描述: 使用 parse 解析字符串为 TemporalAccessor
- 断言: 解析 "2026-07-22" 可转换为 LocalDate

```java
// 方法体开始
System.out.println("=== parseToTemporalAccessor ===");
java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ISO_LOCAL_DATE;
java.time.temporal.TemporalAccessor parsed = formatter.parse("2026-07-22");
System.out.println("解析结果: " + parsed);
LocalDate date = LocalDate.from(parsed);
assertEquals(2026, date.getYear());
assertEquals(7, date.getMonthValue());
assertEquals(22, date.getDayOfMonth());
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### parseQuery

- 描述: 使用 parse 并指定 TemporalQuery 直接转换
- 断言: 解析 "2026-07-22" 直接得到 LocalDate

```java
// 方法体开始
System.out.println("=== parseWithQuery ===");
java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ISO_LOCAL_DATE;
LocalDate date = formatter.parse("2026-07-22", LocalDate::from);
System.out.println("解析结果: " + date);
assertEquals(2026, date.getYear());
assertEquals(7, date.getMonthValue());
assertEquals(22, date.getDayOfMonth());
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### parseWithCustomFormatter

- 描述: 使用自定义格式化器解析字符串
- 断言: "2026/07/22 10:30" 解析为正确的 LocalDateTime

```java
// 方法体开始
System.out.println("=== parseWithCustomFormatter ===");
java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm");
LocalDateTime dt = formatter.parse("2026/07/22 10:30", LocalDateTime::from);
System.out.println("解析结果: " + dt);
assertEquals(2026, dt.getYear());
assertEquals(7, dt.getMonthValue());
assertEquals(22, dt.getDayOfMonth());
assertEquals(10, dt.getHour());
assertEquals(30, dt.getMinute());
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### parseBest

- 描述: 使用 parseBest 解析日期字符串
- 断言: "2026-07-22" 最佳匹配为 LocalDate

```java
// 方法体开始
System.out.println("=== parseBestLocalDate ===");
java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ISO_LOCAL_DATE;
java.time.temporal.TemporalAccessor result = formatter.parseBest("2026-07-22", LocalDate::from, LocalDateTime::from);
System.out.println("解析结果类型: " + result.getClass().getName());
assertTrue(result instanceof LocalDate);
LocalDate date = (LocalDate) result;
assertEquals(2026, date.getYear());
assertEquals(7, date.getMonthValue());
assertEquals(22, date.getDayOfMonth());
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### parseBestLocalDateTime

- 描述: 使用 parseBest 解析日期时间字符串
- 断言: "2026-07-22T10:30" 最佳匹配为 LocalDateTime

```java
// 方法体开始
System.out.println("=== parseBestLocalDateTime ===");
java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME;
java.time.temporal.TemporalAccessor result = formatter.parseBest("2026-07-22T10:30:00", LocalDateTime::from, LocalDate::from);
System.out.println("解析结果类型: " + result.getClass().getName());
assertTrue(result instanceof LocalDateTime);
LocalDateTime dt = (LocalDateTime) result;
assertEquals(2026, dt.getYear());
assertEquals(7, dt.getMonthValue());
assertEquals(22, dt.getDayOfMonth());
assertEquals(10, dt.getHour());
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### toFormat

- 描述: 将 DateTimeFormatter 转换为 java.text.Format
- 断言: 转换后的 Format 可以格式化日期

```java
// 方法体开始
System.out.println("=== toFormat ===");
java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd");
java.text.Format format = formatter.toFormat();
System.out.println("Format 类型: " + format.getClass().getName());
assertNotNull(format);
LocalDate date = LocalDate.of(2026, 7, 22);
String result = format.format(date);
System.out.println("Format 格式化结果: " + result);
assertEquals("2026-07-22", result);
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### formatWithNanos

- 描述: 格式化带纳秒的时间
- 断言: 纳秒部分被格式化

```java
// 方法体开始
System.out.println("=== formatWithNanos ===");
java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss.SSSSSS");
LocalTime time = LocalTime.of(10, 30, 45, 123456789);
String result = formatter.format(time);
System.out.println("带纳秒格式化结果: " + result);
assertEquals("10:30:45.123456", result);
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### parseWithFormattedString

- 描述: 解析复杂格式的日期时间字符串
- 断言: "2026年07月22日 10时30分" 解析正确

```java
// 方法体开始
System.out.println("=== parseWithFormattedString ===");
java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("yyyy'年'MM'月'dd'日' HH'时'mm'分'");
LocalDateTime dt = formatter.parse("2026年07月22日 10时30分", LocalDateTime::from);
System.out.println("解析结果: " + dt);
assertEquals(2026, dt.getYear());
assertEquals(7, dt.getMonthValue());
assertEquals(22, dt.getDayOfMonth());
assertEquals(10, dt.getHour());
assertEquals(30, dt.getMinute());
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### threadSafety

- 描述: 验证 DateTimeFormatter 的线程安全性（重复使用）
- 断言: 同一实例多次格式化返回正确结果

```java
// 方法体开始
System.out.println("=== threadSafety ===");
java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd");
LocalDate date1 = LocalDate.of(2026, 7, 22);
LocalDate date2 = LocalDate.of(2026, 12, 25);
String result1 = formatter.format(date1);
String result2 = formatter.format(date2);
System.out.println("第一次: " + result1);
System.out.println("第二次: " + result2);
assertEquals("2026-07-22", result1);
assertEquals("2026-12-25", result2);
System.out.println("=== 测试通过 ===");
// 方法体结束
```
