---
name: AtomicBoolean
package: java.util.concurrent.atomic
order: 39
---

## 介绍

`AtomicBoolean` 是 Java 5 引入的原子操作类，提供了对 `boolean` 值的原子更新操作。内部基于 CAS（Compare-And-Swap）机制实现，无需使用锁即可保证线程安全。

`AtomicBoolean` 常用于并发场景下的标志位控制、状态开关、一次性初始化等场景。其方法集比 `AtomicInteger`/`AtomicLong` 更精简，专注于布尔值的原子读写和条件更新。

## 方法

### AtomicBoolean()

```java
public AtomicBoolean()
```

创建一个初始值为 `false` 的 `AtomicBoolean`。

### AtomicBoolean(boolean)

```java
public AtomicBoolean(boolean initialValue)
```

创建一个指定初始值的 `AtomicBoolean`。

- **参数**: `initialValue` — 初始值

### get

```java
public final boolean get()
```

获取当前值。

- **返回**: `boolean` — 当前值

### set

```java
public final void set(boolean newValue)
```

设置为给定值。

- **参数**: `newValue` — 新值

### lazySet

```java
public final void lazySet(boolean newValue)
```

延迟设置为给定值。与 `set()` 的区别在于不保证立即可见，性能更高。

- **参数**: `newValue` — 新值

### getAndSet

```java
public final boolean getAndSet(boolean newValue)
```

原子地设置为给定值并返回旧值。

- **参数**: `newValue` — 新值
- **返回**: `boolean` — 旧值

### compareAndSet

```java
public final boolean compareAndSet(boolean expect, boolean update)
```

如果当前值等于期望值，则原子地设置为新值。CAS 操作的核心方法。

- **参数**: `expect` — 期望值；`update` — 新值
- **返回**: `boolean` — 如果设置成功则返回 true

### toString

```java
public String toString()
```

返回当前值的字符串表示。

- **返回**: `String` — `"true"` 或 `"false"`

## 测试

### AtomicBoolean

- 描述: 测试两个构造方法
- 断言: 无参构造初始值为 false，有参构造初始值为指定值

```java
// 方法体开始
System.out.println("=== AtomicBoolean ===");
java.util.concurrent.atomic.AtomicBoolean ab1 = new java.util.concurrent.atomic.AtomicBoolean();
System.out.println("无参构造默认值: " + ab1.get());
assertFalse(ab1.get());
java.util.concurrent.atomic.AtomicBoolean ab2 = new java.util.concurrent.atomic.AtomicBoolean(true);
System.out.println("有参构造初始值: " + ab2.get());
assertTrue(ab2.get());
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### get

- 描述: 测试 get 方法
- 断言: get 返回当前值

```java
// 方法体开始
System.out.println("=== get ===");
java.util.concurrent.atomic.AtomicBoolean ab = new java.util.concurrent.atomic.AtomicBoolean(true);
boolean val = ab.get();
assertTrue(val);
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
java.util.concurrent.atomic.AtomicBoolean ab = new java.util.concurrent.atomic.AtomicBoolean(true);
ab.set(false);
assertFalse(ab.get());
System.out.println("set(false) 后: " + ab.get());
ab.set(true);
assertTrue(ab.get());
System.out.println("set(true) 后: " + ab.get());
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### compareAndSet

- 描述: 测试 compareAndSet CAS 操作
- 断言: 期望值匹配时设置成功返回 true，不匹配时返回 false

```java
// 方法体开始
System.out.println("=== compareAndSet ===");
java.util.concurrent.atomic.AtomicBoolean ab = new java.util.concurrent.atomic.AtomicBoolean(false);
boolean success1 = ab.compareAndSet(false, true);
System.out.println("CAS(false,true) 结果: " + success1 + ", 当前值: " + ab.get());
assertTrue(success1);
assertTrue(ab.get());
boolean success2 = ab.compareAndSet(false, true);
System.out.println("CAS(false,true) 结果: " + success2 + "（期望不匹配），当前值: " + ab.get());
assertFalse(success2);
assertTrue(ab.get());
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### getAndSet

- 描述: 测试 getAndSet 方法
- 断言: getAndSet 返回旧值并设置新值

```java
// 方法体开始
System.out.println("=== getAndSet ===");
java.util.concurrent.atomic.AtomicBoolean ab = new java.util.concurrent.atomic.AtomicBoolean(false);
boolean old = ab.getAndSet(true);
assertFalse(old);
assertTrue(ab.get());
System.out.println("getAndSet(true) 旧值: " + old + ", 新值: " + ab.get());
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### toString

- 描述: 测试 toString 方法
- 断言: toString 返回当前值的字符串表示

```java
// 方法体开始
System.out.println("=== toString ===");
java.util.concurrent.atomic.AtomicBoolean ab1 = new java.util.concurrent.atomic.AtomicBoolean(true);
assertEquals("true", ab1.toString());
System.out.println("true 的 toString: " + ab1.toString());
java.util.concurrent.atomic.AtomicBoolean ab2 = new java.util.concurrent.atomic.AtomicBoolean(false);
assertEquals("false", ab2.toString());
System.out.println("false 的 toString: " + ab2.toString());
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### lazySet

- 描述: 延迟设置值
- 断言: lazySet 后 get 返回新值

```java
// 方法体开始
System.out.println("=== lazySet ===");
java.util.concurrent.atomic.AtomicBoolean ab = new java.util.concurrent.atomic.AtomicBoolean(true);
ab.lazySet(false);
assertFalse(ab.get());
System.out.println("lazySet false: " + ab.get());
System.out.println("=== 测试通过 ===");
// 方法体结束
```

