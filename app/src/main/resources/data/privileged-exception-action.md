---
name: PrivilegedExceptionAction
package: java.security
order: 303
---

## 介绍

`java.security.PrivilegedExceptionAction<T>` 是**可抛出异常的特权操作接口**，与 `PrivilegedAction` 类似但允许抛出受检异常。

## 方法

### run

```java
public T run() throws Exception
```

## 测试

- 描述: 使用可以抛出异常的特权操作
- 断言: 操作执行成功

```java
// 方法体开始
System.out.println("=== PrivilegedExceptionAction ===");
String tmpdir = AccessController.doPrivileged(
    (PrivilegedExceptionAction<String>) () -> System.getProperty("java.io.tmpdir"));
assertNotNull(tmpdir);
System.out.println("临时目录: " + tmpdir);
System.out.println("=== 测试通过 ===");
// 方法体结束
```
