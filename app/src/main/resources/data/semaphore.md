---
name: Semaphore
package: java.util.concurrent
order: 14
---

## 介绍

`Semaphore` 是 Java 5 引入的计数信号量，用于控制同时访问特定资源的线程数。它通过维护一个许可集来管理访问，线程在执行前必须先获取许可，使用完毕后释放许可。

常见用途：
- 限流控制（限制并发访问数）
- 资源池管理
- 生产者-消费者模式

## 方法

### Semaphore(int permits)

```java
public Semaphore(int permits)
```

创建具有给定许可数的非公平信号量。

- **参数**: `permits` — 初始许可数
- **异常**: `IllegalArgumentException` — 如果 permits 为负数

### Semaphore(int permits, boolean fair)

```java
public Semaphore(int permits, boolean fair)
```

创建具有给定许可数的信号量，并指定是否公平。

- **参数**: `permits` — 初始许可数；`fair` — 如果为 true，则按 FIFO 顺序授予许可

### acquire

```java
public void acquire() throws InterruptedException
```

获取一个许可，如果没有可用许可则阻塞直到有许可可用或线程被中断。

- **异常**: `InterruptedException` — 当前线程在等待时被中断

### acquire(int permits)

```java
public void acquire(int permits) throws InterruptedException
```

获取指定数量的许可。

- **参数**: `permits` — 要获取的许可数
- **异常**: `InterruptedException` — 当前线程在等待时被中断；`IllegalArgumentException` — 如果 permits 为负数或 0

### release

```java
public void release()
```

释放一个许可，将其返回到信号量中。

### release(int permits)

```java
public void release(int permits)
```

释放指定数量的许可。

- **参数**: `permits` — 要释放的许可数
- **异常**: `IllegalArgumentException` — 如果 permits 为负数或 0

### tryAcquire

```java
public boolean tryAcquire()
```

仅在调用时许可可用的情况下获取一个许可。如果许可可用则立即返回 true，否则返回 false。

- **返回**: `boolean` — 如果获取到许可返回 true

### tryAcquire(long timeout, TimeUnit unit)

```java
public boolean tryAcquire(long timeout, TimeUnit unit) throws InterruptedException
```

如果在给定等待时间内许可可用且当前线程未被中断，则获取一个许可。

- **参数**: `timeout` — 最长等待时间；`unit` — 时间单位
- **返回**: `boolean` — 如果获取到许可返回 true
- **异常**: `InterruptedException` — 当前线程在等待时被中断

### availablePermits

```java
public int availablePermits()
```

返回当前可用许可数。

- **返回**: `int` — 可用许可数

### drainPermits

```java
public int drainPermits()
```

获取并返回所有立即可用的许可。

- **返回**: `int` — 获取到的许可数

### isFair

```java
public boolean isFair()
```

如果信号量采用公平性设置则返回 true。

- **返回**: `boolean`

### hasQueuedThreads

```java
public boolean hasQueuedThreads()
```

查询是否有线程正在等待获取许可。

- **返回**: `boolean`

### getQueueLength

```java
public int getQueueLength()
```

返回正在等待获取许可的线程数的估计值。

- **返回**: `int` — 等待线程数的估计值

### toString

```java
public String toString()
```

返回标识此信号量的字符串及其状态。

- **返回**: `String`

## 测试

### Semaphore(int)

- 描述: 测试 Semaphore(int) 构造函数和基本状态
- 断言: 构造后 availablePermits 等于初始值，isFair 为 false

```java
// 方法体开始
System.out.println("=== Semaphore(int) ===");
Semaphore sem = new Semaphore(3);
assertEquals(3, sem.availablePermits());
assertFalse(sem.isFair());
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### Semaphore(boolean)

- 描述: 测试 Semaphore(int, boolean) 构造函数和公平性
- 断言: 公平信号量 isFair 返回 true，非公平返回 false

```java
// 方法体开始
System.out.println("=== Semaphore(boolean) ===");
Semaphore fair = new Semaphore(1, true);
assertTrue(fair.isFair());
assertEquals(1, fair.availablePermits());
Semaphore unfair = new Semaphore(1, false);
assertFalse(unfair.isFair());
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### acquire

- 描述: 测试 acquire() 获取一个许可
- 断言: acquire 后可用许可减 1

```java
// 方法体开始
System.out.println("=== acquire ===");
Semaphore sem = new Semaphore(3);
sem.acquire();
assertEquals(2, sem.availablePermits());
sem.acquire();
assertEquals(1, sem.availablePermits());
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### acquire(int)

- 描述: 测试 acquire(int) 批量获取许可
- 断言: acquire(3) 后可用许可减 3

```java
// 方法体开始
System.out.println("=== acquire(int) ===");
Semaphore sem = new Semaphore(5);
sem.acquire(3);
assertEquals(2, sem.availablePermits());
sem.acquire(2);
assertEquals(0, sem.availablePermits());
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### release

