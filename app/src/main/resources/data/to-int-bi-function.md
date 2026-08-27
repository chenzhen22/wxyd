---
name: ToIntBiFunction
package: java.util.function
order: 94
---

## 介绍

`ToIntBiFunction<T, U>` 是 Java 8 引入的一个**函数式接口**，代表一个接受两个对象参数并返回 `int` 值的函数。

对应的特化接口：
- `ToLongBiFunction<T, U>` — 返回 long
- `ToDoubleBiFunction<T, U>` — 返回 double

## 方法

### applyAsInt

```java
int applyAsInt(T t, U u)
```

对给定两个参数执行计算并返回 int。

## 测试

### 计算距离

- 描述: 计算两个点的曼哈顿距离
- 断言: (1,1) 到 (4,5) 的距离为 7

```java
// 方法体开始
System.out.println("=== 曼哈顿距离 ===");
ToIntBiFunction<Integer, Integer> manhattan = (a, b) -> Math.abs(a) + Math.abs(b);
int dist = manhattan.applyAsInt(3, 4);
assertEquals(7, dist);
System.out.println("|3| + |4| = " + dist);
System.out.println("=== 测试通过 ===");
// 方法体结束
```
