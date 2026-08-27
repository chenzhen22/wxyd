---
name: AbstractQueuedSynchronizer
package: java.util.concurrent.locks
order: 135
---

## 介绍

`java.util.concurrent.locks.AbstractQueuedSynchronizer`（AQS）是 Java 并发包的**核心基础设施**，是实现锁和同步器的框架。`ReentrantLock`、`CountDownLatch`、`Semaphore`、`ReentrantReadWriteLock` 等底层的实现都基于 AQS。

AQS 在 Java 8 中新增了新的 `getWaitQueueLength` 优化和时获取方法的增强。

AQS 的核心概念：
- **状态**：通过 `volatile int state` 表示同步状态
- **CLH 队列**：FIFO 等待队列（虚拟双向队列）
- **独占/共享**：支持独占模式和共享模式
- **条件队列**：内部 ConditionObject 实现等待/通知

## 方法

构造方法：
```java
protected AbstractQueuedSynchronizer()
```

常用 protected 方法：
- `getState()` / `setState(int)` / `compareAndSetState(int, int)` — 状态管理
- `acquire(int)` / `release(int)` — 独占获取/释放
- `acquireShared(int)` / `releaseShared(int)` — 共享获取/释放
- `tryAcquire(int)` / `tryRelease(int)` — 独占尝试获取/释放
- `tryAcquireShared(int)` / `tryReleaseShared(int)` — 共享尝试获取/释放

## 测试

### 实现简单二元门

- 描述: 使用 AQS 实现简单的二元门（一次只允许一个线程通过）
- 断言: 互斥访问

```java
// 方法体开始
System.out.println("=== AQS 二元门 ===");
class SimpleGate extends AbstractQueuedSynchronizer {
    public void lock() { acquire(1); }
    public void unlock() { release(1); }
    protected boolean tryAcquire(int acquires) {
        return compareAndSetState(0, 1);
    }
    protected boolean tryRelease(int releases) {
        setState(0);
        return true;
    }
}
SimpleGate gate = new SimpleGate();
int[] counter = {0};
int THREADS = 10;
Thread[] threads = new Thread[THREADS];
for (int i = 0; i < THREADS; i++) {
    threads[i] = new Thread(() -> {
        gate.lock();
        try { counter[0]++; }
        finally { gate.unlock(); }
    });
    threads[i].start();
}
for (Thread t : threads) t.join();
assertEquals(THREADS, counter[0]);
System.out.println("AQS 门控计数: " + counter[0]);
System.out.println("=== 测试通过 ===");
// 方法体结束
```
