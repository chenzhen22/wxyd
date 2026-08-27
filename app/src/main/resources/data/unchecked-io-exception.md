---
name: UncheckedIOException
package: java.io
order: 133
---

## 介绍

`java.io.UncheckedIOException` 是 Java 8 新增的**未检查 IO 异常**，用于在 Stream API 和 Lambda 表达式中包装 `IOException`。由于 Lambda 表达式不能抛出受检异常，`UncheckedIOException` 提供了一种包装 `IOException` 的方式。

## 构造方法

```java
public UncheckedIOException(String message, IOException cause)
public UncheckedIOException(IOException cause)
```

## 测试

### 创建和获取 cause

- 描述: 创建 UncheckedIOException 并获取原始 IOException
- 断言: cause 与传入的一致

```java
// 方法体开始
System.out.println("=== UncheckedIOException ===");
IOException original = new IOException("文件未找到");
UncheckedIOException uio = new UncheckedIOException("包装", original);
assertEquals("包装", uio.getMessage());
assertSame(original, uio.getCause());
System.out.println("异常信息: " + uio.getMessage());
System.out.println("原始异常: " + uio.getCause().getMessage());
System.out.println("=== 测试通过 ===");
// 方法体结束
```
