---
name: ReentrantReadWriteLock
package: java.util.concurrent.locks
order: 42
---

## 介绍

`java.util.concurrent.locks.ReentrantReadWriteLock` 是 Java 5 引入的**读写锁**实现，维护一对相关的锁：一个用于只读操作（读锁），一个用于写入操作（写锁）。读锁可以被多个线程同时持有，写锁是独占的。

核心规则：
- **读锁共享**：多条线程可以同时持有读锁
- **写锁独占**：写锁被持有时，其他线程不能获取读锁或写锁
- **读写互斥**：有线程持有读锁时，写锁会被阻塞；反之亦然
- **可重入**：支持锁降级（写锁→读锁）

## 方法

### ReentrantReadWriteLock()

```java
public ReentrantReadWriteLock()
```

创建非公平的读写锁。

### ReentrantReadWriteLock(boolean)

```java
public ReentrantReadWriteLock(boolean fair)
```

创建指定公平性的读写锁。

### readLock

```java
public ReentrantReadWriteLock.ReadLock readLock()
```

返回读锁。

- **返回**: `ReentrantReadWriteLock.ReadLock`

### writeLock

```java
public ReentrantReadWriteLock.WriteLock writeLock()
```

返回写锁。

- **返回**: `ReentrantReadWriteLock.WriteLock`

### WriteLock.lock

```java
public void lock()
```

获取写锁（独占）。

### WriteLock.unlock

```java
public void unlock()
```

释放写锁。

### ReadLock.lock

```java
public void lock()
```

获取读锁（共享）。

### ReadLock.unlock

```java
public void unlock()
```

释放读锁。

### getReadLockCount

```java
public int getReadLockCount()
```

返回读锁被持有的次数。

- **返回**: `int`

### isWriteLocked

```java
public boolean isWriteLocked()
```

判断写锁是否被持有。

- **返回**: `boolean`

### getQueueLength

```java
public int getQueueLength()
```

返回等待获取锁的线程数。

- **返回**: `int`

## 测试

### readWriteLock

- 描述: 创建读写锁
- 断言: 读锁和写锁不为 null

```java
// 方法体开始
System.out.println("=== readWriteLock ===");
ReentrantReadWriteLock rw = new ReentrantReadWriteLock();
assertNotNull(rw.readLock());
assertNotNull(rw.writeLock());
System.out.println("读锁: " + rw.readLock() + ", 写锁: " + rw.writeLock());
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### writeLock

- 描述: 获取写锁
- 断言: 写锁独占

```java
// 方法体开始
System.out.println("=== writeLock ===");
ReentrantReadWriteLock rw = new ReentrantReadWriteLock();
rw.writeLock().lock();
assertTrue(rw.isWriteLocked());
assertTrue(rw.isWriteLocked());
rw.writeLock().unlock();
assertFalse(rw.isWriteLocked());
System.out.println("写锁已获取并释放");
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### readLock

- 描述: 获取读锁（共享）
- 断言: 读锁可重入

```java
// 方法体开始
System.out.println("=== readLock ===");
ReentrantReadWriteLock rw = new ReentrantReadWriteLock();
rw.readLock().lock();
assertEquals(1, rw.getReadLockCount());
assertFalse(rw.isWriteLocked());
rw.readLock().unlock();
assertEquals(0, rw.getReadLockCount());
System.out.println("读锁已获取并释放");
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### fair

- 描述: 公平锁
- 断言: 公平锁的 isFair 返回 true

```java
// 方法体开始
System.out.println("=== fair ===");
ReentrantReadWriteLock fair = new ReentrantReadWriteLock(true);
assertTrue(fair.isFair());
ReentrantReadWriteLock unfair = new ReentrantReadWriteLock(false);
assertFalse(unfair.isFair());
System.out.println("公平锁: " + fair.isFair());
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### getQueueLength

- 描述: 查询等待线程数
- 断言: 无竞争时返回 0