- 描述: 测试 release() 释放一个许可
- 断言: release 后可用许可加 1

```java
// 方法体开始
System.out.println("=== release ===");
Semaphore sem = new Semaphore(0);
sem.release();
assertEquals(1, sem.availablePermits());
sem.release();
assertEquals(2, sem.availablePermits());
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### release(int)

- 描述: 测试 release(int) 批量释放许可
- 断言: release(3) 后可用许可加 3

```java
// 方法体开始
System.out.println("=== release(int) ===");
Semaphore sem = new Semaphore(0);
sem.release(3);
assertEquals(3, sem.availablePermits());
sem.release(2);
assertEquals(5, sem.availablePermits());
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### tryAcquire

- 描述: 测试 tryAcquire() 尝试获取许可
- 断言: 许可充足时返回 true，不足时返回 false

```java
// 方法体开始
System.out.println("=== tryAcquire ===");
Semaphore sem = new Semaphore(2);
assertTrue(sem.tryAcquire());
assertTrue(sem.tryAcquire());
assertFalse(sem.tryAcquire());
sem.release();
assertTrue(sem.tryAcquire());
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### tryAcquire(timeout, unit)

- 描述: 测试 tryAcquire(long, TimeUnit) 超时获取许可
- 断言: 许可可用时返回 true，不可用时超时返回 false

```java
// 方法体开始
System.out.println("=== tryAcquire(timeout, unit) ===");
Semaphore sem = new Semaphore(1);
assertTrue(sem.tryAcquire(100, TimeUnit.MILLISECONDS));
assertFalse(sem.tryAcquire(10, TimeUnit.MILLISECONDS));
sem.release();
assertTrue(sem.tryAcquire(100, TimeUnit.MILLISECONDS));
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### availablePermits

- 描述: 测试 availablePermits 返回可用许可数
- 断言: acquire/release 后返回值正确

```java
// 方法体开始
System.out.println("=== availablePermits ===");
Semaphore sem = new Semaphore(5);
assertEquals(5, sem.availablePermits());
sem.acquire();
assertEquals(4, sem.availablePermits());
sem.release(2);
assertEquals(6, sem.availablePermits());
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### drainPermits

- 描述: 测试 drainPermits 获取所有可用许可
- 断言: drainPermits 返回并清空所有可用许可

```java
// 方法体开始
System.out.println("=== drainPermits ===");
Semaphore sem = new Semaphore(5);
assertEquals(5, sem.availablePermits());
int drained = sem.drainPermits();
assertEquals(5, drained);
assertEquals(0, sem.availablePermits());
sem.release(3);
drained = sem.drainPermits();
assertEquals(3, drained);
assertEquals(0, sem.availablePermits());
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### isFair

- 描述: 测试 isFair 方法
- 断言: 公平信号量返回 true，非公平返回 false

```java
// 方法体开始
System.out.println("=== isFair ===");
assertTrue(new Semaphore(1, true).isFair());
assertFalse(new Semaphore(1, false).isFair());
assertFalse(new Semaphore(1).isFair());
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### hasQueuedThreads

- 描述: 测试 hasQueuedThreads 判断是否有等待线程
- 断言: 有线程等待时返回 true，完成后返回 false

```java
// 方法体开始
System.out.println("=== hasQueuedThreads ===");
Semaphore sem = new Semaphore(0);
assertFalse(sem.hasQueuedThreads());
Thread t = new Thread(() -> {
    try {
        sem.acquire();
    } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
    }
});
t.start();
Thread.sleep(50);
assertTrue(sem.hasQueuedThreads());
sem.release();
t.join();
assertFalse(sem.hasQueuedThreads());
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### getQueueLength

- 描述: 测试 getQueueLength 获取等待线程数
- 断言: 有线程等待时返回正值，完成后返回 0

```java
// 方法体开始
System.out.println("=== getQueueLength ===");
Semaphore sem = new Semaphore(0);
assertEquals(0, sem.getQueueLength());
Thread t = new Thread(() -> {
    try {
        sem.acquire();
    } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
    }
});
t.start();
Thread.sleep(50);
assertTrue(sem.getQueueLength() >= 1);
sem.release();
t.join();
assertEquals(0, sem.getQueueLength());
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### toString

- 描述: 测试 toString 方法
- 断言: toString 包含 Semaphore 类名和许可信息

```java
// 方法体开始
System.out.println("=== toString ===");
Semaphore sem = new Semaphore(2);
String str = sem.toString();
assertTrue(str.contains("Semaphore"));
System.out.println("toString: " + str);
System.out.println("=== 测试通过 ===");
// 方法体结束
```
