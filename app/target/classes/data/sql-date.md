---
name: SQLDate
package: java.sql
order: 332
---

## 介绍

`java.sql.Date` 是 **SQL DATE 类型**的 Java 表示，继承自 `java.util.Date`，只保留日期部分（年、月、日）。

## 方法

构造方法：
```java
public Date(long date)
```

### valueOf / toString

## 测试

- 描述: 创建 SQL Date
- 断言: 只包含日期部分

```java
// 方法体开始
System.out.println("=== SQL Date ===");
java.sql.Date date = java.sql.Date.valueOf("2024-01-15");
assertEquals("2024-01-15", date.toString());
System.out.println("SQL Date: " + date);
System.out.println("=== 测试通过 ===");
// 方法体结束
```
