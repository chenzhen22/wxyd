---
name: DoubleAdder
package: java.util.concurrent.atomic
order: 69
---

## 介绍

`DoubleAdder` 是 Java 8 引入的**高性能 double 累加器**，是 `AtomicDouble` 的高吞吐替代方案。它内部维护一组变量，多个线程可以并发更新，最终通过 `sum()` 获取累加结果。

DoubleAdder 的核心特点：
- **高吞吐**：内部使用 Cell 数组分散竞争，多线程写入时性能优于 AtomicDouble
- **惰性求和**：`sum()` 每次返回准确值但可能较慢；`sumThenReset()` 可以在快照后重置
- **适用于统计**：非常适合**频繁更新、偶尔读取**的统计场景
- **非阻塞**：基于 Striped 64 算法（LongAdder 的 double 版本）

对应关系：
- `LongAdder` — long 版本的高性能累加器
- `LongAccumulator` / `DoubleAccumulator` — 支持自定义运算的累加器

## 方法

### 构造方法

```java
public DoubleAdder()
```

创建一个初始值为 0 的 DoubleAdder。

### add

```java
public void add(double x)
```

添加指定的 double 值。

- **参数**: `x` — 要添加的值
- **返回**: 无

### sum

```java
public double sum()
```

返回当前累加总和。在没有并发更新的情况下返回准确值。

- **返回**: `double` — 当前总和

### sumThenReset

```java
public double sumThenReset()
```

等效于先调用 `sum()` 再调用 `reset()`。这在多线程环境下可以获取快照并重置，避免后续线程继续累加旧值。

- **返回**: `double` — 总和
- **注意**: 此方法不是原子性的，但比分别调用 sum 和 reset 更一致

### reset

```java
public void reset()
```

将总和重置为 0。

- **返回**: 无

### doubleValue

```java
public double doubleValue()
```

等价于 `sum()`。

- **返回**: `double` — 当前总和

### intValue / longValue / floatValue

- **返回**: 分别返回 `sum()` 的 int、long、float 转换

## 测试

### add 和 sum

- 描述: 使用 `add` 添加多个值并获取总和
- 断言: 总和等于所有添加值的和

```java
// 方法体开始
System.out.println("=== add / sum ===");
DoubleAdder adder = new DoubleAdder();
adder.add(1.5);
adder.add(2.5);
adder.add(3.0);
assertEquals(7.0, adder.sum(), 0.0001);
System.out.println("总和: " + adder.sum());
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### sumThenReset

- 描述: 使用 `sumThenReset` 获取并重置总和
- 断言: 第一次求和为累加值，重置后为 0

```java
// 方法体开始
System.out.println("=== sumThenReset ===");
DoubleAdder adder = new DoubleAdder();
adder.add(10.5);
adder.add(20.5);
double sum1 = adder.sumThenReset();
assertEquals(31.0, sum1, 0.0001);
assertEquals(0.0, adder.sum(), 0.0001);
System.out.println("第一次求和: " + sum1 + ", 重置后: " + adder.sum());
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### 多线程累加

- 描述: 多个线程并发累加 DoubleAdder
- 断言: 最终总和等于所有线程添加值的总和

```java
// 方法体开始
System.out.println("=== 多线程累加 ===");
DoubleAdder adder = new DoubleAdder();
int threadCount = 5;
int addsPerThread = 1000;
Thread[] threads = new Thread[threadCount];
for (int i = 0; i < threadCount; i++) {
    threads[i] = new Thread(() -> {
        for (int j = 0; j < addsPerThread; j++) {
            adder.add(1.0);
        }
    });
    threads[i].start();
}
for (Thread t : threads) t.join();
assertEquals((double)(threadCount * addsPerThread), adder.sum(), 0.0001);
System.out.println("多线程累加结果: " + adder.sum());
System.out.println("=== 测试通过 ===");
// 方法体结束
```
