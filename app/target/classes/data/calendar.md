---
name: Calendar
package: java.util
order: 171
---

## 介绍

`java.util.Calendar` 是 Java 中最早的日期计算抽象类。Java 8 新增了 `toInstant()` 方法，用于桥接到新的 `java.time` API。

Java 8 新增的方法：
- `toInstant()` — 转换为 Instant

## 方法

### toInstant

```java
public Instant toInstant()
```

将 Calendar 转换为 Instant。

- **返回**: `Instant` — 对应的 Instant

### getTime

```java
public final Date getTime()
```

获取 Calendar 对应的 Date。

## 测试

### toInstant

- 描述: 将 Calendar 转换为 Instant
- 断言: 表示同一时刻

```java
// 方法体开始
System.out.println("=== toInstant ===");
Calendar cal = Calendar.getInstance();
long timeMs = cal.getTimeInMillis();
Instant instant = cal.toInstant();
assertEquals(timeMs, instant.toEpochMilli());
System.out.println("Calendar -> Instant: " + instant);
System.out.println("=== 测试通过 ===");
// 方法体结束
```
