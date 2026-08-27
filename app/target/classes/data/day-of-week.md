---
name: DayOfWeek
package: java.time
order: 224
---

## 介绍

`java.time.DayOfWeek` 是 Java 8 新增的**星期枚举**，表示周一至周日。它提供了方便的日期计算方法。

DayOfWeek 的常量：`MONDAY` 到 `SUNDAY`。`DayOfWeek.MONDAY.getValue()` 返回 1，`SUNDAY` 返回 7。

## 方法

### getValue

```java
public int getValue()
```

返回星期值（1 = 周一，7 = 周日）。

### plus / minus

```java
public DayOfWeek plus(long days)
public DayOfWeek minus(long days)
```

增减天数，循环到正确星期。

### from

```java
public static DayOfWeek from(TemporalAccessor temporal)
```

从时间对象获取星期。

## 测试

### getValue

- 描述: 获取星期数值
- 断言: 值与 ISO 标准一致

```java
// 方法体开始
System.out.println("=== getValue ===");
assertEquals(1, DayOfWeek.MONDAY.getValue());
assertEquals(7, DayOfWeek.SUNDAY.getValue());
assertEquals(4, DayOfWeek.THURSDAY.getValue());
System.out.println("周一=" + DayOfWeek.MONDAY.getValue() + ", 周日=" + DayOfWeek.SUNDAY.getValue());
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### plus

- 描述: 星期循环增减
- 断言: 循环正确

```java
// 方法体开始
System.out.println("=== plus ===");
assertEquals(DayOfWeek.THURSDAY, DayOfWeek.MONDAY.plus(3));
assertEquals(DayOfWeek.MONDAY, DayOfWeek.SUNDAY.plus(1));  // 周日+1=周一
assertEquals(DayOfWeek.SUNDAY, DayOfWeek.MONDAY.minus(1));  // 周一-1=周日
System.out.println("周一+3=" + DayOfWeek.MONDAY.plus(3));
System.out.println("周日+1=" + DayOfWeek.SUNDAY.plus(1));
System.out.println("=== 测试通过 ===");
// 方法体结束
```
