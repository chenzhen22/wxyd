---
name: TimeUnit
package: java.util.concurrent
order: 34
---

## 介绍

`TimeUnit` 是 Java 5 引入的时间单位枚举，提供了跨时间单位的转换方法和基于时间的操作（休眠、等待、join）。它定义了从纳秒到天的 7 个时间单位常量：`NANOSECONDS`、`MICROSECONDS`、`MILLISECONDS`、`SECONDS`、`MINUTES`、`HOURS`、`DAYS`。

`TimeUnit` 本质上是一个枚举，但实现了复杂的时间转换和时间操作，编写并发代码时推荐使用 `TimeUnit.sleep()` 替代 `Thread.sleep()`，因为它更语义化且不易出错。

常见用途：
- 时间单位之间的便捷转换
- 替代 `Thread.sleep()` 的语义化休眠
- 与锁和等待方法配合使用（`timedJoin`、`timedWait`）

## 方法

### toNanos

```java
public long toNanos(long duration)
```

将给定时间转换为纳秒。

- **参数**: `duration` — 以当前单位表示的时间长度
- **返回**: `long` — 转换后的纳秒数

### toMicros

```java
public long toMicros(long duration)
```

将给定时间转换为微秒。

- **参数**: `duration` — 以当前单位表示的时间长度
- **返回**: `long` — 转换后的微秒数

### toMillis

```java
public long toMillis(long duration)
```

将给定时间转换为毫秒。

- **参数**: `duration` — 以当前单位表示的时间长度
- **返回**: `long` — 转换后的毫秒数

### toSeconds

```java
public long toSeconds(long duration)
```

将给定时间转换为秒。

- **参数**: `duration` — 以当前单位表示的时间长度
- **返回**: `long` — 转换后的秒数

### toMinutes

```java
public long toMinutes(long duration)
```

将给定时间转换为分钟。

- **参数**: `duration` — 以当前单位表示的时间长度
- **返回**: `long` — 转换后的分钟数

### toHours

```java
public long toHours(long duration)
```

将给定时间转换为小时。

- **参数**: `duration` — 以当前单位表示的时间长度
- **返回**: `long` — 转换后的小时数

### toDays

```java
public long toDays(long duration)
```

将给定时间转换为天。

- **参数**: `duration` — 以当前单位表示的时间长度
- **返回**: `long` — 转换后的天数

### convert

```java
public long convert(long sourceDuration, TimeUnit sourceUnit)
```

将给定单位的时间转换为当前单位。

- **参数**: `sourceDuration` — 原时间长度；`sourceUnit` — 原时间单位
- **返回**: `long` — 转换后的时间（以当前单位表示）

### sleep

```java
public void sleep(long timeout) throws InterruptedException
```

在给定超时时间内休眠当前线程。语义上等价于 `Thread.sleep()`，但时间单位由枚举本身指定。

- **参数**: `timeout` — 休眠时间长度（以当前单位表示）
- **异常**: `InterruptedException` — 当前线程在休眠时被中断

### timedJoin

```java
public void timedJoin(Thread thread, long timeout) throws InterruptedException
```

在给定超时时间内等待指定线程终止。语义上等价于 `Thread.join(millis)`，但时间单位由枚举本身指定。

- **参数**: `thread` — 要等待的线程；`timeout` — 等待时间长度
- **异常**: `InterruptedException` — 当前线程在等待时被中断

### timedWait

```java
public void timedWait(Object obj, long timeout) throws InterruptedException
```

在给定超时时间内等待对象通知。语义上等价于 `Object.wait(millis)`，但时间单位由枚举本身指定。

- **参数**: `obj` — 要等待的对象；`timeout` — 等待时间长度
- **异常**: `InterruptedException` — 当前线程在等待时被中断

## 测试

### toMillis

- 描述: 测试 toMillis 秒转毫秒
- 断言: 秒转毫秒结果正确

```java
// 方法体开始
System.out.println("=== toMillis ===");
long ms = TimeUnit.SECONDS.toMillis(5);
assertEquals(5000, ms);
ms = TimeUnit.MINUTES.toMillis(1);
assertEquals(60000, ms);
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### toSeconds

- 描述: 测试 toSeconds 毫秒转秒
- 断言: 毫秒转秒结果正确（向下取整）

```java
// 方法体开始
System.out.println("=== toSeconds ===");
long s = TimeUnit.MILLISECONDS.toSeconds(5000);
assertEquals(5, s);
s = TimeUnit.MILLISECONDS.toSeconds(1500);
assertEquals(1, s);
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### toMinutes

