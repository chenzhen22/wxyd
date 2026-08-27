---
name: DoubleSupplier
package: java.util.function
order: 89
---

## 介绍

`DoubleSupplier` 是 Java 8 引入的一个**函数式接口**，代表一个不接受参数但返回 `double` 值的供应者。它是 `Supplier<Double>` 的原始 double 特化版本。

DoubleSupplier 的单个方法：
- `getAsDouble()` — 核心方法

## 方法

### getAsDouble

```java
double getAsDouble()
```

供应一个 double 值。

## 测试

### getAsDouble

- 描述: 返回圆周率值
- 断言: 返回 Math.PI

```java
// 方法体开始
System.out.println("=== getAsDouble ===");
DoubleSupplier pi = () -> Math.PI;
assertEquals(Math.PI, pi.getAsDouble(), 0.0001);
System.out.println("PI: " + pi.getAsDouble());
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### DoubleStream.generate

- 描述: 使用 DoubleSupplier 生成随机数流
- 断言: 生成 3 个随机数

```java
// 方法体开始
System.out.println("=== DoubleStream.generate ===");
double[] randomNums = DoubleStream.generate(Math::random)
        .limit(3)
        .toArray();
assertEquals(3, randomNums.length);
for (double v : randomNums) {
    assertTrue(v >= 0.0 && v < 1.0);
}
System.out.println("3 个随机数: " + java.util.Arrays.toString(randomNums));
System.out.println("=== 测试通过 ===");
// 方法体结束
```
