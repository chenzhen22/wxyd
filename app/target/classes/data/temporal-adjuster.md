---
name: TemporalAdjuster
package: java.time.temporal
order: 246
---

## 介绍

`java.time.temporal.TemporalAdjuster` 是 Java 8 新增的**函数式接口**，用于调整时间对象。它是一个 `@FunctionalInterface`，可以用 Lambda 自定义调整策略。

`TemporalAdjusters` 工具类提供了大量预定义实现（如 `next(MONDAY)`、`lastDayOfMonth()` 等）。

## 方法

### adjustInto

```java
public Temporal adjustInto(Temporal temporal)
```

调整指定的时间对象并返回调整后的副本。

## 测试

- 描述: 使用 Lambda 创建自定义 TemporalAdjuster
- 断言: 调整结果正确

```java
// 方法体开始
System.out.println("=== TemporalAdjuster ===");
TemporalAdjuster nextPayday = temporal -> {
    LocalDate date = LocalDate.from(temporal);
    LocalDate nextMonth = date.withDayOfMonth(15);
    if (date.getDayOfMonth() >= 15) {
        nextMonth = date.plusMonths(1).withDayOfMonth(1);
    }
    return nextMonth;
};
LocalDate today = LocalDate.of(2024, 1, 10);
assertEquals(LocalDate.of(2024, 1, 15), today.with(nextPayday));
System.out.println("自定义 adjuster: " + today + " -> " + today.with(nextPayday));
System.out.println("=== 测试通过 ===");
// 方法体结束
```
