---
name: DoubleToIntFunction
package: java.util.function
order: 111
---

## 介绍

`DoubleToIntFunction` 是 Java 8 引入的一个**函数式接口**，代表一个接受 `double` 参数并返回 `int` 值的函数。

## 方法

### applyAsInt

```java
int applyAsInt(double value)
```

对给定 double 值执行转换并返回 int。

## 测试

- 描述: double 到 int 的截断转换
- 断言: 正确转换

```java
// 方法体开始
System.out.println("=== DoubleToIntFunction ===");
DoubleToIntFunction floor = v -> (int) Math.floor(v);
assertEquals(3, floor.applyAsInt(3.99));
assertEquals(-4, floor.applyAsInt(-3.5));
System.out.println("floor(3.99) = " + floor.applyAsInt(3.99));
System.out.println("floor(-3.5) = " + floor.applyAsInt(-3.5));
System.out.println("=== 测试通过 ===");
// 方法体结束
```
