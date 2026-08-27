---
name: TimeZone
package: java.util
order: 173
---

## 介绍

`java.util.TimeZone` 是旧的时区类。Java 8 新增了 `toZoneId()` 方法，用于桥接到新的 `java.time.ZoneId` API。

Java 8 新增的方法：
- `toZoneId()` — 转换为 ZoneId

## 方法

### toZoneId

```java
public ZoneId toZoneId()
```

将 TimeZone 转换为 ZoneId（Java 8 新增）。

- **返回**: `ZoneId` — 对应的 ZoneId

### getTimeZone

```java
public static TimeZone getTimeZone(String ID)
```

获取指定 ID 的 TimeZone。

## 测试

### toZoneId

- 描述: 将 TimeZone 转换为 ZoneId
- 断言: 时区 ID 一致

```java
// 方法体开始
System.out.println("=== toZoneId ===");
TimeZone tz = TimeZone.getTimeZone("Asia/Shanghai");
ZoneId zoneId = tz.toZoneId();
assertEquals("Asia/Shanghai", zoneId.getId());
System.out.println("TimeZone: " + tz.getID() + " -> ZoneId: " + zoneId);
System.out.println("=== 测试通过 ===");
// 方法体结束
```
