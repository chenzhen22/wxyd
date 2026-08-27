---
name: AccessController
package: java.security
order: 301
---

## 介绍

`java.security.AccessController` 是**访问控制器**类，用于执行访问控制决策和特权操作。

## 方法

### doPrivileged

```java
public static <T> T doPrivileged(PrivilegedAction<T> action)
```

以特权模式执行操作，忽略当前调用栈的权限限制。

### checkPermission

```java
public static void checkPermission(Permission perm) throws AccessControlException
```

检查是否有指定权限。

### getContext

获取当前 AccessControlContext。

## 测试

- 描述: 使用 doPrivileged 执行特权操作
- 断言: 操作执行成功

```java
// 方法体开始
System.out.println("=== AccessController ===");
String result = AccessController.doPrivileged(
    (PrivilegedAction<String>) () -> System.getProperty("java.version"));
assertNotNull(result);
System.out.println("Java 版本: " + result);
System.out.println("=== 测试通过 ===");
// 方法体结束
```
