---
name: LongAdder
package: java.util.concurrent.atomic
order: 30
---

## 介绍

`java.util.concurrent.atomic.LongAdder` 是 Java 8 引入的高性能累加器，专为**高并发写频繁、读较少**的场景设计。它通过将内部计数分散到多个变量（Cell 数组）来减少 CAS 竞争，在高度并发下性能远超 `AtomicLong`。

与 `AtomicLong` 的核心区别：
- **写性能高**：内部维护多个变量，不同线程更新不同 Cell，CAS 竞争大幅减少
- **读性能低**：`sum()` 需要遍历所有 Cell 求和，适合写多读少的场景
- **不保证强一致性**：`sum()` 返回的是调用时刻的近似值，但在实际场景中通常足够
- **适合统计计数**：如请求量统计、QPS 计数等

## 方法

### LongAdder()

```java
public LongAdder()
```

创建一个初始值为 0 的 LongAdder。

### add

```java
public void add(long x)
```

将当前值加上指定的增量 `x`。

- **参数**: `x` — 要加的值（可以为负数）

### increment

```java
public void increment()
```

将当前值加 1。等价于 `add(1L)`。

### decrement

```java
public void decrement()
```

将当前值减 1。等价于 `add(-1L)`。

### sum

```java
public long sum()
```

返回当前总和。在多线程环境下，返回值是一个近似值，因为其他线程可能同时更新。

- **返回**: `long` — 当前总和

### sumThenReset

```java
public long sumThenReset()
```

返回当前总和并将值重置为 0。适用于需要定期采集计数的场景（如每秒钟统计一次 QPS）。

- **返回**: `long` — 重置前的总和

### reset

```java
public void reset()
```

将值重置为 0。

### intValue

```java
public int intValue()
```

返回当前总和的 `int` 类型转换。等价于 `(int) sum()`。

- **返回**: `int`

### longValue

```java
public long longValue()
```

返回当前总和。等价于 `sum()`。

- **返回**: `long`

### floatValue

```java
public float floatValue()
```

返回当前总和的 `float` 类型转换。

- **返回**: `float`

### doubleValue

```java
public double doubleValue()
```

返回当前总和的 `double` 类型转换。

- **返回**: `double`

### toString

```java
public String toString()
```

返回当前总和的字符串表示。

- **返回**: `String`

## 测试

### constructorAndAdd

- 描述: 测试构造方法和 add 方法
- 断言: 初始值为 0，add(10) 后值为 10

```java
// 方法体开始
System.out.println("=== constructorAndAdd ===");
java.util.concurrent.atomic.LongAdder adder = new java.util.concurrent.atomic.LongAdder();
System.out.println("初始值: " + adder.sum());
assertEquals(0L, adder.sum());
adder.add(10L);
System.out.println("add(10) 后: " + adder.sum());
assertEquals(10L, adder.sum());
adder.add(5L);
System.out.println("add(5) 后: " + adder.sum());
assertEquals(15L, adder.sum());
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### increment

- 描述: 测试 increment 方法
- 断言: 多次 increment 后值正确

```java
// 方法体开始
System.out.println("=== increment ===");
java.util.concurrent.atomic.LongAdder adder = new java.util.concurrent.atomic.LongAdder();
adder.increment();
adder.increment();
adder.increment();
System.out.println("increment 3 次后: " + adder.sum());
assertEquals(3L, adder.sum());
for (int i = 0; i < 7; i++) {
    adder.increment();
}
System.out.println("再 increment 7 次后: " + adder.sum());
assertEquals(10L, adder.sum());
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### decrement

- 描述: 测试 decrement 方法
- 断言: increment 后 decrement 值正确

```java
// 方法体开始
System.out.println("=== decrement ===");
java.util.concurrent.atomic.LongAdder adder = new java.util.concurrent.atomic.LongAdder();
adder.add(10L);
adder.decrement();
System.out.println("add(10) 后 decrement: " + adder.sum());
assertEquals(9L, adder.sum());
adder.decrement();
adder.decrement();
System.out.println("再 decrement 2 次: " + adder.sum());
assertEquals(7L, adder.sum());
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### sumThenReset

- 描述: 测试 sumThenReset 方法
- 断言: 返回当前值并重置为 0

```java
// 方法体开始
System.out.println("=== sumThenReset ===");
java.util.concurrent.atomic.LongAdder adder = new java.util.concurrent.atomic.LongAdder();
adder.add(100L);
long sum = adder.sumThenReset();
System.out.println("sumThenReset 返回: " + sum + ", 重置后: " + adder.sum());
assertEquals(100L, sum);
assertEquals(0L, adder.sum());
adder.add(50L);
System.out.println("重置后 add(50): " + adder.sum());
assertEquals(50L, adder.sum());
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### conversions

