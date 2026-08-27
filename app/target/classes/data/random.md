---
name: Random
package: java.util
order: 163
---

## 介绍

`java.util.Random` 在 Java 8 中新增了多个生成**随机数流**的方法，可以方便地生成无穷随机数流供 Stream API 使用。

Java 8 新增的方法：
- `ints()` — 生成无穷 int 随机数流
- `ints(long streamSize)` — 生成指定数量的 int 随机数流
- `ints(int randomNumberOrigin, int randomNumberBound)` — 无穷范围内 int 流
- `ints(long streamSize, int randomNumberOrigin, int randomNumberBound)` — 有限范围内 int 流
- `longs()` / `doubles()` — 对应 long 和 double 版本

## 方法

### ints

```java
public IntStream ints()
public IntStream ints(long streamSize)
public IntStream ints(int randomNumberOrigin, int randomNumberBound)
public IntStream ints(long streamSize, int randomNumberOrigin, int randomNumberBound)
```

返回伪随机 int 值流。

### doubles

```java
public DoubleStream doubles()
public DoubleStream doubles(long streamSize)
```

返回伪随机 double 值流（0.0 到 1.0）。

## 测试

### ints 基本

- 描述: 生成 5 个随机 int
- 断言: 生成指定数量的随机数

```java
// 方法体开始
System.out.println("=== ints ===");
Random r = new Random(42);  // 固定种子保证可测试
int[] arr = r.ints(5).toArray();
assertEquals(5, arr.length);
System.out.println("5 个随机 int: " + java.util.Arrays.toString(arr));
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### ints 范围

- 描述: 生成 0-100 范围内的 10 个随机数
- 断言: 所有值在范围内

```java
// 方法体开始
System.out.println("=== ints 范围 ===");
Random r = new Random(123);
int[] arr = r.ints(10, 0, 100).toArray();
assertEquals(10, arr.length);
for (int v : arr) {
    assertTrue(v >= 0 && v < 100);
}
System.out.println("0-99 随机数: " + java.util.Arrays.toString(arr));
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### doubles

- 描述: 生成 5 个随机 double
- 断言: 值在 0.0-1.0 之间

```java
// 方法体开始
System.out.println("=== doubles ===");
Random r = new Random(42);
double[] arr = r.doubles(5).toArray();
assertEquals(5, arr.length);
for (double v : arr) {
    assertTrue(v >= 0.0 && v < 1.0);
}
System.out.println("5 个随机 double: " + java.util.Arrays.toString(arr));
System.out.println("=== 测试通过 ===");
// 方法体结束
```
