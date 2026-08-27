---
name: RuntimeException
package: java.lang
order: 352
---

## 介绍

`java.lang.RuntimeException` 是所有**运行时异常**的基类，是不需要捕获的异常。

## 方法

构造方法：
```java
public RuntimeException()
public RuntimeException(String message)
public RuntimeException(String message, Throwable cause)
```

## 测试

- 描述: 运行时异常
- 断言: 异常信息正确

```java
// 方法体开始
System.out.println("=== RuntimeException ===");
try {
    throw new RuntimeException("运行时错误");
} catch (RuntimeException e) {
    assertEquals("运行时错误", e.getMessage());
    System.out.println("RuntimeException: " + e.getMessage());
}
// 常见子类
assertTrue(new IllegalArgumentException("参数错误") instanceof RuntimeException);
assertTrue(new NullPointerException("空指针") instanceof RuntimeException);
System.out.println("=== 测试通过 ===");
// 方法体结束
```
