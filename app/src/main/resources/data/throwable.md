---
name: Throwable
package: java.lang
order: 350
---

## 介绍

`java.lang.Throwable` 是 Java 异常体系的**根类**，所有错误和异常的父类。

## 方法

构造方法：
```java
public Throwable()
public Throwable(String message)
public Throwable(String message, Throwable cause)
```

### getMessage / getCause / printStackTrace / getStackTrace

### addSuppressed / getSuppressed

## 测试

- 描述: Throwable 基本操作
- 断言: 异常链正确

```java
// 方法体开始
System.out.println("=== Throwable ===");
Throwable root = new Throwable("根异常");
Throwable mid = new Throwable("中间异常", root);
Throwable top = new Throwable("顶层异常", mid);
assertEquals("顶层异常", top.getMessage());
assertEquals(mid, top.getCause());
assertEquals(root, top.getCause().getCause());
assertNull(top.getCause().getCause().getCause());
System.out.println("异常链: " + top.getMessage() + " -> " + mid.getMessage() + " -> " + root.getMessage());
System.out.println("=== 测试通过 ===");
// 方法体结束
```
