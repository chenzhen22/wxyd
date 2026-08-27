---
name: AtomicInteger
package: java.util.concurrent.atomic
order: 15
---

## 介绍

`AtomicInteger` 是 Java 5 引入的原子操作类，提供了对 `int` 值的原子更新操作，用于高并发场景下的计数、累加等。内部基于 CAS（Compare-And-Swap）机制实现，无需使用锁即可保证线程安全。

与 `synchronized` 或 `Lock` 相比，`AtomicInteger` 的优势在于：
- **无锁**：基于 CAS 硬件原语，性能更高
- **轻量级**：只针对单个变量，无需复杂的同步管理
- **自旋重试**：CAS 失败时自动重试，不会阻塞线程

## 方法

### AtomicInteger()

```java
public AtomicInteger()
```

创建一个初始值为 `0` 的 `AtomicInteger`。

### AtomicInteger(int)

```java
public AtomicInteger(int initialValue)
```

创建一个指定初始值的 `AtomicInteger`。

- **参数**: `initialValue` — 初始值

### get

```java
public final int get()
```

获取当前值。

- **返回**: `int` — 当前值

### set

```java
public final void set(int newValue)
```

设置为给定值。

- **参数**: `newValue` — 新值

### lazySet

```java
public final void lazySet(int newValue)
```

延迟设置为给定值。与 `set()` 的区别在于不保证立即可见，性能更高。

- **参数**: `newValue` — 新值
- **说明**: 在后续的 `volatile` 写或同步操作后，才对其他线程可见

### getAndSet

```java
public final int getAndSet(int newValue)
```

原子地设置为给定值并返回旧值。

- **参数**: `newValue` — 新值
- **返回**: `int` — 旧值

### compareAndSet

```java
public final boolean compareAndSet(int expect, int update)
```

如果当前值等于期望值，则原子地设置为新值。CAS 操作的核心方法。

- **参数**: `expect` — 期望值；`update` — 新值
- **返回**: `boolean` — 如果设置成功则返回 true

### getAndIncrement

```java
public final int getAndIncrement()
```

原子地自增 1 并返回旧值。

- **返回**: `int` — 旧值

### incrementAndGet

```java
public final int incrementAndGet()
```

原子地自增 1 并返回新值。

- **返回**: `int` — 新值

### getAndDecrement

```java
public final int getAndDecrement()
```

原子地自减 1 并返回旧值。

- **返回**: `int` — 旧值

### decrementAndGet

```java
public final int decrementAndGet()
```

原子地自减 1 并返回新值。

- **返回**: `int` — 新值

### getAndAdd

```java
public final int getAndAdd(int delta)
```

原子地加上给定值并返回旧值。

- **参数**: `delta` — 要加的值
- **返回**: `int` — 旧值

### addAndGet

```java
public final int addAndGet(int delta)
```

原子地加上给定值并返回新值。

- **参数**: `delta` — 要加的值
- **返回**: `int` — 新值

### intValue / longValue / floatValue / doubleValue

```java
public int intValue()
public long longValue()
public float floatValue()
public double doubleValue()
```

将 `AtomicInteger` 的值转换为对应的数字类型。

- **返回**: 转换后的值

### toString

```java
public String toString()
```

返回当前值的字符串表示。

- **返回**: `String`

## 测试

### AtomicInteger

- 描述: 测试两个构造方法
- 断言: 无参构造初始值为 0，有参构造初始值为指定值

```java
// 方法体开始
System.out.println("=== AtomicInteger ===");
java.util.concurrent.atomic.AtomicInteger ai1 = new java.util.concurrent.atomic.AtomicInteger();
System.out.println("无参构造默认值: " + ai1.get());
assertEquals(0, ai1.get());
java.util.concurrent.atomic.AtomicInteger ai2 = new java.util.concurrent.atomic.AtomicInteger(100);
System.out.println("有参构造初始值: " + ai2.get());
assertEquals(100, ai2.get());
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### get

- 描述: 测试 get 方法
- 断言: get 返回当前值

```java
// 方法体开始
System.out.println("=== get ===");
java.util.concurrent.atomic.AtomicInteger ai = new java.util.concurrent.atomic.AtomicInteger(42);
int val = ai.get();
assertEquals(42, val);
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
java.util.concurrent.atomic.AtomicInteger ai = new java.util.concurrent.atomic.AtomicInteger(0);
ai.set(100);
assertEquals(100, ai.get());
System.out.println("set(100) 后: " + ai.get());
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### getAndSet

- 描述: 测试 getAndSet 方法
- 断言: getAndSet 返回旧值并设置新值

```java
// 方法体开始
System.out.println("=== getAndSet ===");
java.util.concurrent.atomic.AtomicInteger ai = new java.util.concurrent.atomic.AtomicInteger(10);
int old = ai.getAndSet(30);
assertEquals(10, old);
assertEquals(30, ai.get());
System.out.println("getAndSet(30) 旧值: " + old + ", 新值: " + ai.get());
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### lazySet

- 描述: 测试 lazySet 方法
- 断言: lazySet 后 get 返回新值

```java
// 方法体开始
System.out.println("=== lazySet ===");
java.util.concurrent.atomic.AtomicInteger ai = new java.util.concurrent.atomic.AtomicInteger(5);
ai.lazySet(50);
System.out.println("lazySet(50) 后: " + ai.get());
assertEquals(50, ai.get());
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### compareAndSet

- 描述: 测试 compareAndSet CAS 操作
- 断言: 期望值匹配时设置成功返回 true，不匹配时返回 false