```java
// 方法体开始
System.out.println("=== getQueueLength ===");
ReentrantReadWriteLock rw = new ReentrantReadWriteLock();
assertEquals(0, rw.getQueueLength());
System.out.println("等待队列长度: " + rw.getQueueLength());
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### ReentrantReadWriteLock

- 描述: 创建读写锁
- 断言: 对象不为 null

```java
// 方法体开始
System.out.println("=== ReentrantReadWriteLock ===");
ReentrantReadWriteLock rw = new ReentrantReadWriteLock();
assertNotNull(rw);
System.out.println("读写锁: " + rw);
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### getReadLockCount

- 描述: 获取读锁持有次数
- 断言: 无锁时为 0

```java
// 方法体开始
System.out.println("=== getReadLockCount ===");
ReentrantReadWriteLock rw = new ReentrantReadWriteLock();
assertEquals(0, rw.getReadLockCount());
rw.readLock().lock();
assertEquals(1, rw.getReadLockCount());
rw.readLock().unlock();
assertEquals(0, rw.getReadLockCount());
System.out.println("读锁计数: " + rw.getReadLockCount());
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### isWriteLocked

- 描述: 判断写锁是否被持有
- 断言: 写锁持有后返回 true

```java
// 方法体开始
System.out.println("=== isWriteLocked ===");
ReentrantReadWriteLock rw = new ReentrantReadWriteLock();
assertFalse(rw.isWriteLocked());
rw.writeLock().lock();
assertTrue(rw.isWriteLocked());
rw.writeLock().unlock();
assertFalse(rw.isWriteLocked());
System.out.println("写锁状态: " + rw.isWriteLocked());
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### WriteLock.lock

- 描述: 获取写锁
- 断言: 获取写锁后 isWriteLocked 为 true

```java
// 方法体开始
System.out.println("=== WriteLock.lock ===");
ReentrantReadWriteLock rw = new ReentrantReadWriteLock();
rw.writeLock().lock();
assertTrue(rw.isWriteLocked());
rw.writeLock().unlock();
System.out.println("写锁已获取并释放");
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### WriteLock.unlock

- 描述: 释放写锁
- 断言: 释放后 isWriteLocked 为 false

```java
// 方法体开始
System.out.println("=== WriteLock.unlock ===");
ReentrantReadWriteLock rw = new ReentrantReadWriteLock();
rw.writeLock().lock();
rw.writeLock().unlock();
assertFalse(rw.isWriteLocked());
System.out.println("写锁已释放");
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### ReadLock.lock

- 描述: 获取读锁
- 断言: 获取读锁后 getReadLockCount 增加

```java
// 方法体开始
System.out.println("=== ReadLock.lock ===");
ReentrantReadWriteLock rw = new ReentrantReadWriteLock();
rw.readLock().lock();
assertTrue(rw.getReadLockCount() > 0);
rw.readLock().unlock();
System.out.println("读锁已获取并释放");
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### ReadLock.unlock

- 描述: 释放读锁
- 断言: 释放后 getReadLockCount 为 0

```java
// 方法体开始
System.out.println("=== ReadLock.unlock ===");
ReentrantReadWriteLock rw = new ReentrantReadWriteLock();
rw.readLock().lock();
rw.readLock().unlock();
assertEquals(0, rw.getReadLockCount());
System.out.println("读锁已释放");
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### ReentrantReadWriteLock(boolean)

- 描述: 创建公平读写锁
- 断言: 公平锁 isFair 为 true

```java
// 方法体开始
System.out.println("=== ReentrantReadWriteLock(boolean) ===");
ReentrantReadWriteLock fair = new ReentrantReadWriteLock(true);
assertTrue(fair.isFair());
ReentrantReadWriteLock unfair = new ReentrantReadWriteLock(false);
assertFalse(unfair.isFair());
System.out.println("公平锁: " + fair.isFair() + ", 非公平锁: " + unfair.isFair());
System.out.println("=== 测试通过 ===");
// 方法体结束
```

