---
name: PrimitiveIterator
package: java.util
order: 103
---

## 介绍

`PrimitiveIterator<T, T_CONS>` 是 Java 8 引入的**原始类型迭代器**接口，专为 int、long、double 原始类型设计，避免装箱开销。它扩展了 `Iterator` 接口，并提供了 `forEachRemaining` 的原始类型版本。

PrimitiveIterator 有三个内置的子接口：
- `PrimitiveIterator.OfInt` — 迭代 int 值
- `PrimitiveIterator.OfLong` — 迭代 long 值
- `PrimitiveIterator.OfDouble` — 迭代 double 值

每个子接口都提供了 `nextInt()`/`nextLong()`/`nextDouble()` 方法，直接返回原始类型值。

## 方法

### nextInt / nextLong / nextDouble

```java
public int nextInt()
public long nextLong()
public double nextDouble()
```

返回下一个原始类型值（子接口方法）。

### forEachRemaining

```java
public default void forEachRemaining(IntConsumer action)
public default void forEachRemaining(LongConsumer action)
public default void forEachRemaining(DoubleConsumer action)
```

对每个剩余元素执行操作（子接口重载版本）。

## 测试

### OfInt 遍历

- 描述: 使用 OfInt 迭代 int 数组
- 断言: 遍历所有元素

```java
// 方法体开始
System.out.println("=== OfInt ===");
int[] array = {10, 20, 30, 40, 50};
PrimitiveIterator.OfInt it = new PrimitiveIterator.OfInt() {
    private int index = 0;
    public boolean hasNext() { return index < array.length; }
    public int nextInt() { return array[index++]; }
};
List<Integer> result = new ArrayList<>();
it.forEachRemaining(result::add);
assertEquals(5, result.size());
assertEquals(Integer.valueOf(10), result.get(0));
assertEquals(Integer.valueOf(50), result.get(4));
System.out.println("遍历结果: " + result);
System.out.println("=== 测试通过 ===");
// 方法体结束
```
