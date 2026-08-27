---
name: CyclicBarrier
package: java.util.concurrent
order: 32
---

## 介绍

`CyclicBarrier` 是 Java 5 引入的同步辅助类，它允许一组线程互相等待，直到所有线程都到达某个公共屏障点 (barrier point)。与 `CountDownLatch` 不同，`CyclicBarrier` 在释放等待线程后可以重用（reset），因此称为"循环"屏障。

常见用途：
- 多线程计算中等待所有子任务完成后再合并结果
- 并行测试中协调多个线程同时开始执行

## 方法

### CyclicBarrier(int parties)

```java
public CyclicBarrier(int parties)
```

构造一个 `CyclicBarrier`，当给定数量的线程（`parties`）都调用 `await()` 时屏障被解除。

- **参数**: `parties` — 必须调用 `await()` 的线程数量（必须为正数）
- **异常**: `IllegalArgumentException` — 如果 `parties` 小于 1

### CyclicBarrier(int parties, Runnable barrierAction)

```java
public CyclicBarrier(int parties, Runnable barrierAction)
```

构造一个 `CyclicBarrier`，当所有线程到达屏障后，在释放它们之前执行给定的 `barrierAction`。

- **参数**: `parties` — 必须调用 `await()` 的线程数量；`barrierAction` — 屏障解除前执行的命令
- **异常**: `IllegalArgumentException` — 如果 `parties` 小于 1

### await

```java
public int await() throws InterruptedException, BrokenBarrierException
```

等待所有 `parties` 都已在此屏障上调用 `await()`。如果当前线程不是最后一个到达的，则阻塞；最后一个线程到达时所有等待线程被释放。

- **返回**: `int` — 当前线程的到达索引，0 表示第一个到达
- **异常**: `InterruptedException` — 当前线程在等待时被中断；`BrokenBarrierException` — 屏障被破坏

### await(long timeout, TimeUnit unit)

```java
public int await(long timeout, TimeUnit unit) throws InterruptedException, BrokenBarrierException, TimeoutException
```

等待所有 `parties` 都已在此屏障上调用 `await()`，但最多等待给定的超时时间。

- **参数**: `timeout` — 最长等待时间；`unit` — 时间单位
- **返回**: `int` — 当前线程的到达索引
- **异常**: `TimeoutException` — 超时时间到达而屏障未被触发

### getNumberWaiting

```java
public int getNumberWaiting()
```

返回当前在屏障处等待的线程数。

- **返回**: `int` — 当前等待的线程数

### getParties

```java
public int getParties()
```

返回触发此屏障所需的线程数。

- **返回**: `int` — 屏障需要的线程数

### isBroken

```java
public boolean isBroken()
```

查询此屏障是否处于损坏状态。

- **返回**: `boolean` — 如果屏障已损坏返回 true

### reset

```java
public void reset()
```

将屏障重置为初始状态。如果有线程正在等待，它们会收到 `BrokenBarrierException`。

## 测试

### CyclicBarrier(int)

- 描述: 测试构造函数 CyclicBarrier(int parties)
- 断言: 构造后 getParties 返回正确值

```java
// 方法体开始
System.out.println("=== CyclicBarrier(int) ===");
CyclicBarrier barrier = new CyclicBarrier(3);
assertNotNull(barrier);
assertEquals(3, barrier.getParties());
assertEquals(0, barrier.getNumberWaiting());
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### CyclicBarrier(Runnable)

- 描述: 测试构造函数 CyclicBarrier(int, Runnable) 及屏障动作
- 断言: 屏障触发后 barrierAction 被执行

```java
// 方法体开始
System.out.println("=== CyclicBarrier(Runnable) ===");
final boolean[] flag = {false};
CyclicBarrier barrier = new CyclicBarrier(2, () -> flag[0] = true);
Thread t1 = new Thread(() -> {
    try { barrier.await(); } catch (Exception e) {}
});
Thread t2 = new Thread(() -> {
    try { barrier.await(); } catch (Exception e) {}
});
t1.start();
t2.start();
t1.join();
t2.join();
assertTrue(flag[0]);
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### await

- 描述: 测试 await() 多线程同步，两个线程同时到达屏障
- 断言: 所有线程 await 返回后计数归零

```java
// 方法体开始
System.out.println("=== await ===");
CyclicBarrier barrier = new CyclicBarrier(2);
final boolean[] result = {false, false};
Thread t1 = new Thread(() -> {
    try {
        barrier.await();
        result[0] = true;
    } catch (Exception e) {}
});
Thread t2 = new Thread(() -> {
    try {
        barrier.await();
        result[1] = true;
    } catch (Exception e) {}
});
t1.start();
t2.start();
t1.join();
t2.join();
assertTrue(result[0]);
assertTrue(result[1]);
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### getParties

- 描述: 测试 getParties 方法
- 断言: 返回构造时指定的线程数

```java
// 方法体开始
System.out.println("=== getParties ===");
CyclicBarrier barrier = new CyclicBarrier(5);
assertEquals(5, barrier.getParties());
CyclicBarrier barrier2 = new CyclicBarrier(1);
assertEquals(1, barrier2.getParties());
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### getNumberWaiting

- 描述: 测试 getNumberWaiting 方法
- 断言: 返回当前在屏障处等待的线程数

```java
// 方法体开始
System.out.println("=== getNumberWaiting ===");
CyclicBarrier barrier = new CyclicBarrier(3);
assertEquals(0, barrier.getNumberWaiting());
Thread t = new Thread(() -> {
    try { barrier.await(); } catch (Exception e) {}
});
t.start();
Thread.sleep(50);
assertEquals(1, barrier.getNumberWaiting());
t.interrupt();
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### isBroken

- 描述: 查询屏障是否损坏
- 断言: 新创建的屏障 isBroken 为 false

```java
// 方法体开始
System.out.println("=== isBroken ===");
CyclicBarrier barrier = new CyclicBarrier(2);
assertFalse(barrier.isBroken());
System.out.println("新屏障 isBroken: " + barrier.isBroken());
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### reset

- 描述: 测试 reset 方法重置屏障
- 断言: reset 后屏障回到初始状态

```java
// 方法体开始
System.out.println("=== reset ===");
CyclicBarrier barrier = new CyclicBarrier(2);
Thread t = new Thread(() -> {
    try { barrier.await(); } catch (Exception e) {}
});
t.start();
Thread.sleep(50);
barrier.reset();
t.join();
assertFalse(barrier.isBroken());
assertEquals(0, barrier.getNumberWaiting());
assertEquals(2, barrier.getParties());
System.out.println("=== 测试通过 ===");
// 方法体结束
```
