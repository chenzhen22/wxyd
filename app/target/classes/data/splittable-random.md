---
name: SplittableRandom
package: java.util
order: 55
---

## 介绍

`java.util.SplittableRandom` 是 Java 8 新增的**快速伪随机数生成器**，专门设计用于并行计算。它可以高效地分裂（split）出新的 Random 实例，每个实例独立生成随机数，互不干扰。

与 `Random` 和 `ThreadLocalRandom` 的区别：
- 比 `Random` 更快，支持并行分裂
- 适用于 `ForkJoinTask` 或并行流的随机数生成
- 不是线程安全的（每次分裂出新的实例用于不同线程）

## 方法

### SplittableRandom()

```java
public SplittableRandom()
```

创建随机数生成器。

### split

```java
public SplittableRandom split()
```

分裂出一个新的 SplittableRandom 实例。

- **返回**: `SplittableRandom`

### nextInt()

```java
public int nextInt()
```

返回随机 int。

- **返回**: `int`

### nextInt(int, int)

```java
public int nextInt(int origin, int bound)
```

返回 [origin, bound) 范围内的随机 int。

- **参数**: `origin` — 下界；`bound` — 上界
- **返回**: `int`

### nextLong

```java
public long nextLong(long origin, long bound)
```

返回 [origin, bound) 范围内的随机 long。

- **参数**: `origin` — 下界；`bound` — 上界
- **返回**: `long`

### nextDouble

```java
public double nextDouble(double origin, double bound)
```

返回 [origin, bound) 范围内的随机 double。

- **参数**: `origin` — 下界；`bound` — 上界
- **返回**: `double`

### ints

```java
public IntStream ints(long streamSize)
```

生成指定数量的随机 int 流。

- **参数**: `streamSize` — 数量
- **返回**: `IntStream`

## 测试

### SplittableRandom

- 描述: 创建并生成随机数
- 断言: 随机数在范围内

```java
// 方法体开始
System.out.println("=== splittableRandom ===");
SplittableRandom rng = new SplittableRandom();
int val = rng.nextInt(1, 100);
assertTrue(val >= 1 && val < 100);
System.out.println("随机 [1,100): " + val);
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### split

- 描述: 分裂出新的随机数生成器
- 断言: 新生成器不为 null

```java
// 方法体开始
System.out.println("=== split ===");
SplittableRandom rng1 = new SplittableRandom();
SplittableRandom rng2 = rng1.split();
assertNotNull(rng2);
int v1 = rng1.nextInt(100);
int v2 = rng2.nextInt(100);
System.out.println("原始: " + v1 + ", 分裂后: " + v2);
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### nextIntRange

- 描述: 指定范围的随机 int
- 断言: 在范围内

```java
// 方法体开始
System.out.println("=== nextIntRange ===");
SplittableRandom rng = new SplittableRandom();
for (int i = 0; i < 10; i++) {
    int val = rng.nextInt(10, 20);
    assertTrue(val >= 10 && val < 20);
}
System.out.println("nextInt(10,20) 测试通过");
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### nextLong

- 描述: 指定范围的随机 long
- 断言: 在范围内

```java
// 方法体开始
System.out.println("=== nextLong ===");
SplittableRandom rng = new SplittableRandom();
long val = rng.nextLong(1000, 2000);
assertTrue(val >= 1000 && val < 2000);
System.out.println("nextLong(1000,2000): " + val);
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### nextDouble

- 描述: 指定范围的随机 double
- 断言: 在范围内

```java
// 方法体开始
System.out.println("=== nextDouble ===");
SplittableRandom rng = new SplittableRandom();
double val = rng.nextDouble(5.0, 10.0);
assertTrue(val >= 5.0 && val < 10.0);
System.out.println("nextDouble(5,10): " + val);
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### ints

- 描述: 生成随机 int 流
- 断言: 流包含指定数量元素

```java
// 方法体开始
System.out.println("=== ints ===");
SplittableRandom rng = new SplittableRandom();
long count = rng.ints(5).count();
assertEquals(5, count);
System.out.println("ints(5) 元素数: " + count);
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### nextInt

- 描述: 生成随机 int
- 断言: 值在范围内

```java
// 方法体开始
System.out.println("=== nextInt ===");
SplittableRandom rng = new SplittableRandom();
int val = rng.nextInt(10, 20);
assertTrue(val >= 10 && val < 20);
System.out.println("nextInt(10,20): " + val);
System.out.println("=== 测试通过 ===");
// 方法体结束
```

