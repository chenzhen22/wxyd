---
name: GregorianCalendar
package: java.util
order: 172
---

## 介绍

`java.util.GregorianCalendar` 是 `Calendar` 的具体实现。Java 8 新增了与 `java.time` 互操作的方法。

Java 8 新增的方法：
- `toZonedDateTime()` — 转换为 ZonedDateTime
- `from(ZonedDateTime)` — 从 ZonedDateTime 创建 GregorianCalendar

## 方法

### toZonedDateTime

```java
public ZonedDateTime toZonedDateTime()
```

将 GregorianCalendar 转换为 ZonedDateTime（Java 8 新增）。

- **返回**: `ZonedDateTime` — 对应的 ZonedDateTime

### from

```java
public static GregorianCalendar from(ZonedDateTime zdt)
```

从 ZonedDateTime 创建 GregorianCalendar（Java 8 新增）。

- **参数**: `zdt` — ZonedDateTime
- **返回**: `GregorianCalendar` — 对应的 GregorianCalendar

## 测试

### toZonedDateTime

- 描述: 互转 GregorianCalendar 和 ZonedDateTime
- 断言: 转换无损

```java
// 方法体开始
System.out.println("=== toZonedDateTime ===");
GregorianCalendar cal = new GregorianCalendar(2024, Calendar.JANUARY, 15, 10, 30, 0);
ZonedDateTime zdt = cal.toZonedDateTime();
assertEquals(2024, zdt.getYear());
assertEquals(1, zdt.getMonthValue());
assertEquals(15, zdt.getDayOfMonth());
assertEquals(10, zdt.getHour());
System.out.println("GregorianCalendar: " + cal.getTime() + " -> ZonedDateTime: " + zdt);
System.out.println("=== 测试通过 ===");
// 方法体结束
```
