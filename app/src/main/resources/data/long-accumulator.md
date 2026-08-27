---
name: LongAccumulator
package: java.util.concurrent.atomic
order: 82
---

## 介绍

`LongAccumulator` 是 Java 8 引入的**高性能 long 累加器**，与 `LongAdder` 类似但更灵活。它允许你指定一个**自定义二元运算**（BinaryOperator），而不仅仅是加法。内部同样使用 Cell 数组分散竞争，实现高吞吐并发更新。

LongAccumulator 的核心特点：
- **自定义运算**：可以指定求和、求最大值、求最小值等任意结合运算
- **高吞吐**：与 LongAdder 一样使用 Striped 64 算法分散竞争
- **非阻塞**：基于 CAS 操作，无锁设计
- **惰性求值**：`get()` 遍历所有 Cell 计算结果

构造方法参数：
- `accumulatorFunction` — 二元运算（如 `Long::max`、`Long::min`、`(a, b) -> a + b`）
- `identity` — 恒等值（如求和用 0、求最大值用 Long.MIN_VALUE、求最小值用 Long.MAX_VALUE）

对应关系：
- `DoubleAccumulator` — double 版本的自定义累加器
- `LongAdder` — 求和专用版本（性能略高于 LongAccumulator）

## 方法

### 构造方法

```java
public LongAccumulator(LongBinaryOperator accumulatorFunction, long identity)
```

创建一个 LongAccumulator。

- **参数**: `accumulatorFunction` — 结合运算；`identity` — 恒等值

### accumulate

```java
public void accumulate(long x)
```

使用当前值和给定值执行累加运算。

- **参数**: `x` — 要累加的值
- **返回**: 无

### get

```java
public long get()
```

返回当前累加结果。

- **返回**: `long` — 当前结果

### getThenReset

```java
public long getThenReset()
```

等效于先调用 `get()` 再调用 `reset()`。

- **返回**: `long` — 结果

### reset

```java
public void reset()
```

将值重置为恒等值。

- **返回**: 无

## 测试

### 求和

- 描述: 使用 LongAccumulator 求和（等价于 LongAdder）
- 断言: 1+2+3+4+5 = 15

```java
// 方法体开始
System.out.println("=== 求和 ===");
LongAccumulator acc = new LongAccumulator((a, b) -> a + b, 0);
acc.accumulate(1);
acc.accumulate(2);
acc.accumulate(3);
acc.accumulate(4);
acc.accumulate(5);
assertEquals(15L, acc.get());
System.out.println("求和: " + acc.get());
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### 求最大值

- 描述: 使用 LongAccumulator 求最大值
- 断言: 最大值为 100

```java
// 方法体开始
System.out.println("=== 求最大值 ===");
LongAccumulator max = new LongAccumulator(Long::max, Long.MIN_VALUE);
max.accumulate(10);
max.accumulate(50);
max.accumulate(100);
max.accumulate(30);
assertEquals(100L, max.get());
System.out.println("最大值: " + max.get());
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### 求最小值

- 描述: 使用 LongAccumulator 求最小值
- 断言: 最小值为 -5

```java
// 方法体开始
System.out.println("=== 求最小值 ===");
LongAccumulator min = new LongAccumulator(Long::min, Long.MAX_VALUE);
min.accumulate(10);
min.accumulate(-5);
min.accumulate(100);
assertEquals(-5L, min.get());
System.out.println("最小值: " + min.get());
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### 多线程累加

- 描述: 多个线程并发累加 LongAccumulator
- 断言: 最终总和正确

```java
// 方法体开始
System.out.println("=== 多线程累加 ===");
LongAccumulator acc = new LongAccumulator((a, b) -> a + b, 0);
int threadCount = 5;
Thread[] threads = new Thread[threadCount];
for (int i = 0; i < threadCount; i++) {
    threads[i] = new Thread(() -> {
        for (int j = 0; j < 1000; j++) {
            acc.accumulate(1);
        }
    });
    threads[i].start();
}
for (Thread t : threads) t.join();
assertEquals(5000L, acc.get());
System.out.println("多线程累加: " + acc.get());
System.out.println("=== 测试通过 ===");
// 方法体结束
```
