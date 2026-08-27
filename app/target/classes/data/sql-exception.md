---
name: SQLException
package: java.sql
order: 330
---

## 介绍

`java.sql.SQLException` 是**数据库访问异常**类，提供了数据库相关错误信息。

## 方法

构造方法：
```java
public SQLException(String reason, String SQLState, int vendorCode)
```

### getSQLState / getErrorCode / getNextException

## 测试

- 描述: 创建 SQLException
- 断言: 异常信息正确

```java
// 方法体开始
System.out.println("=== SQLException ===");
SQLException ex = new SQLException("连接失败", "08001", 100);
assertEquals("连接失败", ex.getMessage());
assertEquals("08001", ex.getSQLState());
assertEquals(100, ex.getErrorCode());
assertNull(ex.getNextException());
System.out.println("SQLException: " + ex.getSQLState());
System.out.println("=== 测试通过 ===");
// 方法体结束
```
