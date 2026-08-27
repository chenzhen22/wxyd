---
name: AtomicIntegerArray
package: java.util.concurrent.atomic
order: 161
---

## 介绍

`java.util.concurrent.atomic.AtomicIntegerArray` 是**原子操作 int 数组**的类。Java 8 为其新增了 `getAndUpdate`、`updateAndGet`、`getAndAccumulate`、`accumulateAndGet` 等 Lambda 友好的方法。

与 `AtomicLongArray` 对应，只是元素类型为 int。

## 方法

### updateAndGet

```java
public final int updateAndGet(int i, IntUnaryOperator updateFunction)
```

对指定索引的元素执行更新操作并返回新值。

### getAndUpdate / getAndAccumulate / accumulateAndGet

同 AtomicLongArray，返回类型为 int。

## 测试

### updateAndGet

- 描述: 使用 updateAndGet 原子更新数组元素
- 断言: 平方操作正确

```java
// 方法体开始
System.out.println("=== AtomicIntegerArray ===");
AtomicIntegerArray arr = new AtomicIntegerArray(new int[]{1, 2, 3, 4, 5});
int old = arr.getAndUpdate(2, x -> x * x);
assertEquals(3, old);  // 旧值
assertEquals(9, arr.get(2));  // 新值
System.out.println("arr[2] 旧值: " + old + ", 新值: " + arr.get(2));
System.out.println("=== 测试通过 ===");
// 方法体结束
```
