---
name: ReentrantLock
package: java.util.concurrent.locks
order: 2
---

## 介绍

`ReentrantLock` 是 Java 5 引入的一个可重入的互斥锁，它与 `synchronized` 关键字具有相同的基本行为和语义，但提供了更强大的功能。

与 `synchronized` 相比，`ReentrantLock` 的优势：

- **可中断锁**：通过 `lockInterruptibly()` 支持响应中断
- **超时获取**：通过 `tryLock(long, TimeUnit)` 支持超时等待
- **非阻塞获取**：通过 `tryLock()` 支持非阻塞尝试
- **公平性选择**：支持公平锁（先等待先获取）
- **条件变量**：通过 `newCondition()` 支持多个 Condition
- **可见性检查**：通过 `getQueuedThreads()` 等获取等待线程信息

`ReentrantLock` 是"可重入"的，意味着同一个线程可以多次获取同一把锁，而不会发生死锁。每次获取后必须对应一次释放。

## 方法

### lock

```java
public void lock()
```

获取锁。如果锁未被其他线程占用，则当前线程立即获取并返回；如果锁已被其他线程占用，则当前线程进入等待状态直到获取到锁。

- **说明**: 可重入，同一线程可多次调用

### unlock

```java
public void unlock()
```

释放锁。通常放在 `finally` 块中以确保释放。

- **说明**: 调用次数必须与 `lock()` 调用次数匹配

### tryLock

```java
public boolean tryLock()
```

非阻塞地尝试获取锁。如果锁可用则立即获取并返回 `true`，否则立即返回 `false`。

- **返回**: `boolean` — 是否成功获取锁

### tryLock(timeout, unit)

```java
public boolean tryLock(long timeout, TimeUnit unit) throws InterruptedException
```

在指定时间内等待获取锁。超时后返回 `false`。可响应中断。

- **参数**: `timeout` — 超时时间；`unit` — 时间单位
- **返回**: `boolean` — 是否成功获取锁
- **异常**: `InterruptedException` — 等待时被中断

### lockInterruptibly

```java
public void lockInterruptibly() throws InterruptedException
```

可中断地获取锁。与 `lock()` 的区别在于，等待过程中可以响应中断。

- **异常**: `InterruptedException` — 等待时被中断

### newCondition

```java
public Condition newCondition()
```

返回绑定到此锁的 Condition 实例。`Condition` 提供了类似 `Object.wait()` / `notify()` 的等待/通知机制，但更灵活。

- **返回**: `Condition` — 与该锁关联的条件变量

### getHoldCount

```java
public int getHoldCount()
```

查询当前线程持有该锁的次数（重入次数）。

- **返回**: `int` — 当前线程的持有计数

### isHeldByCurrentThread

```java
public boolean isHeldByCurrentThread()
```

查询当前线程是否持有该锁。

- **返回**: `boolean` — 当前线程是否持有该锁

### isLocked

```java
public boolean isLocked()
```

查询该锁是否被任意线程持有。此方法用于监控，不应用于同步控制。

- **返回**: `boolean` — 该锁是否被持有

### isFair

```java
public final boolean isFair()
```

判断该锁是否为公平锁。

- **返回**: `boolean` — 如果锁是公平的返回 true

### getQueueLength

```java
public final int getQueueLength()
```

返回正在等待获取该锁的线程数。

- **返回**: `int` — 等待队列长度

### getWaitQueueLength

```java
public int getWaitQueueLength(Condition condition)
```

返回在给定 Condition 上等待的线程数。

- **参数**: `condition` — 条件变量
- **返回**: `int` — 在条件上等待的线程数

### hasQueuedThread

```java
public final boolean hasQueuedThread(Thread thread)
```

查询指定线程是否在等待获取该锁。

- **参数**: `thread` — 指定线程
- **返回**: `boolean` — 该线程是否在等待队列中

### hasQueuedThreads

```java
public final boolean hasQueuedThreads()
```

查询是否有线程在等待获取该锁。

- **返回**: `boolean` — 是否有等待线程

### hasWaiters

```java
public boolean hasWaiters(Condition condition)
```

查询是否有线程在给定 Condition 上等待。

