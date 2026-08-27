---
name: LockSupport
package: java.util.concurrent.locks
order: 149
---

## 介绍

`java.util.concurrent.locks.LockSupport` 是**线程阻塞/唤醒工具类**，是 AQS 和所有锁实现的底层基础。它基于 `Unsafe.park`/`unpark` 实现。

LockSupport 的核心特点：
- **基于线程**：与信号量不同，操作是基于线程的许可
- **不会累积**：许可最多只有一个
- **精确唤醒**：`unpark` 可以精确唤醒指定线程
- **响应中断**：`park` 方法会响应中断

## 方法

### park / unpark

```java
public static void park()
public static void unpark(Thread thread)
```

阻塞当前线程 / 唤醒指定线程。

### parkNanos

```java
public static void parkNanos(long nanos)
```

限时阻塞。

### park(Object)

```java
public static void park(Object blocker)
```

带阻塞者对象的 park，方便线程 dump 分析。

## 测试

### park / unpark

- 描述: 使用 park/unpark 控制线程状态
- 断言: 线程正确被唤醒

```java
// 方法体开始
System.out.println("=== park/unpark ===");
Thread t = new Thread(() -> {
    try { Thread.sleep(200); } catch (Exception e) {}
    LockSupport.park();
});
t.start();
Thread.sleep(300);
LockSupport.unpark(t);
t.join();
assertFalse(t.isAlive());
System.out.println("park/unpark 线程控制成功");
System.out.println("=== 测试通过 ===");
// 方法体结束
```
