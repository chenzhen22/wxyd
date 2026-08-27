---
name: ZoneOffset
package: java.time
order: 81
---

## 介绍

`java.time.ZoneOffset` 是 Java 8 日期时间 API 中表示**时区偏移量**的类，是 `ZoneId` 的子类。它表示与 UTC/格林尼治时间的固定偏移，如 `+08:00`（中国标准时间）、`-05:00`（美国东部标准时间）。

ZoneOffset 的核心特点：
- **不可变且线程安全**
- **固定偏移**：没有夏令时变化，适合已知固定偏移的场景
- **ZoneId 的子类**：任何需要 ZoneId 的地方都可以使用 ZoneOffset
- **支持秒级精度**：偏移量可以精确到秒

ZoneOffset 的范围：从 `-18:00` 到 `+18:00`。

ZoneOffset 的常用常量：
- `UTC` — UTC 偏移量 (`Z`)
- `MIN` / `MAX` — 最小/最大偏移量

## 方法

### of

```java
public static ZoneOffset of(String offsetId)
```

从字符串创建 ZoneOffset，格式为 `"+08:00"`、`"-05:00"`、`"Z"` 等。

- **参数**: `offsetId` — 偏移量 ID
- **返回**: `ZoneOffset` — 对应的 ZoneOffset

### ofHours

```java
public static ZoneOffset ofHours(int hours)
```

从小时数创建 ZoneOffset，范围 -18 到 +18。

- **参数**: `hours` — 偏移小时数
- **返回**: `ZoneOffset` — 对应的 ZoneOffset

### ofHoursMinutes

```java
public static ZoneOffset ofHoursMinutes(int hours, int minutes)
```

从小时和分钟创建 ZoneOffset。

- **参数**: `hours` — 偏移小时数；`minutes` — 偏移分钟数
- **返回**: `ZoneOffset` — 对应的 ZoneOffset

### ofTotalSeconds

```java
public static ZoneOffset ofTotalSeconds(int totalSeconds)
```

从总秒数创建 ZoneOffset。

- **参数**: `totalSeconds` — 总偏移秒数（范围 -64800 到 +64800）
- **返回**: `ZoneOffset` — 对应的 ZoneOffset

### getId

```java
public String getId()
```

重写 ZoneId.getId()，返回偏移量字符串，如 `"+08:00"` 或 `"Z"`。

- **返回**: `String` — 偏移量 ID

### getTotalSeconds

```java
public int getTotalSeconds()
```

返回偏移量的总秒数。正值表示比 UTC 早，负值表示比 UTC 晚。

- **返回**: `int` — 总秒数

## 测试

### of

- 描述: 使用 `of` 方法创建时区偏移量
- 断言: 上海时区为 +08:00

```java
// 方法体开始
System.out.println("=== of ===");
ZoneOffset offset = ZoneOffset.of("+08:00");
assertEquals("+08:00", offset.getId());
assertEquals(8 * 3600, offset.getTotalSeconds());
ZoneOffset utc = ZoneOffset.UTC;
assertEquals("Z", utc.getId());
assertEquals(0, utc.getTotalSeconds());
System.out.println("上海偏移: " + offset);
System.out.println("UTC: " + utc);
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### ofHours

- 描述: 使用 `ofHours` 创建整小时偏移
- 断言: +8 小时 = +08:00

```java
// 方法体开始
System.out.println("=== ofHours ===");
ZoneOffset offset = ZoneOffset.ofHours(8);
assertEquals("+08:00", offset.getId());
assertEquals(28800, offset.getTotalSeconds());  // 8 * 3600
System.out.println("+8 小时: " + offset);
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### OffsetDateTime 使用

- 描述: 使用 ZoneOffset 创建 OffsetDateTime
- 断言: 正确应用偏移量

```java
// 方法体开始
System.out.println("=== OffsetDateTime ===");
ZoneOffset shanghaiOffset = ZoneOffset.ofHours(8);
OffsetDateTime odt = OffsetDateTime.of(
        LocalDateTime.of(2024, 1, 15, 10, 0),
        shanghaiOffset);
assertEquals("+08:00", odt.getOffset().getId());
assertEquals(10, odt.getHour());
System.out.println("上海偏移时间: " + odt);
System.out.println("=== 测试通过 ===");
// 方法体结束
```
