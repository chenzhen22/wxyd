---
name: AtomicLong
package: java.util.concurrent.atomic
order: 38
---

## 介绍

`AtomicLong` 是 Java 5 引入的原子操作类，提供了对 `long` 值的原子更新操作，用于高并发场景下的计数、累加、序列号生成等。内部基于 CAS（Compare-And-Swap）机制实现，无需使用锁即可保证线程安全。

与 `AtomicInteger` 类似，`AtomicLong` 同样支持原子自增、自减、累加等操作，适用于需要 64 位原子计数器的场景。

## 方法

### AtomicLong()

```java
public AtomicLong()
```

创建一个初始值为 `0` 的 `AtomicLong`。

### AtomicLong(long)

```java
public AtomicLong(long initialValue)
```

创建一个指定初始值的 `AtomicLong`。

- **参数**: `initialValue` — 初始值

### get

```java
public final long get()
```

获取当前值。

- **返回**: `long` — 当前值

### set

```java
public final void set(long newValue)
```

设置为给定值。

- **参数**: `newValue` — 新值

### lazySet

```java
public final void lazySet(long newValue)
```

延迟设置为给定值。与 `set()` 的区别在于不保证立即可见，性能更高。

- **参数**: `newValue` — 新值

### getAndSet

```java
public final long getAndSet(long newValue)
```

原子地设置为给定值并返回旧值。

- **参数**: `newValue` — 新值
- **返回**: `long` — 旧值

### compareAndSet

```java
public final boolean compareAndSet(long expect, long update)
```

如果当前值等于期望值，则原子地设置为新值。CAS 操作的核心方法。

- **参数**: `expect` — 期望值；`update` — 新值
- **返回**: `boolean` — 如果设置成功则返回 true

### getAndIncrement

```java
public final long getAndIncrement()
```

原子地自增 1 并返回旧值。

- **返回**: `long` — 旧值

### incrementAndGet

```java
public final long incrementAndGet()
```

原子地自增 1 并返回新值。

- **返回**: `long` — 新值

### getAndDecrement

```java
public final long getAndDecrement()
```

原子地自减 1 并返回旧值。

- **返回**: `long` — 旧值

### decrementAndGet

```java
public final long decrementAndGet()
```

原子地自减 1 并返回新值。

- **返回**: `long` — 新值

### getAndAdd

```java
public final long getAndAdd(long delta)
```

原子地加上给定值并返回旧值。

- **参数**: `delta` — 要加的值
- **返回**: `long` — 旧值

### addAndGet

```java
public final long addAndGet(long delta)
```

原子地加上给定值并返回新值。

- **参数**: `delta` — 要加的值
- **返回**: `long` — 新值

### intValue

```java
public int intValue()
public long longValue()
public float floatValue()
public double doubleValue()
```

将 `AtomicLong` 的值转换为对应的数字类型。

- **返回**: 转换后的值

### toString

```java
public String toString()
```

返回当前值的字符串表示。

- **返回**: `String`

## 测试

### AtomicLong

- 描述: 测试两个构造方法
- 断言: 无参构造初始值为 0，有参构造初始值为指定值

```java
// 方法体开始
System.out.println("=== AtomicLong ===");
java.util.concurrent.atomic.AtomicLong al1 = new java.util.concurrent.atomic.AtomicLong();
System.out.println("无参构造默认值: " + al1.get());
assertEquals(0L, al1.get());
java.util.concurrent.atomic.AtomicLong al2 = new java.util.concurrent.atomic.AtomicLong(100L);
System.out.println("有参构造初始值: " + al2.get());
assertEquals(100L, al2.get());
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### get

- 描述: 测试 get 方法
- 断言: get 返回当前值

```java
// 方法体开始
System.out.println("=== get ===");
java.util.concurrent.atomic.AtomicLong al = new java.util.concurrent.atomic.AtomicLong(42L);
long val = al.get();
assertEquals(42L, val);
System.out.println("get: " + val);
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### set

- 描述: 测试 set 方法
- 断言: set 设置新值后 get 返回新值

```java
// 方法体开始
System.out.println("=== set ===");
java.util.concurrent.atomic.AtomicLong al = new java.util.concurrent.atomic.AtomicLong(0L);
al.set(100L);
assertEquals(100L, al.get());
System.out.println("set(100) 后: " + al.get());
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### getAndSet

- 描述: 测试 getAndSet 方法
- 断言: getAndSet 返回旧值并设置新值

```java
// 方法体开始
System.out.println("=== getAndSet ===");
java.util.concurrent.atomic.AtomicLong al = new java.util.concurrent.atomic.AtomicLong(10L);
long old = al.getAndSet(30L);
assertEquals(10L, old);
assertEquals(30L, al.get());
System.out.println("getAndSet(30) 旧值: " + old + ", 新值: " + al.get());
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### compareAndSet

- 描述: 测试 compareAndSet CAS 操作
- 断言: 期望值匹配时设置成功返回 true，不匹配时返回 false