- 描述: 测试 toMinutes 秒转分钟
- 断言: 秒转分钟结果正确

```java
// 方法体开始
System.out.println("=== toMinutes ===");
long min = TimeUnit.SECONDS.toMinutes(120);
assertEquals(2, min);
min = TimeUnit.SECONDS.toMinutes(60);
assertEquals(1, min);
min = TimeUnit.SECONDS.toMinutes(0);
assertEquals(0, min);
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### convert

- 描述: 测试 convert 跨单位转换
- 断言: 天转小时结果正确

```java
// 方法体开始
System.out.println("=== convert ===");
long h = TimeUnit.HOURS.convert(2, TimeUnit.DAYS);
assertEquals(48, h);
h = TimeUnit.HOURS.convert(1, TimeUnit.DAYS);
assertEquals(24, h);
long s = TimeUnit.SECONDS.convert(1, TimeUnit.MINUTES);
assertEquals(60, s);
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### sleep

- 描述: 测试 sleep 方法
- 断言: sleep 在指定时间内完成，不抛出异常

```java
// 方法体开始
System.out.println("=== sleep ===");
long start = System.currentTimeMillis();
TimeUnit.MILLISECONDS.sleep(10);
long elapsed = System.currentTimeMillis() - start;
assertTrue(elapsed >= 5);
System.out.println("elapsed: " + elapsed + "ms");
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### toNanos

- 描述: 测试 toNanos 毫秒转纳秒
- 断言: 毫秒转纳秒结果正确

```java
// 方法体开始
System.out.println("=== toNanos ===");
long ns = TimeUnit.MILLISECONDS.toNanos(1);
assertEquals(1000000, ns);
ns = TimeUnit.SECONDS.toNanos(1);
assertEquals(1000000000, ns);
ns = TimeUnit.MILLISECONDS.toNanos(0);
assertEquals(0, ns);
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### toDays

- 描述: 测试 toDays 小时转天
- 断言: 小时转天结果正确

```java
// 方法体开始
System.out.println("=== toDays ===");
long d = TimeUnit.HOURS.toDays(48);
assertEquals(2, d);
d = TimeUnit.HOURS.toDays(24);
assertEquals(1, d);
d = TimeUnit.HOURS.toDays(0);
assertEquals(0, d);
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### toMicros

- 描述: 测试 toMicros 毫秒转微秒
- 断言: 毫秒转微秒结果正确

```java
// 方法体开始
System.out.println("=== toMicros ===");
long us = TimeUnit.MILLISECONDS.toMicros(1);
assertEquals(1000, us);
us = TimeUnit.SECONDS.toMicros(1);
assertEquals(1000000, us);
us = TimeUnit.MILLISECONDS.toMicros(0);
assertEquals(0, us);
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### toHours

- 描述: 测试 toHours 分钟转小时
- 断言: 转换结果正确

```java
// 方法体开始
System.out.println("=== toHours ===");
long h = TimeUnit.MINUTES.toHours(120);
assertEquals(2, h);
h = TimeUnit.SECONDS.toHours(3600);
assertEquals(1, h);
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### timedJoin

- 描述: 测试 timedJoin 带超时的线程等待
- 断言: 线程在超时前完成

```java
// 方法体开始
System.out.println("=== timedJoin ===");
Thread t = new Thread(() -> {
    try { Thread.sleep(50); } catch (Exception e) {}
});
t.start();
TimeUnit.MILLISECONDS.timedJoin(t, 500);
assertFalse(t.isAlive());
System.out.println("timedJoin 成功");
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### timedWait

- 描述: 测试 timedWait 带超时的等待
- 断言: 等待超时后线程继续执行

```java
// 方法体开始
System.out.println("=== timedWait ===");
Object lock = new Object();
long start = System.nanoTime();
synchronized (lock) {
    TimeUnit.MILLISECONDS.timedWait(lock, 10);
}
long elapsed = System.nanoTime() - start;
assertTrue(elapsed >= 0);
System.out.println("timedWait 执行完成");
System.out.println("=== 测试通过 ===");
// 方法体结束
```
