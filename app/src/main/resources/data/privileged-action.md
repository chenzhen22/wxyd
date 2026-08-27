---
name: PrivilegedAction
package: java.security
order: 302
---

## 介绍

`java.security.PrivilegedAction<T>` 是**特权操作接口**，在 `AccessController.doPrivileged` 中使用。Java 8 中它是一个 `@FunctionalInterface`，可以用 Lambda 实现。

## 方法

### run

```java
public T run()
```

## 测试

- 描述: 使用 Lambda 创建 PrivilegedAction
- 断言: 操作执行成功

```java
// 方法体开始
System.out.println("=== PrivilegedAction ===");
PrivilegedAction<String> action = () -> System.getProperty("user.name");
String userName = AccessController.doPrivileged(action);
assertNotNull(userName);
System.out.println("用户名: " + userName);
System.out.println("=== 测试通过 ===");
// 方法体结束
```
