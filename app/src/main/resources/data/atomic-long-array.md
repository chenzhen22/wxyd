---
name: AtomicLongArray
package: java.util.concurrent.atomic
order: 160
---

## 介绍

`java.util.concurrent.atomic.AtomicLongArray` 是**原子操作 long 数组**的类。Java 8 新增了 `getAndUpdate`、`updateAndGet`、`getAndAccumulate`、`accumulateAndGet` 等 Lambda 友好的方法。

AtomicLongArray 在 Java 8 中新增的方法：
- `getAndUpdate(int, LongUnaryOperator)` — 获取旧值并更新
- `updateAndGet(int, LongUnaryOperator)` — 更新并返回新值
- `getAndAccumulate(int, long, LongBinaryOperator)` — 获取旧值并累加
- `accumulateAndGet(int, long, LongBinaryOperator)` — 累加并返回新值

对应的还有 `AtomicIntegerArray` 和 `AtomicReferenceArray`。

## 方法

### getAndUpdate / updateAndGet

```java
public final long getAndUpdate(int i, LongUnaryOperator updateFunction)
public final long updateAndGet(int i, LongUnaryOperator updateFunction)
```

对指定索引的元素执行更新操作。

### getAndAccumulate / accumulateAndGet

```java
public final long getAndAccumulate(int i, long x, LongBinaryOperator accumulatorFunction)
public final long accumulateAndGet(int i, long x, LongBinaryOperator accumulatorFunction)
```

对指定索引的元素执行累加操作。

## 测试

### updateAndGet

- 描述: 使用 updateAndGet 原子更新
- 断言: 翻倍操作正确

```java
// 方法体开始
System.out.println("=== updateAndGet ===");
AtomicLongArray arr = new AtomicLongArray(new long[]{10, 20, 30});
long v = arr.updateAndGet(1, x -> x * 2);
assertEquals(40L, v);
assertEquals(40L, arr.get(1));
assertEquals(10L, arr.get(0));
System.out.println("arr[1] 翻倍: " + arr.get(1));
System.out.println("=== 测试通过 ===");
// 方法体结束
```
