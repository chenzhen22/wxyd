---
name: StampedLock
package: java.util.concurrent.locks
order: 33
---

## 介绍

`StampedLock` 是 Java 8 引入的读写锁增强实现，提供了三种锁模式：写锁、读锁和乐观读。与 `ReentrantReadWriteLock` 不同，`StampedLock` 不可重入，且锁的获取返回一个 long 类型的 stamp 值，释放锁时需要传入该 stamp。

乐观读（Optimistic Read）模式是一种完全无锁的读操作，仅在读取后通过 `validate(stamp)` 验证期间是否发生过写操作，非常适合读多写少的场景。

常见用途：
- 高性能读写并发控制
- 读多写少场景下的无锁读操作
- 替代 `ReentrantReadWriteLock` 获得更高吞吐量

## 方法

### writeLock

```java
public long writeLock()
```

获取写锁（独占锁），阻塞直到获取成功。

- **返回**: `long` — 可用于解锁的 stamp 值

### readLock

```java
public long readLock()
```

获取读锁（共享锁），阻塞直到获取成功。

- **返回**: `long` — 可用于解锁的 stamp 值

### tryWriteLock

```java
public long tryWriteLock()
```

尝试获取写锁，如果不可用立即返回 0。

- **返回**: `long` — 成功获取返回 stamp，否则返回 0

### tryReadLock

```java
public long tryReadLock()
```

尝试获取读锁，如果不可用立即返回 0。

- **返回**: `long` — 成功获取返回 stamp，否则返回 0

### tryOptimisticRead

```java
public long tryOptimisticRead()
```

尝试获取乐观读 stamp，如果当前没有写锁被持有则返回非零 stamp。不阻塞，也不保证数据一致性，需要通过 `validate(stamp)` 验证。

- **返回**: `long` — 可用时返回 stamp，否则返回 0

### validate

```java
public boolean validate(long stamp)
```

验证给定的 stamp 在乐观读后是否有效（即没有发生过写操作）。

- **参数**: `stamp` — 乐观读获取的 stamp
- **返回**: `boolean` — 如果获取 stamp 后没有写操作返回 true

### unlockWrite

```java
public void unlockWrite(long stamp)
```

释放写锁。

- **参数**: `stamp` — 写锁获取时返回的 stamp
- **异常**: `IllegalMonitorStateException` — 如果 stamp 不匹配

### unlockRead

```java
public void unlockRead(long stamp)
```

释放读锁。

- **参数**: `stamp` — 读锁获取时返回的 stamp
- **异常**: `IllegalMonitorStateException` — 如果 stamp 不匹配

### unlock

```java
public void unlock(long stamp)
```

释放锁，自动识别锁类型（读锁或写锁）。

- **参数**: `stamp` — 锁获取时返回的 stamp
- **异常**: `IllegalMonitorStateException` — 如果 stamp 不匹配

### asReadLock

```java
public Lock asReadLock()
```

返回此 `StampedLock` 的读锁视图（`Lock` 接口）。

- **返回**: `Lock` — 读锁视图

### asWriteLock

```java
public Lock asWriteLock()
```

返回此 `StampedLock` 的写锁视图（`Lock` 接口）。

- **返回**: `Lock` — 写锁视图

## 测试

### writeLock

- 描述: 测试 writeLock 获取和释放写锁
- 断言: 写锁期间其他线程无法获取写锁

```java
// 方法体开始
System.out.println("=== writeLock ===");
StampedLock lock = new StampedLock();
long stamp = lock.writeLock();
assertTrue(stamp != 0);
long stamp2 = lock.tryWriteLock();
assertEquals(0, stamp2);
lock.unlockWrite(stamp);
stamp2 = lock.tryWriteLock();
assertTrue(stamp2 != 0);
lock.unlockWrite(stamp2);
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### readLock

- 描述: 测试 readLock 获取和释放读锁
- 断言: 读锁之间不互斥，写锁与读锁互斥

```java
// 方法体开始
System.out.println("=== readLock ===");
StampedLock lock = new StampedLock();
long s1 = lock.readLock();
assertTrue(s1 != 0);
long s2 = lock.readLock();
assertTrue(s2 != 0);
lock.unlockRead(s1);
lock.unlockRead(s2);
long ws = lock.writeLock();
long rs = lock.tryReadLock();
assertEquals(0, rs);
lock.unlockWrite(ws);
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### tryWriteLock

