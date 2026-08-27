---
name: LongFunction
package: java.util.function
order: 86
---

## 介绍

`LongFunction<R>` 是 Java 8 引入的一个**函数式接口**，代表一个接受 `long` 参数并返回结果的函数。与 `Function<Long, R>` 不同，它直接操作原始 long 值。

LongFunction 的单个方法：
- `apply(long value)` — 核心方法

## 方法

### apply

```java
R apply(long value)
```

对给定 long 值执行转换并返回结果。

## 测试

### apply 格式化

- 描述: 将 long 值格式化为字符串
- 断言: 1000000L 转为 "1,000,000"

```java
// 方法体开始
System.out.println("=== apply ===");
LongFunction<String> formatWithComma = v -> String.format("%,d", v);
assertEquals("1,000,000", formatWithComma.apply(1000000L));
assertEquals("0", formatWithComma.apply(0L));
System.out.println("1000000: " + formatWithComma.apply(1000000L));
System.out.println("=== 测试通过 ===");
// 方法体结束
```
