---
name: SQLTime
package: java.sql
order: 328
---

## 介绍

`java.sql.Time` 是 **SQL TIME 类型**的 Java 表示，继承自 `java.util.Date`。

## 方法

构造方法：
```java
public Time(long time)
```

### valueOf / toString

```java
public static Time valueOf(String s)
```

## 测试

- 描述: 创建 SQL Time
- 断言: 创建成功

```java
// 方法体开始
System.out.println("=== SQL Time ===");
Time time = Time.valueOf("14:30:00");
assertEquals(14, time.getHours());
assertEquals(30, time.getMinutes());
assertEquals("14:30:00", time.toString());
System.out.println("Time: " + time);
System.out.println("=== 测试通过 ===");
// 方法体结束
```