```java
// 方法体开始
System.out.println("=== compareAndSet ===");
java.util.concurrent.atomic.AtomicInteger ai = new java.util.concurrent.atomic.AtomicInteger(0);
boolean success1 = ai.compareAndSet(0, 1);
System.out.println("CAS(0,1) 结果: " + success1 + ", 当前值: " + ai.get());
assertTrue(success1);
assertEquals(1, ai.get());
boolean success2 = ai.compareAndSet(0, 2);
System.out.println("CAS(0,2) 结果: " + success2 + "（期望不匹配），当前值: " + ai.get());
assertFalse(success2);
assertEquals(1, ai.get());
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### getAndIncrement

- 描述: 测试 getAndIncrement 方法
- 断言: getAndIncrement 返回旧值并自增

```java
// 方法体开始
System.out.println("=== getAndIncrement ===");
java.util.concurrent.atomic.AtomicInteger ai = new java.util.concurrent.atomic.AtomicInteger(10);
int old = ai.getAndIncrement();
assertEquals(10, old);
assertEquals(11, ai.get());
System.out.println("getAndIncrement 旧值: " + old + ", 新值: " + ai.get());
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### incrementAndGet

- 描述: 测试 incrementAndGet 方法
- 断言: incrementAndGet 自增并返回新值

```java
// 方法体开始
System.out.println("=== incrementAndGet ===");
java.util.concurrent.atomic.AtomicInteger ai = new java.util.concurrent.atomic.AtomicInteger(10);
int val = ai.incrementAndGet();
assertEquals(11, val);
assertEquals(11, ai.get());
System.out.println("incrementAndGet 新值: " + val);
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### getAndDecrement

- 描述: 测试 getAndDecrement 方法
- 断言: getAndDecrement 返回旧值并自减

```java
// 方法体开始
System.out.println("=== getAndDecrement ===");
java.util.concurrent.atomic.AtomicInteger ai = new java.util.concurrent.atomic.AtomicInteger(10);
int old = ai.getAndDecrement();
assertEquals(10, old);
assertEquals(9, ai.get());
System.out.println("getAndDecrement 旧值: " + old + ", 新值: " + ai.get());
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### decrementAndGet

- 描述: 测试 decrementAndGet 方法
- 断言: decrementAndGet 自减并返回新值

```java
// 方法体开始
System.out.println("=== decrementAndGet ===");
java.util.concurrent.atomic.AtomicInteger ai = new java.util.concurrent.atomic.AtomicInteger(10);
int val = ai.decrementAndGet();
assertEquals(9, val);
assertEquals(9, ai.get());
System.out.println("decrementAndGet 新值: " + val);
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### getAndAdd

- 描述: 测试 getAndAdd 方法
- 断言: getAndAdd 返回旧值并加上给定值

```java
// 方法体开始
System.out.println("=== getAndAdd ===");
java.util.concurrent.atomic.AtomicInteger ai = new java.util.concurrent.atomic.AtomicInteger(100);
int old = ai.getAndAdd(5);
assertEquals(100, old);
assertEquals(105, ai.get());
System.out.println("getAndAdd(5) 旧值: " + old + ", 新值: " + ai.get());
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### addAndGet

- 描述: 测试 addAndGet 方法
- 断言: addAndGet 加上给定值并返回新值

```java
// 方法体开始
System.out.println("=== addAndGet ===");
java.util.concurrent.atomic.AtomicInteger ai = new java.util.concurrent.atomic.AtomicInteger(100);
int sum = ai.addAndGet(15);
assertEquals(115, sum);
assertEquals(115, ai.get());
System.out.println("addAndGet(15) 新值: " + sum);
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### intValue

- 描述: 测试 intValue 方法
- 断言: intValue 返回 int 值

```java
// 方法体开始
System.out.println("=== intValue ===");
java.util.concurrent.atomic.AtomicInteger ai = new java.util.concurrent.atomic.AtomicInteger(42);
int v = ai.intValue();
assertEquals(42, v);
System.out.println("intValue: " + v);
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### longValue

- 描述: 测试 longValue 方法
- 断言: longValue 返回 long 值

```java
// 方法体开始
System.out.println("=== longValue ===");
java.util.concurrent.atomic.AtomicInteger ai = new java.util.concurrent.atomic.AtomicInteger(42);
long v = ai.longValue();
assertEquals(42L, v);
System.out.println("longValue: " + v);
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### floatValue

- 描述: 测试 floatValue 方法
- 断言: floatValue 返回 float 值

```java
// 方法体开始
System.out.println("=== floatValue ===");
java.util.concurrent.atomic.AtomicInteger ai = new java.util.concurrent.atomic.AtomicInteger(42);
float v = ai.floatValue();
assertEquals(42.0f, v, 0.0001f);
System.out.println("floatValue: " + v);
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### doubleValue

- 描述: 测试 doubleValue 方法
- 断言: doubleValue 返回 double 值

```java
// 方法体开始
System.out.println("=== doubleValue ===");
java.util.concurrent.atomic.AtomicInteger ai = new java.util.concurrent.atomic.AtomicInteger(42);
double v = ai.doubleValue();
assertEquals(42.0d, v, 0.0001d);
System.out.println("doubleValue: " + v);
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### toString

- 描述: 测试 toString 方法
- 断言: toString 返回当前值的字符串表示

```java
// 方法体开始
System.out.println("=== toString ===");
java.util.concurrent.atomic.AtomicInteger ai = new java.util.concurrent.atomic.AtomicInteger(256);
String str = ai.toString();
System.out.println("toString: " + str);
assertEquals("256", str);
assertNotNull(str);
java.util.concurrent.atomic.AtomicInteger ai2 = new java.util.concurrent.atomic.AtomicInteger(-99);
System.out.println("负数 toString: " + ai2.toString());
assertEquals("-99", ai2.toString());
System.out.println("=== 测试通过 ===");
// 方法体结束
```
