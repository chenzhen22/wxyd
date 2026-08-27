---
name: ZoneId
package: java.time
order: 62
---

## 介绍

`java.time.ZoneId` 是 Java 8 日期时间 API 中表示**时区标识符**的核心类。它取代了旧的 `java.util.TimeZone`，提供了更加清晰和完整的时区支持。

ZoneId 的核心特点：
- **不可变且线程安全**：所有的 ZoneId 实例都是不可变的
- **两种类型**：`ZoneId` 分为**固定偏移**（`ZoneOffset`）和**区域规则**（`ZoneRegion`）两种
- **丰富的预制 ID**：支持 `"Asia/Shanghai"`、`"America/New_York"` 等 IANA 时区
- **与 ZonedDateTime 配合**：`ZoneId` + `LocalDateTime` = `ZonedDateTime`

ZoneId 的常用获取方式：
- `ZoneId.of("Asia/Shanghai")` — 通过 IANA 时区 ID 创建
- `ZoneId.systemDefault()` — 获取系统默认时区
- `ZoneOffset.of("+08:00")` — 通过偏移量创建（ZoneOffset 是 ZoneId 的子类）
- `ZoneId.getAvailableZoneIds()` — 获取所有可用时区 ID

## 方法

### of

```java
public static ZoneId of(String zoneId)
```

根据 IANA 时区 ID 创建一个 ZoneId。

- **参数**: `zoneId` — 时区 ID，如 `"Asia/Shanghai"`、`"America/New_York"`、`"UTC"` 等
- **返回**: `ZoneId` — 对应的时区对象
- **抛出**: `DateTimeException` — 如果时区 ID 无效

### systemDefault

```java
public static ZoneId systemDefault()
```

返回系统默认时区。

- **返回**: `ZoneId` — 系统默认时区

### getAvailableZoneIds

```java
public static Set<String> getAvailableZoneIds()
```

返回所有可用时区 ID 的集合，包含约 600 个 IANA 时区 ID。

- **返回**: `Set<String>` — 所有可用时区 ID

### getId

```java
public String getId()
```

获取时区的唯一标识符。

- **返回**: `String` — 时区 ID，如 `"Asia/Shanghai"`

### getDisplayName

```java
public String getDisplayName(TextStyle style, Locale locale)
```

返回时区的显示名称。注意：只有在 ZoneId 是 `ZoneRegion` 类型时才有可读的名称。

- **参数**: `style` — 文本样式（FULL、SHORT 等）；`locale` — 语言环境
- **返回**: `String` — 时区的显示名称

### getRules

```java
public ZoneRules getRules()
```

获取此时区的规则，包括夏令时转换规则和偏移量变化历史。

- **返回**: `ZoneRules` — 时区规则

### from

```java
public static ZoneId from(TemporalAccessor temporal)
```

从时间对象中获取 ZoneId。支持 `ZonedDateTime` 等包含时区信息的对象。

- **参数**: `temporal` — 时间对象
- **返回**: `ZoneId` — 提取的时区
- **抛出**: `DateTimeException` — 如果无法获取时区

## 测试

### of

- 描述: 使用 `of` 方法创建指定时区
- 断言: 创建 Asia/Shanghai 时区成功

```java
// 方法体开始
System.out.println("=== of ===");
ZoneId shanghai = ZoneId.of("Asia/Shanghai");
assertEquals("Asia/Shanghai", shanghai.getId());
ZoneId utc = ZoneId.of("UTC");
assertEquals("UTC", utc.getId());
System.out.println("时区: " + shanghai + ", " + utc);
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### systemDefault

- 描述: 获取系统默认时区
- 断言: 默认时区不为 null

```java
// 方法体开始
System.out.println("=== systemDefault ===");
ZoneId defaultZone = ZoneId.systemDefault();
assertNotNull(defaultZone);
System.out.println("系统默认时区: " + defaultZone.getId());
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### ZonedDateTime 配合使用

- 描述: 使用 ZoneId 配合 LocalDateTime 创建 ZonedDateTime
- 断言: 上海时间比 UTC 早 8 小时

```java
// 方法体开始
System.out.println("=== ZonedDateTime 配合 ===");
ZoneId shanghai = ZoneId.of("Asia/Shanghai");
LocalDateTime local = LocalDateTime.of(2024, 1, 15, 10, 0);
ZonedDateTime shanghaiTime = ZonedDateTime.of(local, shanghai);
assertEquals("Asia/Shanghai", shanghaiTime.getZone().getId());
assertEquals(local, shanghaiTime.toLocalDateTime());
System.out.println("上海时间: " + shanghaiTime);
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### getAvailableZoneIds

- 描述: 查看可用时区 ID 集合
- 断言: 包含常见时区名称

```java
// 方法体开始
System.out.println("=== getAvailableZoneIds ===");
Set<String> zoneIds = ZoneId.getAvailableZoneIds();
assertTrue(zoneIds.contains("Asia/Shanghai"));
assertTrue(zoneIds.contains("America/New_York"));
assertTrue(zoneIds.contains("Europe/London"));
System.out.println("总时区数: " + zoneIds.size());
System.out.println("包含 Asia/Shanghai: " + zoneIds.contains("Asia/Shanghai"));
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### 时区转换

- 描述: 使用 ZoneId 在不同时区之间转换时间
- 断言: 纽约时间比上海晚 13 小时（标准时间）

```java
// 方法体开始
System.out.println("=== 时区转换 ===");
ZonedDateTime shanghaiTime = ZonedDateTime.of(
        LocalDateTime.of(2024, 1, 15, 20, 0),
        ZoneId.of("Asia/Shanghai"));
ZonedDateTime newYorkTime = shanghaiTime.withZoneSameInstant(ZoneId.of("America/New_York"));
assertEquals(7, newYorkTime.getHour());  // 20 - 13 = 7
System.out.println("上海: " + shanghaiTime);
System.out.println("纽约: " + newYorkTime);
System.out.println("=== 测试通过 ===");
// 方法体结束
```