- **参数**: `condition` — 条件变量
- **返回**: `boolean` — 是否有条件等待者

### getQueuedThreads

```java
public final Collection<Thread> getQueuedThreads()
```

返回正在等待获取该锁的线程集合。

- **返回**: `Collection<Thread>` — 等待线程集合

### getWaitingThreads

```java
public Collection<Thread> getWaitingThreads(Condition condition)
```

返回在给定 Condition 上等待的线程集合。

- **参数**: `condition` — 条件变量
- **返回**: `Collection<Thread>` — 等待线程集合

### toString

```java
public String toString()
```

返回锁的字符串表示，包括锁的当前状态（是否被锁定）。

- **返回**: `String`

## 测试

### lockAndUnlock

- 描述: 基本加锁/解锁
- 断言: 加锁后执行操作，结果正确

```java
// 方法体开始
System.out.println("=== lockAndUnlock ===");
ReentrantLock lock = new ReentrantLock();
List<String> list = new ArrayList<>();
System.out.println("获取锁...");
lock.lock();
try {
    list.add("item");
    System.out.println("已添加元素，列表大小: " + list.size());
    assertEquals(1, list.size());
    assertEquals("item", list.get(0));
} finally {
    lock.unlock();
    System.out.println("已释放锁");
}
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### newCondition

- 描述: 创建条件变量
- 断言: newCondition() 返回非 null 的 Condition

```java
// 方法体开始
System.out.println("=== newCondition ===");
ReentrantLock lock = new ReentrantLock();
Condition condition = lock.newCondition();
System.out.println("已创建 Condition: " + condition);
assertNotNull(condition);
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### isFair

- 描述: 判断锁是否为公平锁
- 断言: 公平锁的 isFair() 返回 true，非公平锁返回 false

```java
// 方法体开始
System.out.println("=== isFair ===");
ReentrantLock fairLock = new ReentrantLock(true);
assertTrue(fairLock.isFair());
ReentrantLock unfairLock = new ReentrantLock(false);
assertFalse(unfairLock.isFair());
System.out.println("公平锁: " + fairLock.isFair() + ", 非公平锁: " + unfairLock.isFair());
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### lock

- 描述: 获取锁
- 断言: 加锁后可以正常持有锁

```java
// 方法体开始
System.out.println("=== lock ===");
ReentrantLock lock = new ReentrantLock();
lock.lock();
assertTrue(lock.isHeldByCurrentThread());
System.out.println("已获取锁，持有状态: " + lock.isHeldByCurrentThread());
lock.unlock();
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### unlock

- 描述: 释放锁
- 断言: 解锁后 isHeldByCurrentThread 返回 false

```java
// 方法体开始
System.out.println("=== unlock ===");
ReentrantLock lock = new ReentrantLock();
lock.lock();
assertTrue(lock.isHeldByCurrentThread());
lock.unlock();
assertFalse(lock.isHeldByCurrentThread());
System.out.println("释放锁后持有状态: " + lock.isHeldByCurrentThread());
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### tryLock

- 描述: 非阻塞尝试获取锁
- 断言: tryLock 成功获取锁时返回 true

```java
// 方法体开始
System.out.println("=== tryLock ===");
ReentrantLock lock = new ReentrantLock();
boolean acquired = lock.tryLock();
System.out.println("尝试获取锁: " + (acquired ? "成功" : "失败"));
assertTrue(acquired);
if (acquired) {
    lock.unlock();
    System.out.println("已释放锁");
}
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### reentrant

- 描述: 可重入特性
- 断言: 同一线程可多次获取锁，holdCount 递增

```java
// 方法体开始
System.out.println("=== reentrant ===");
ReentrantLock lock = new ReentrantLock();
System.out.println("第一次获取锁");
lock.lock();
System.out.println("第二次获取锁（可重入）");
lock.lock();
int count = lock.getHoldCount();
System.out.println("当前持有次数: " + count);
assertEquals(2, count);
lock.unlock();
System.out.println("释放一次，剩余持有: " + lock.getHoldCount());
lock.unlock();
assertEquals(0, lock.getHoldCount());
System.out.println("已完全释放锁");
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### isHeldByCurrentThread

- 描述: 当前线程是否持有锁
- 断言: 获取锁后 isHeldByCurrentThread 返回 true

```java
// 方法体开始
System.out.println("=== isHeldByCurrentThread ===");
ReentrantLock lock = new ReentrantLock();
assertFalse(lock.isHeldByCurrentThread());
System.out.println("尚未持有锁: " + lock.isHeldByCurrentThread());
lock.lock();
assertTrue(lock.isHeldByCurrentThread());
System.out.println("持有锁后: " + lock.isHeldByCurrentThread());
lock.unlock();
assertFalse(lock.isHeldByCurrentThread());
System.out.println("释放锁后: " + lock.isHeldByCurrentThread());
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### isLocked

