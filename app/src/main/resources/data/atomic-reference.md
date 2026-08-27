---
name: AtomicReference
package: java.util.concurrent.atomic
order: 40
---

## 介绍

`AtomicReference` 是 Java 5 引入的原子操作类，提供了对对象引用的原子更新操作。内部基于 CAS（Compare-And-Swap）机制实现，无需使用锁即可保证线程安全。

与 `AtomicInteger`、`AtomicLong` 等基本类型原子类不同，`AtomicReference` 可以包装任意引用类型，适用于需要原子更新对象引用的并发场景，如实现无锁数据结构、状态机转换等。

## 方法

### AtomicReference()

```java
public AtomicReference()
```

创建一个初始值为 `null` 的 `AtomicReference`。

### AtomicReference(V)

```java
public AtomicReference(V initialValue)
```

创建一个指定初始值的 `AtomicReference`。

- **参数**: `initialValue` — 初始值（可为 null）

### get

```java
public final V get()
```

获取当前值。

- **返回**: `V` — 当前值

### set

```java
public final void set(V newValue)
```

设置为给定值。

- **参数**: `newValue` — 新值

### lazySet

```java
public final void lazySet(V newValue)
```

延迟设置为给定值。与 `set()` 的区别在于不保证立即可见，性能更高。

- **参数**: `newValue` — 新值

### getAndSet

```java
public final V getAndSet(V newValue)
```

原子地设置为给定值并返回旧值。

- **参数**: `newValue` — 新值
- **返回**: `V` — 旧值

### compareAndSet

```java
public final boolean compareAndSet(V expect, V update)
```

如果当前值等于期望值（按 `==` 比较），则原子地设置为新值。CAS 操作的核心方法。

- **参数**: `expect` — 期望值；`update` — 新值
- **返回**: `boolean` — 如果设置成功则返回 true
- **说明**: 使用 `==` 而非 `equals()` 进行比较

### toString

```java
public String toString()
```

返回当前值的字符串表示。

- **返回**: `String`

## 测试

### AtomicReference

- 描述: 测试两个构造方法
- 断言: 无参构造初始值为 null，有参构造初始值为指定值

```java
// 方法体开始
System.out.println("=== AtomicReference ===");
java.util.concurrent.atomic.AtomicReference<String> ar1 = new java.util.concurrent.atomic.AtomicReference<String>();
System.out.println("无参构造默认值: " + ar1.get());
assertNull(ar1.get());
java.util.concurrent.atomic.AtomicReference<String> ar2 = new java.util.concurrent.atomic.AtomicReference<String>("hello");
System.out.println("有参构造初始值: " + ar2.get());
assertEquals("hello", ar2.get());
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### get

- 描述: 测试 get 方法
- 断言: get 返回当前值

```java
// 方法体开始
System.out.println("=== get ===");
java.util.concurrent.atomic.AtomicReference<String> ar = new java.util.concurrent.atomic.AtomicReference<String>("world");
String val = ar.get();
assertEquals("world", val);
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
java.util.concurrent.atomic.AtomicReference<String> ar = new java.util.concurrent.atomic.AtomicReference<String>("old");
ar.set("new");
assertEquals("new", ar.get());
System.out.println("set(\"new\") 后: " + ar.get());
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### compareAndSet

- 描述: 测试 compareAndSet CAS 操作
- 断言: 期望值匹配时设置成功返回 true，不匹配时返回 false

```java
// 方法体开始
System.out.println("=== compareAndSet ===");
java.util.concurrent.atomic.AtomicReference<String> ar = new java.util.concurrent.atomic.AtomicReference<String>("a");
boolean success1 = ar.compareAndSet("a", "b");
System.out.println("CAS(\"a\",\"b\") 结果: " + success1 + ", 当前值: " + ar.get());
assertTrue(success1);
assertEquals("b", ar.get());
boolean success2 = ar.compareAndSet("a", "c");
System.out.println("CAS(\"a\",\"c\") 结果: " + success2 + "（期望不匹配），当前值: " + ar.get());
assertFalse(success2);
assertEquals("b", ar.get());
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### getAndSet

- 描述: 测试 getAndSet 方法
- 断言: getAndSet 返回旧值并设置新值

```java
// 方法体开始
System.out.println("=== getAndSet ===");
java.util.concurrent.atomic.AtomicReference<String> ar = new java.util.concurrent.atomic.AtomicReference<String>("first");
String old = ar.getAndSet("second");
assertEquals("first", old);
assertEquals("second", ar.get());
System.out.println("getAndSet(\"second\") 旧值: " + old + ", 新值: " + ar.get());
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### toString

- 描述: 测试 toString 方法
- 断言: toString 返回当前值的字符串表示

```java
// 方法体开始
System.out.println("=== toString ===");
java.util.concurrent.atomic.AtomicReference<String> ar = new java.util.concurrent.atomic.AtomicReference<String>("hello");
System.out.println("toString: " + ar.toString());
assertEquals("hello", ar.toString());
assertNotNull(ar.toString());
java.util.concurrent.atomic.AtomicReference<Integer> ar2 = new java.util.concurrent.atomic.AtomicReference<Integer>(42);
System.out.println("Integer 的 toString: " + ar2.toString());
assertEquals("42", ar2.toString());
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### lazySet

- 描述: 延迟设置值
- 断言: lazySet 后 get 返回新值

```java
// 方法体开始
System.out.println("=== lazySet ===");
java.util.concurrent.atomic.AtomicReference<String> ar = new java.util.concurrent.atomic.AtomicReference<>("old");
ar.lazySet("new");
assertEquals("new", ar.get());
System.out.println("lazySet new: " + ar.get());
System.out.println("=== 测试通过 ===");
// 方法体结束
```

