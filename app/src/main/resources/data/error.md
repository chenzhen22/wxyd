---
name: Error
package: java.lang
order: 353
---

## 介绍

`java.lang.Error` 是所有**错误**的基类，表示程序中通常不应尝试捕获的严重问题。

## 方法

构造方法：
```java
public Error()
public Error(String message)
```

## 测试

- 描述: Error 及其子类
- 断言: 正确

```java
// 方法体开始
System.out.println("=== Error ===");
Error error = new Error("严重错误");
assertEquals("严重错误", error.getMessage());
assertTrue(new StackOverflowError() instanceof Error);
assertTrue(new OutOfMemoryError() instanceof Error);
System.out.println("Error: " + error.getMessage());
System.out.println("StackOverflowError instanceof Error: " + (new StackOverflowError() instanceof Error));
System.out.println("=== 测试通过 ===");
// 方法体结束
```
