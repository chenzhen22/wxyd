---
name: ToLongBiFunction
package: java.util.function
order: 95
---

## 介绍

`ToLongBiFunction<T, U>` 是 Java 8 引入的一个**函数式接口**，代表一个接受两个对象参数并返回 `long` 值的函数。

## 方法

### applyAsLong

```java
long applyAsLong(T t, U u)
```

对给定两个参数执行计算并返回 long。

## 测试

### 计算乘积

- 描述: 两个整数相乘并返回 long
- 断言: 100000 * 200000 = 20000000000

```java
// 方法体开始
System.out.println("=== 乘积 ===");
ToLongBiFunction<Integer, Integer> product = (a, b) -> (long) a * b;
assertEquals(20000000000L, product.applyAsLong(100000, 200000));
System.out.println("100000 * 200000 = " + product.applyAsLong(100000, 200000));
System.out.println("=== 测试通过 ===");
// 方法体结束
```
