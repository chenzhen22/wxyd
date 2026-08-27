---
name: LongToDoubleFunction
package: java.util.function
order: 110
---

## 介绍

`LongToDoubleFunction` 是 Java 8 引入的一个**函数式接口**，代表一个接受 `long` 参数并返回 `double` 值的函数。

## 方法

### applyAsDouble

```java
double applyAsDouble(long value)
```

对给定 long 值执行转换并返回 double。

## 测试

- 描述: long 到 double 的转换
- 断言: 大数正确转换为 double

```java
// 方法体开始
System.out.println("=== LongToDoubleFunction ===");
LongToDoubleFunction toMB = bytes -> bytes / (1024.0 * 1024.0);
assertEquals(1.0, toMB.applyAsDouble(1048576L), 0.001);
System.out.println("1048576 bytes = " + toMB.applyAsDouble(1048576L) + " MB");
System.out.println("=== 测试通过 ===");
// 方法体结束
```