- 描述: 查询锁状态
- 断言: 加锁后 isLocked 返回 true

```java
// 方法体开始
System.out.println("=== isLocked ===");
ReentrantLock lock = new ReentrantLock();
assertFalse(lock.isLocked());
lock.lock();
assertTrue(lock.isLocked());
System.out.println("锁已获取: " + lock.isLocked());
lock.unlock();
assertFalse(lock.isLocked());
System.out.println("锁已释放: " + lock.isLocked());
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### hasQueuedThreads

- 描述: 查询是否有线程等待锁
- 断言: 无竞争时 hasQueuedThreads 返回 false

```java
// 方法体开始
System.out.println("=== hasQueuedThreads ===");
ReentrantLock lock = new ReentrantLock();
assertFalse(lock.hasQueuedThreads());
System.out.println("是否有等待线程: " + lock.hasQueuedThreads());
assertEquals(0, lock.getQueueLength());
System.out.println("等待队列长度: " + lock.getQueueLength());
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### fairness

- 描述: 公平锁
- 断言: 公平锁的 isFair() 返回 true

```java
// 方法体开始
System.out.println("=== fairness ===");
ReentrantLock fairLock = new ReentrantLock(true);
boolean fair = fairLock.isFair();
System.out.println("是否为公平锁: " + (fair ? "是" : "否"));
assertTrue(fair);
ReentrantLock unfairLock = new ReentrantLock(false);
System.out.println("非公平锁默认: " + unfairLock.isFair());
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### condition

- 描述: 条件变量基本使用
- 断言: Condition 对象创建成功

```java
// 方法体开始
System.out.println("=== condition ===");
ReentrantLock lock1 = new ReentrantLock();
Condition condition = lock1.newCondition();
System.out.println("已创建 Condition 实例: " + condition);
assertNotNull(condition);
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### tryLockTimeout

- 描述: 带超时的锁获取
- 断言: 在超时时间内成功获取锁

```java
// 方法体开始
System.out.println("=== tryLockTimeout ===");
ReentrantLock lock = new ReentrantLock();
try {
    boolean acquired = lock.tryLock(100, TimeUnit.MILLISECONDS);
    System.out.println("在 100ms 内获取锁: " + (acquired ? "成功" : "失败"));
    assertTrue(acquired);
    if (acquired) {
        lock.unlock();
        System.out.println("已释放锁");
    }
} catch (InterruptedException e) {
    Thread.currentThread().interrupt();
    fail("不应被中断");
}
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### lockInterruptibly

- 描述: 可中断获取锁
- 断言: 正常获取锁后当前线程持有锁

```java
// 方法体开始
System.out.println("=== lockInterruptibly ===");
ReentrantLock lock = new ReentrantLock();
try {
    lock.lockInterruptibly();
    System.out.println("已通过 lockInterruptibly 获取锁");
    assertTrue(lock.isHeldByCurrentThread());
    lock.unlock();
    System.out.println("已释放锁");
} catch (InterruptedException e) {
    Thread.currentThread().interrupt();
    fail("不应被中断");
}
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### getHoldCount

- 描述: 查询当前线程持有锁的次数
- 断言: 未持有锁时返回 0，持有后返回正确次数

