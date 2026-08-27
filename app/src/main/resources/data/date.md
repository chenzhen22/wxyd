---
name: Date
package: java.util
order: 170
---

## 介绍

`java.util.Date` 是 Java 中最早的日期时间类。Java 8 为其新增了与 `java.time` 包互操作的方法，用于在老代码和新 API 之间架起桥梁。

Java 8 新增的方法：
- `toInstant()` — 转换为 Instant（用于与新 API 交互）
- `from(Instant)` — 从 Instant 创建 Date（静态方法）

## 方法

### toInstant

```java
public Instant toInstant()
```

将 Date 转换为 Instant。由于 Date 和 Instant 都表示时间线上的某个点，此转换是无损的。

- **返回**: `Instant` — 对应的 Instant

### from

```java
public static Date from(Instant instant)
```

从 Instant 创建 Date（Java 8 新增）。

- **参数**: `instant` — Instant 对象
- **返回**: `Date` — 对应的 Date

## 测试

### toInstant

- 描述: 将 Date 转换为 Instant
- 断言: Instant 与 Date 表示同一时刻

```java
// 方法体开始
System.out.println("=== toInstant ===");
Date date = new Date();
Instant instant = date.toInstant();
assertNotNull(instant);
assertEquals(date.getTime(), instant.toEpochMilli());
System.out.println("Date: " + date + " -> Instant: " + instant);
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### from

- 描述: 从 Instant 创建 Date
- 断言: 与原 Instant 一致

```java
// 方法体开始
System.out.println("=== from ===");
Instant instant = Instant.now();
Date date = Date.from(instant);
assertNotNull(date);
assertEquals(instant.toEpochMilli(), date.getTime());
System.out.println("Instant: " + instant + " -> Date: " + date);
System.out.println("=== 测试通过 ===");
// 方法体结束
```
