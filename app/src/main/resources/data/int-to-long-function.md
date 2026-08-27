---
name: IntToLongFunction
package: java.util.function
order: 108
---

## 介绍

`IntToLongFunction` 是 Java 8 引入的一个**函数式接口**，代表一个接受 `int` 参数并返回 `long` 值的函数。用于 int → long 的转换。

## 方法

### applyAsLong

```java
long applyAsLong(int value)
```

对给定 int 值执行转换并返回 long。

## 测试

- 描述: int 到 long 的转换
- 断言: 正确转换

```java
// 方法体开始
System.out.println("=== IntToLongFunction ===");
IntToLongFunction factorialCalc = n -> {
    long result = 1;
    for (int i = 2; i <= n; i++) result *= i;
    return result;
};
assertEquals(120L, factorialCalc.applyAsLong(5));
System.out.println("5! = " + factorialCalc.applyAsLong(5));
System.out.println("=== 测试通过 ===");
// 方法体结束
```