- 描述: 测试数值转换方法
- 断言: intValue、longValue、floatValue、doubleValue 返回正确

```java
// 方法体开始
System.out.println("=== conversions ===");
java.util.concurrent.atomic.LongAdder adder = new java.util.concurrent.atomic.LongAdder();
adder.add(42L);
int intVal = adder.intValue();
long longVal = adder.longValue();
float floatVal = adder.floatValue();
double doubleVal = adder.doubleValue();
System.out.println("intValue: " + intVal + ", longValue: " + longVal + ", floatValue: " + floatVal + ", doubleValue: " + doubleVal);
assertEquals(42, intVal);
assertEquals(42L, longVal);
assertEquals(42.0f, floatVal, 0.0001f);
assertEquals(42.0d, doubleVal, 0.0001d);
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### toString

- 描述: 测试 toString 方法
- 断言: 返回当前值的字符串表示

```java
// 方法体开始
System.out.println("=== toString ===");
java.util.concurrent.atomic.LongAdder adder = new java.util.concurrent.atomic.LongAdder();
adder.add(256L);
String str = adder.toString();
System.out.println("toString: " + str);
assertEquals("256", str);
java.util.concurrent.atomic.LongAdder adder2 = new java.util.concurrent.atomic.LongAdder();
adder2.add(-99L);
System.out.println("负数 toString: " + adder2.toString());
assertEquals("-99", adder2.toString());
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### LongAdder

- 描述: 构造 LongAdder
- 断言: 初始和为 0

```java
// 方法体开始
System.out.println("=== LongAdder ===");
java.util.concurrent.atomic.LongAdder adder = new java.util.concurrent.atomic.LongAdder();
assertEquals(0L, adder.sum());
System.out.println("初始值: " + adder.sum());
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### add

- 描述: 测试 add 方法
- 断言: add 后值正确

```java
// 方法体开始
System.out.println("=== add ===");
java.util.concurrent.atomic.LongAdder adder = new java.util.concurrent.atomic.LongAdder();
adder.add(10L);
assertEquals(10L, adder.sum());
System.out.println("add(10) 后: " + adder.sum());
adder.add(-3L);
assertEquals(7L, adder.sum());
System.out.println("add(-3) 后: " + adder.sum());
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### sum

- 描述: 测试 sum 方法
- 断言: 返回当前总和

```java
// 方法体开始
System.out.println("=== sum ===");
java.util.concurrent.atomic.LongAdder adder = new java.util.concurrent.atomic.LongAdder();
assertEquals(0L, adder.sum());
adder.add(25L);
assertEquals(25L, adder.sum());
System.out.println("sum: " + adder.sum());
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### reset

- 描述: 测试 reset 方法
- 断言: reset 后值为 0

```java
// 方法体开始
System.out.println("=== reset ===");
java.util.concurrent.atomic.LongAdder adder = new java.util.concurrent.atomic.LongAdder();
adder.add(100L);
System.out.println("reset 前: " + adder.sum());
adder.reset();
assertEquals(0L, adder.sum());
System.out.println("reset 后: " + adder.sum());
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### intValue

- 描述: 测试 intValue 方法
- 断言: 返回 int 类型值

```java
// 方法体开始
System.out.println("=== intValue ===");
java.util.concurrent.atomic.LongAdder adder = new java.util.concurrent.atomic.LongAdder();
adder.add(42L);
int val = adder.intValue();
System.out.println("intValue: " + val);
assertEquals(42, val);
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### longValue

- 描述: 测试 longValue 方法
- 断言: 返回 long 类型值

```java
// 方法体开始
System.out.println("=== longValue ===");
java.util.concurrent.atomic.LongAdder adder = new java.util.concurrent.atomic.LongAdder();
adder.add(99L);
long val = adder.longValue();
System.out.println("longValue: " + val);
assertEquals(99L, val);
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### floatValue

- 描述: 测试 floatValue 方法
- 断言: 返回 float 类型值

```java
// 方法体开始
System.out.println("=== floatValue ===");
java.util.concurrent.atomic.LongAdder adder = new java.util.concurrent.atomic.LongAdder();
adder.add(10L);
float val = adder.floatValue();
System.out.println("floatValue: " + val);
assertEquals(10.0f, val, 0.0001f);
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### doubleValue

- 描述: 测试 doubleValue 方法
- 断言: 返回 double 类型值

```java
// 方法体开始
System.out.println("=== doubleValue ===");
java.util.concurrent.atomic.LongAdder adder = new java.util.concurrent.atomic.LongAdder();
adder.add(10L);
double val = adder.doubleValue();
System.out.println("doubleValue: " + val);
assertEquals(10.0d, val, 0.0001d);
System.out.println("=== 测试通过 ===");
// 方法体结束
```
