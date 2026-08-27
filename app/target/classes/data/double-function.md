---
name: DoubleFunction
package: java.util.function
order: 96
---

## 介绍

`DoubleFunction<R>` 是 Java 8 引入的一个**函数式接口**，代表一个接受 `double` 参数并返回结果的函数。它是 `Function<Double, R>` 的原始 double 特化版本。

## 方法

### apply

```java
R apply(double value)
```

对给定 double 值执行转换并返回结果。

## 测试

### 格式化金额

- 描述: 将 double 格式化为货币字符串
- 断言: 99.99 → "$99.99"

```java
// 方法体开始
System.out.println("=== 格式化金额 ===");
DoubleFunction<String> currency = v -> String.format("$%.2f", v);
assertEquals("$99.99", currency.apply(99.99));
assertEquals("$0.00", currency.apply(0.0));
System.out.println("99.99 → " + currency.apply(99.99));
System.out.println("=== 测试通过 ===");
// 方法体结束
```