```java
// 方法体开始
System.out.println("=== getHoldCount ===");
ReentrantLock lock = new ReentrantLock();
assertEquals(0, lock.getHoldCount());
System.out.println("未持有锁时 holdCount: " + lock.getHoldCount());
lock.lock();
assertEquals(1, lock.getHoldCount());
System.out.println("持有一次后 holdCount: " + lock.getHoldCount());
lock.lock();
assertEquals(2, lock.getHoldCount());
System.out.println("重入一次后 holdCount: " + lock.getHoldCount());
lock.unlock();
lock.unlock();
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### getQueueLength

- 描述: 查询等待获取锁的线程数
- 断言: 无竞争时队列长度为 0

```java
// 方法体开始
System.out.println("=== getQueueLength ===");
ReentrantLock lock = new ReentrantLock();
assertEquals(0, lock.getQueueLength());
System.out.println("等待队列长度: " + lock.getQueueLength());
lock.lock();
assertEquals(0, lock.getQueueLength());
System.out.println("当前线程持有锁后队列长度: " + lock.getQueueLength());
lock.unlock();
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### getWaitQueueLength

- 描述: 查询在 Condition 上等待的线程数
- 断言: 无等待时返回 0

```java
// 方法体开始
System.out.println("=== getWaitQueueLength ===");
ReentrantLock lock = new ReentrantLock();
Condition condition = lock.newCondition();
lock.lock();
try {
    assertEquals(0, lock.getWaitQueueLength(condition));
    System.out.println("Condition 等待队列长度: " + lock.getWaitQueueLength(condition));
} finally {
    lock.unlock();
}
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### hasQueuedThread

- 描述: 查询指定线程是否在等待获取锁
- 断言: 当前线程不在等待队列中

```java
// 方法体开始
System.out.println("=== hasQueuedThread ===");
ReentrantLock lock = new ReentrantLock();
Thread current = Thread.currentThread();
assertFalse(lock.hasQueuedThread(current));
System.out.println("当前线程是否在等待队列: " + lock.hasQueuedThread(current));
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### hasWaiters

- 描述: 查询是否有线程在 Condition 上等待
- 断言: 无等待时返回 false

```java
// 方法体开始
System.out.println("=== hasWaiters ===");
ReentrantLock lock = new ReentrantLock();
Condition condition = lock.newCondition();
lock.lock();
try {
    assertFalse(lock.hasWaiters(condition));
    System.out.println("是否有线程在 Condition 上等待: " + lock.hasWaiters(condition));
} finally {
    lock.unlock();
}
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### getQueuedThreads

- 描述: 获取等待获取锁的线程集合
- 断言: 无竞争时集合为空

```java
// 方法体开始
System.out.println("=== getQueuedThreads ===");
class ExposedLock extends ReentrantLock {
    public Collection<Thread> getQueuedThreadsPublic() { return getQueuedThreads(); }
}
ExposedLock lock = new ExposedLock();
Collection<Thread> queuedThreads = lock.getQueuedThreadsPublic();
System.out.println("等待获取锁的线程数: " + queuedThreads.size());
assertTrue(queuedThreads.isEmpty());
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### getWaitingThreads

- 描述: 获取在 Condition 上等待的线程集合
- 断言: 无等待时集合为空

```java
// 方法体开始
System.out.println("=== getWaitingThreads ===");
class ExposedLock2 extends ReentrantLock {
    public Collection<Thread> getWaitingThreadsPublic(Condition condition) { return getWaitingThreads(condition); }
}
ExposedLock2 lock = new ExposedLock2();
Condition condition = lock.newCondition();
lock.lock();
try {
    Collection<Thread> waitingThreads = lock.getWaitingThreadsPublic(condition);
    System.out.println("Condition 上等待的线程数: " + waitingThreads.size());
    assertTrue(waitingThreads.isEmpty());
} finally {
    lock.unlock();
}
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### toString

- 描述: 锁的字符串表示
- 断言: toString 返回非空字符串且包含锁状态信息

```java
// 方法体开始
System.out.println("=== toString ===");
ReentrantLock lock = new ReentrantLock();
String str = lock.toString();
System.out.println("锁未持有时的 toString: " + str);
assertNotNull(str);
assertFalse(str.isEmpty());
lock.lock();
String lockedStr = lock.toString();
System.out.println("锁持有后的 toString: " + lockedStr);
assertNotNull(lockedStr);
assertFalse(lockedStr.isEmpty());
lock.unlock();
System.out.println("=== 测试通过 ===");
// 方法体结束
```
