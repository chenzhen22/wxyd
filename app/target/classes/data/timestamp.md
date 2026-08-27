---
name: Timestamp
package: java.sql
order: 329
---

## 介绍

`java.sql.Timestamp` 是 **SQL TIMESTAMP 类型**的 Java 表示，继承自 `java.util.Date`，增加了纳秒精度。

## 方法

构造方法：
```java
public Timestamp(long time)
```

### valueOf / toString / getNanos / setNanos

## 测试

- 描述: 创建 SQL Timestamp
- 断言: 纳秒精度正确

```java
// 方法体开始
System.out.println("=== Timestamp ===");
Timestamp ts = Timestamp.valueOf("2024-01-15 10:30:00.123456789");
assertEquals(2024, ts.getYear() + 1900);
assertEquals(1, ts.getMonth() + 1);
assertEquals(15, ts.getDayOfMonth());
assertEquals(123456789, ts.getNanos());
System.out.println("Timestamp: " + ts);
System.out.println("=== 测试通过 ===");
// 方法体结束
```