- 描述: 测试 tryWriteLock 非阻塞获取写锁
- 断言: 可用时返回非零，不可用时返回 0

```java
// 方法体开始
System.out.println("=== tryWriteLock ===");
StampedLock lock = new StampedLock();
long s1 = lock.tryWriteLock();
assertTrue(s1 != 0);
long s2 = lock.tryWriteLock();
assertEquals(0, s2);
lock.unlock(s1);
long s3 = lock.tryWriteLock();
assertTrue(s3 != 0);
lock.unlockWrite(s3);
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### tryOptimisticRead

- 描述: 测试 tryOptimisticRead 乐观读
- 断言: 无写锁时返回非零 stamp，有写锁时返回 0

```java
// 方法体开始
System.out.println("=== tryOptimisticRead ===");
StampedLock lock = new StampedLock();
long stamp = lock.tryOptimisticRead();
assertTrue(stamp != 0);
long ws = lock.writeLock();
long stamp2 = lock.tryOptimisticRead();
assertEquals(0, stamp2);
lock.unlockWrite(ws);
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### validate

- 描述: 测试 validate 方法验证乐观读结果
- 断言: 乐观读后无写操作则有效，有写操作则无效

```java
// 方法体开始
System.out.println("=== validate ===");
StampedLock lock = new StampedLock();
long stamp = lock.tryOptimisticRead();
assertTrue(lock.validate(stamp));
long ws = lock.writeLock();
assertFalse(lock.validate(stamp));
lock.unlockWrite(ws);
stamp = lock.tryOptimisticRead();
assertTrue(lock.validate(stamp));
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### tryReadLock

- 描述: 测试 tryReadLock 非阻塞获取读锁
- 断言: 可用时返回非零，写锁持有时返回 0

```java
// 方法体开始
System.out.println("=== tryReadLock ===");
StampedLock lock = new StampedLock();
long s1 = lock.tryReadLock();
assertTrue(s1 != 0);
lock.unlockRead(s1);
StampedLock lock2 = new StampedLock();
long ws = lock2.writeLock();
long s2 = lock2.tryReadLock();
assertEquals(0, s2);
lock2.unlockWrite(ws);
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### unlock

- 描述: 测试 unlock 通用释放锁方法
- 断言: unlock 能正确释放读锁和写锁

```java
// 方法体开始
System.out.println("=== unlock ===");
StampedLock lock = new StampedLock();
long ws = lock.writeLock();
lock.unlock(ws);
long ws2 = lock.writeLock();
assertTrue(ws2 != 0);
lock.unlock(ws2);
long rs = lock.readLock();
lock.unlock(rs);
long rs2 = lock.readLock();
assertTrue(rs2 != 0);
lock.unlockRead(rs2);
System.out.println("=== 测试通过 ===");
// 方法体结束
```


### asReadLock

- 描述: 转换为 ReadLock 视图
- 断言: asReadLock 返回的 Lock 不为 null

```java
// 方法体开始
System.out.println("=== asReadLock ===");
StampedLock sl = new StampedLock();
Lock lock = sl.asReadLock();
assertNotNull(lock);
System.out.println("asReadLock: " + lock);
System.out.println("=== 测试通过 ===");
// 方法体结束
```


### asWriteLock

- 描述: 转换为 WriteLock 视图
- 断言: asWriteLock 返回的 Lock 不为 null

```java
// 方法体开始
System.out.println("=== asWriteLock ===");
StampedLock sl = new StampedLock();
Lock lock = sl.asWriteLock();
assertNotNull(lock);
lock.lock();
lock.unlock();
System.out.println("asWriteLock: " + lock);
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### conversions

- 描述: 测试 asReadLock 和 asWriteLock 返回的 Lock 视图
- 断言: 视图可用且正确委托到 StampedLock

```java
// 方法体开始
System.out.println("=== conversions ===");
StampedLock sl = new StampedLock();
Lock readLock = sl.asReadLock();
Lock writeLock = sl.asWriteLock();
assertNotNull(readLock);
assertNotNull(writeLock);
writeLock.lock();
long stamp = sl.tryOptimisticRead();
assertEquals(0, stamp);
writeLock.unlock();
stamp = sl.tryOptimisticRead();
assertTrue(stamp != 0);
System.out.println("=== 测试通过 ===");
// 方法体结束
```
