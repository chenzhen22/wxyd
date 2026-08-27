---
name: DriverManager
package: java.sql
order: 331
---

## 介绍

`java.sql.DriverManager` 是 **JDBC 驱动管理器**类，管理数据库驱动列表和建立数据库连接。

## 方法

### getConnection

```java
public static Connection getConnection(String url, String user, String password) throws SQLException
```

### registerDriver / deregisterDriver / getDrivers

## 测试

- 描述: DriverManager 基本操作
- 断言: 功能正常

```java
// 方法体开始
System.out.println("=== DriverManager ===");
java.util.Enumeration<Driver> drivers = DriverManager.getDrivers();
assertNotNull(drivers);
System.out.println("已注册驱动列表:");
while (drivers.hasMoreElements()) {
    Driver d = drivers.nextElement();
    System.out.println("  - " + d.getClass().getName());
}
if (!DriverManager.getDrivers().hasMoreElements()) {
    System.out.println("  (无 JDBC 驱动)");
}
System.out.println("=== 测试通过 ===");
// 方法体结束
```
