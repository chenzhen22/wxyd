---
name: Exception
package: java.lang
order: 351
---

## 介绍

`java.lang.Exception` 是所有**受检异常**的基类，表示程序可以处理的异常情况。

## 方法

构造方法：
```java
public Exception()
public Exception(String message)
public Exception(String message, Throwable cause)
```

## 测试

- 描述: 创建和使用 Exception
- 断言: 异常信息正确

```java
// 方法体开始
System.out.println("=== Exception ===");
Exception ex = new Exception("发生错误");
assertEquals("发生错误", ex.getMessage());
try {
    throw new Exception("测试异常");
} catch (Exception e) {
    assertEquals("测试异常", e.getMessage());
    System.out.println("捕获异常: " + e.getMessage());
}
System.out.println("=== 测试通过 ===");
// 方法体结束
```
