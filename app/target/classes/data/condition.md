---
name: Condition
package: java.util.concurrent.locks
order: 327
---

## 介绍

`java.util.concurrent.locks.Condition` 是**条件变量**接口，将 Object 的 `wait/notify` 替换为更强大的条件等待机制，与 `ReentrantLock` 配合使用。

## 方法

### await

```java
public void await() throws InterruptedException
```

释放锁并等待条件满足。

### signal / signalAll

```java
public void signal()
public void signalAll()
```

唤醒等待线程。

### awaitNanos / awaitUntil / awaitUninterruptibly

带超时/不可中断的等待。

## 测试

- 描述: 使用 Condition 实现生产者-消费者
- 断言: 数据正确传递

```java
// 方法体开始
System.out.println("=== Condition ===");
ReentrantLock lock = new ReentrantLock();
Condition notEmpty = lock.newCondition();
Condition notFull = lock.newCondition();
List<String> buffer = new ArrayList<>();
int MAX = 5;
Thread producer = new Thread(() -> {
    lock.lock();
    try {
        while (buffer.size() >= MAX) notFull.await();
        buffer.add("data");
        notEmpty.signal();
    } catch (InterruptedException e) {} finally { lock.unlock(); }
});
Thread consumer = new Thread(() -> {
    lock.lock();
    try {
        while (buffer.isEmpty()) notEmpty.await();
        String data = buffer.remove(0);
        notFull.signal();
    } catch (InterruptedException e) {} finally { lock.unlock(); }
});
producer.start(); producer.join();
assertFalse(buffer.isEmpty());
consumer.start(); consumer.join();
assertTrue(buffer.isEmpty());
System.out.println("Condition 生产者-消费者测试通过");
System.out.println("=== 测试通过 ===");
// 方法体结束
```
