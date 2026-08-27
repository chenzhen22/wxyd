---
name: CountDownLatch
package: java.util.concurrent
order: 13
---

## 介绍

`CountDownLatch` 是 Java 5 引入的同步辅助类，允许一个或多个线程等待其他线程完成操作。它用给定的计数初始化，`await()` 方法阻塞直到计数通过 `countDown()` 降为 0。

常见用途：
- 主线程等待多个子任务完成
- 并行任务协调

## 方法

### CountDownLatch(int count)

```java
public CountDownLatch(int count)
```

构造一个以给定计数初始化的 `CountDownLatch`。

- **参数**: `count` — 需要等待的线程数（必须为正数）
- **异常**: `IllegalArgumentException` — 如果 count 为负数

### await

```java
public void await() throws InterruptedException
```

阻塞当前线程直到计数降为 0，或者当前线程被中断。

- **异常**: `InterruptedException` — 当前线程在等待时被中断

### await(long timeout, TimeUnit unit)

```java
public boolean await(long timeout, TimeUnit unit) throws InterruptedException
```

阻塞当前线程直到计数降为 0，或者指定的超时时间到达，或者当前线程被中断。

- **参数**: `timeout` — 最长等待时间；`unit` — 时间单位
- **返回**: `boolean` — 如果计数降为 0 返回 true，否则超时返回 false
- **异常**: `InterruptedException` — 当前线程在等待时被中断

### countDown

```java
public void countDown()
```

递减计数，如果计数降为 0，则释放所有等待的线程。

### getCount

```java
public long getCount()
```

返回当前计数。该方法通常用于调试和测试。

- **返回**: `long` — 当前计数

### toString

```java
public String toString()
```

返回一个标识此锁存器的字符串及其状态。

- **返回**: `String`

## 测试

### CountDownLatch

- 描述: 测试构造函数 CountDownLatch(int count)
- 断言: 构造后计数为初始值

```java
// 方法体开始
System.out.println("=== CountDownLatch ===");
CountDownLatch latch = new CountDownLatch(3);
assertNotNull(latch);
assertEquals(3, latch.getCount());
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### await

- 描述: 测试 await() 阻塞等待，另一个线程 countDown 后解除阻塞
- 断言: await 在 countDown 后正常返回，getCount 为 0

```java
// 方法体开始
System.out.println("=== await ===");
CountDownLatch latch = new CountDownLatch(1);
Thread t = new Thread(() -> {
    latch.countDown();
});
t.start();
latch.await();
t.join();
assertEquals(0, latch.getCount());
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### await(timeout, unit)

- 描述: 测试 await(long, TimeUnit) 的超时行为
- 断言: 计数未归零时超时返回 false，计数归零后返回 true

```java
// 方法体开始
System.out.println("=== await(timeout, unit) ===");
CountDownLatch latch = new CountDownLatch(1);
assertFalse(latch.await(10, TimeUnit.MILLISECONDS));
latch.countDown();
assertTrue(latch.await(1, TimeUnit.SECONDS));
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### countDown

- 描述: 测试 countDown 递减计数
- 断言: 每次 countDown 后计数减 1

```java
// 方法体开始
System.out.println("=== countDown ===");
CountDownLatch latch = new CountDownLatch(3);
latch.countDown();
assertEquals(2, latch.getCount());
latch.countDown();
assertEquals(1, latch.getCount());
latch.countDown();
assertEquals(0, latch.getCount());
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### getCount

- 描述: 测试 getCount 返回当前计数
- 断言: 构造后和 countDown 后返回值正确

```java
// 方法体开始
System.out.println("=== getCount ===");
CountDownLatch latch = new CountDownLatch(5);
assertEquals(5, latch.getCount());
latch.countDown();
assertEquals(4, latch.getCount());
latch.countDown();
assertEquals(3, latch.getCount());
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### toString

- 描述: 测试 toString 方法
- 断言: toString 包含 CountDownLatch 类名和计数信息

```java
// 方法体开始
System.out.println("=== toString ===");
CountDownLatch latch = new CountDownLatch(2);
String str = latch.toString();
assertTrue(str.contains("CountDownLatch"));
assertTrue(str.contains("2") || str.contains("count"));
latch.countDown();
str = latch.toString();
assertTrue(str.contains("1") || str.contains("count"));
System.out.println("toString: " + str);
System.out.println("=== 测试通过 ===");
// 方法体结束
```