```java
// 方法体开始
System.out.println("=== compareAndSet ===");
java.util.concurrent.atomic.AtomicLong al = new java.util.concurrent.atomic.AtomicLong(0L);
boolean success1 = al.compareAndSet(0L, 1L);
System.out.println("CAS(0,1) 结果: " + success1 + ", 当前值: " + al.get());
assertTrue(success1);
assertEquals(1L, al.get());
boolean success2 = al.compareAndSet(0L, 2L);
System.out.println("CAS(0,2) 结果: " + success2 + "（期望不匹配），当前值: " + al.get());
assertFalse(success2);
assertEquals(1L, al.get());
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### getAndIncrement

- 描述: 测试 getAndIncrement 方法
- 断言: getAndIncrement 返回旧值并自增

```java
// 方法体开始
System.out.println("=== getAndIncrement ===");
java.util.concurrent.atomic.AtomicLong al = new java.util.concurrent.atomic.AtomicLong(10L);
long old = al.getAndIncrement();
assertEquals(10L, old);
assertEquals(11L, al.get());
System.out.println("getAndIncrement 旧值: " + old + ", 新值: " + al.get());
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### incrementAndGet

- 描述: 测试 incrementAndGet 方法
- 断言: incrementAndGet 自增并返回新值

```java
// 方法体开始
System.out.println("=== incrementAndGet ===");
java.util.concurrent.atomic.AtomicLong al = new java.util.concurrent.atomic.AtomicLong(10L);
long val = al.incrementAndGet();
assertEquals(11L, val);
assertEquals(11L, al.get());
System.out.println("incrementAndGet 新值: " + val);
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### addAndGet

- 描述: 测试 addAndGet 方法
- 断言: addAndGet 加上给定值并返回新值

```java
// 方法体开始
System.out.println("=== addAndGet ===");
java.util.concurrent.atomic.AtomicLong al = new java.util.concurrent.atomic.AtomicLong(100L);
long sum = al.addAndGet(15L);
assertEquals(115L, sum);
assertEquals(115L, al.get());
System.out.println("addAndGet(15) 新值: " + sum);
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### toString

- 描述: 测试 toString 方法
- 断言: toString 返回当前值的字符串表示

```java
// 方法体开始
System.out.println("=== toString ===");
java.util.concurrent.atomic.AtomicLong al = new java.util.concurrent.atomic.AtomicLong(256L);
String str = al.toString();
System.out.println("toString: " + str);
assertEquals("256", str);
assertNotNull(str);
java.util.concurrent.atomic.AtomicLong al2 = new java.util.concurrent.atomic.AtomicLong(-99L);
System.out.println("负数 toString: " + al2.toString());
assertEquals("-99", al2.toString());
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### lazySet

- 描述: 延迟设置值
- 断言: lazySet 后 get 返回新值

```java
// 方法体开始
System.out.println("=== lazySet ===");
java.util.concurrent.atomic.AtomicLong al = new java.util.concurrent.atomic.AtomicLong(10);
al.lazySet(20);
assertEquals(20, al.get());
System.out.println("lazySet(20): " + al.get());
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### decrementAndGet

- 描述: 自减并返回新值
- 断言: 10 自减后为 9

```java
// 方法体开始
System.out.println("=== decrementAndGet ===");
java.util.concurrent.atomic.AtomicLong al = new java.util.concurrent.atomic.AtomicLong(10);
long val = al.decrementAndGet();
assertEquals(9, val);
System.out.println("decrementAndGet: " + val);
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### longValue

- 描述: 转换为 long
- 断言: 转换值正确

```java
// 方法体开始
System.out.println("=== longValue ===");
java.util.concurrent.atomic.AtomicLong al = new java.util.concurrent.atomic.AtomicLong(42);
assertEquals(42L, al.longValue());
System.out.println("longValue: " + al.longValue());
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### floatValue

- 描述: 转换为 float
- 断言: 转换值正确

```java
// 方法体开始
System.out.println("=== floatValue ===");
java.util.concurrent.atomic.AtomicLong al = new java.util.concurrent.atomic.AtomicLong(42);
assertEquals(42.0f, al.floatValue(), 0.001f);
System.out.println("floatValue: " + al.floatValue());
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### doubleValue

- 描述: 转换为 double
- 断言: 转换值正确

```java
// 方法体开始
System.out.println("=== doubleValue ===");
java.util.concurrent.atomic.AtomicLong al = new java.util.concurrent.atomic.AtomicLong(42);
assertEquals(42.0d, al.doubleValue(), 0.001d);
System.out.println("doubleValue: " + al.doubleValue());
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### intValue

- 描述: 转换为 int
- 断言: 转换值正确

```java
// 方法体开始
System.out.println("=== intValue ===");
java.util.concurrent.atomic.AtomicLong al = new java.util.concurrent.atomic.AtomicLong(42);
assertEquals(42, al.intValue());
System.out.println("intValue: " + al.intValue());
System.out.println("=== 测试通过 ===");
// 方法体结束
```
