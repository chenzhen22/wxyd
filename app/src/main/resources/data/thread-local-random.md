---
name: ThreadLocalRandom
package: java.util.concurrent
order: 44
---

## 介绍

`java.util.concurrent.ThreadLocalRandom` 是 Java 7 引入的**线程本地随机数生成器**，比 `Random` 在多线程环境下有更好的性能（每个线程独立维护随机数种子，减少竞争）。

Java 8 新增了 `ints`、`longs`、`doubles` 等流式方法。

## 方法

### current

```java
public static ThreadLocalRandom current()
```

获取当前线程的 ThreadLocalRandom 实例。

- **返回**: `ThreadLocalRandom`

### nextInt()

```java
public int nextInt()
```

返回随机整数。

- **返回**: `int`

### nextInt(int)

```java
public int nextInt(int bound)
```

返回 [0, bound) 之间的随机整数。

- **参数**: `bound` — 上界（不含）
- **返回**: `int`

### nextInt(int, int)

```java
public int nextInt(int origin, int bound)
```

返回 [origin, bound) 之间的随机整数。

- **参数**: `origin` — 下界；`bound` — 上界
- **返回**: `int`

### nextLong

```java
public long nextLong(long origin, long bound)
```

返回 [origin, bound) 之间的随机 long。

- **参数**: `origin` — 下界；`bound` — 上界
- **返回**: `long`

### nextDouble

```java
public double nextDouble(double origin, double bound)
```

返回 [origin, bound) 之间的随机 double。

- **参数**: `origin` — 下界；`bound` — 上界
- **返回**: `double`

### ints

```java
public IntStream ints(int streamSize)
```

生成指定数量的随机 int 流（Java 8 新增）。

- **参数**: `streamSize` — 数量
- **返回**: `IntStream`

## 测试

### current

- 描述: 获取当前线程的随机数生成器
- 断言: 返回不为 null

```java
// 方法体开始
System.out.println("=== current ===");
ThreadLocalRandom rng = ThreadLocalRandom.current();
assertNotNull(rng);
System.out.println("ThreadLocalRandom: " + rng);
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### nextInt

- 描述: 生成随机整数
- 断言: 在范围内

```java
// 方法体开始
System.out.println("=== nextInt ===");
ThreadLocalRandom rng = ThreadLocalRandom.current();
int val = rng.nextInt(1, 100);
assertTrue(val >= 1 && val < 100);
System.out.println("随机整数 [1,100): " + val);
int bounded = rng.nextInt(10);
assertTrue(bounded >= 0 && bounded < 10);
System.out.println("随机整数 [0,10): " + bounded);
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### nextLong

- 描述: 生成随机 long
- 断言: 在范围内

```java
// 方法体开始
System.out.println("=== nextLong ===");
ThreadLocalRandom rng = ThreadLocalRandom.current();
long val = rng.nextLong(1000, 2000);
assertTrue(val >= 1000 && val < 2000);
System.out.println("随机 long [1000,2000): " + val);
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### nextDouble

- 描述: 生成随机 double
- 断言: 在范围内

```java
// 方法体开始
System.out.println("=== nextDouble ===");
ThreadLocalRandom rng = ThreadLocalRandom.current();
double val = rng.nextDouble(10.0, 20.0);
assertTrue(val >= 10.0 && val < 20.0);
System.out.println("随机 double [10,20): " + val);
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### ints

- 描述: 生成随机 int 流
- 断言: 流包含指定数量元素

```java
// 方法体开始
System.out.println("=== ints ===");
ThreadLocalRandom rng = ThreadLocalRandom.current();
long count = rng.ints(5).count();
assertEquals(5, count);
System.out.println("ints(5) 元素数量: " + count);
System.out.println("=== 测试通过 ===");
// 方法体结束
```
