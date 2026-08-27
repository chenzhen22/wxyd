---
name: DoubleToLongFunction
package: java.util.function
order: 112
---

## 介绍

`DoubleToLongFunction` 是 Java 8 引入的一个**函数式接口**，代表一个接受 `double` 参数并返回 `long` 值的函数。

## 方法

### applyAsLong

```java
long applyAsLong(double value)
```

对给定 double 值执行转换并返回 long。

## 测试

- 描述: double 到 long 的转换
- 断言: 正确转换

```java
// 方法体开始
System.out.println("=== DoubleToLongFunction ===");
DoubleToLongFunction round = v -> Math.round(v);
assertEquals(4L, round.applyAsLong(3.6));
assertEquals(-4L, round.applyAsLong(-3.6));
System.out.println("round(3.6) = " + round.applyAsLong(3.6));
System.out.println("round(-3.6) = " + round.applyAsLong(-3.6));
System.out.println("=== 测试通过 ===");
// 方法体结束
```
