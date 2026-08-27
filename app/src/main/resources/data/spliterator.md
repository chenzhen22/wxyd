---
name: Spliterator
package: java.util
order: 100
---

## 介绍

`Spliterator<T>` 是 Java 8 引入的一个**可拆分迭代器**，是 Stream 并行处理的底层基础设施。它代表一个可以分割成子部分的元素源，支持顺序遍历和并行遍历两种模式。

Spliterator 的核心特点：
- **可拆分**：通过 `trySplit()` 将元素源分拆为两部分，支持并行处理
- **批量遍历**：`tryAdvance` 单元素遍历 + `forEachRemaining` 批量遍历
- **特性标记**：通过 `characteristics()` 报告数据源特性（有序、去重、不可变等）
- **大小估计**：`estimateSize()` 估计剩余元素数量

Spliterator 的特性常量：
- `ORDERED` — 元素有固定顺序
- `DISTINCT` — 元素不重复
- `SORTED` — 元素已排序
- `SIZED` — 有精确大小
- `NONNULL` — 元素不为 null
- `IMMUTABLE` — 数据源不可变
- `CONCURRENT` — 数据源可以并发修改
- `SUBSIZED` — 子 Spliterator 也有精确大小

## 方法

### tryAdvance

```java
public boolean tryAdvance(Consumer<? super T> action)
```

对下一个元素执行动作，有元素返回 true，没有返回 false。

### forEachRemaining

```java
public void forEachRemaining(Consumer<? super T> action)
```

对剩余所有元素顺序执行动作。

### trySplit

```java
public Spliterator<T> trySplit()
```

拆分当前 Spliterator 为两部分，返回的新 Spliterator 处理一部分，当前对象处理另一部分。

### estimateSize

```java
public long estimateSize()
```

估计剩余元素数量。

### getExactSizeIfKnown

```java
public long getExactSizeIfKnown()
```

如果具有 SIZED 特性则返回精确大小，否则返回 -1。

### characteristics

```java
public int characteristics()
```

返回特性标记集合。

### hasCharacteristics

```java
public boolean hasCharacteristics(int characteristics)
```

检查是否具有指定特性。

## 测试

### tryAdvance 遍历

- 描述: 使用 Spliterator 遍历列表
- 断言: 遍历所有元素

```java
// 方法体开始
System.out.println("=== tryAdvance ===");
List<String> list = Arrays.asList("A", "B", "C");
Spliterator<String> spl = list.spliterator();
List<String> result = new ArrayList<>();
spl.tryAdvance(result::add);
spl.tryAdvance(result::add);
spl.tryAdvance(result::add);
assertFalse(spl.tryAdvance(result::add));  // 没有更多元素
assertEquals(3, result.size());
assertEquals("[A, B, C]", result.toString());
System.out.println("遍历结果: " + result);
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### forEachRemaining

- 描述: 使用 `forEachRemaining` 批量遍历
- 断言: 遍历所有剩余元素

```java
// 方法体开始
System.out.println("=== forEachRemaining ===");
List<Integer> list = Arrays.asList(1, 2, 3, 4, 5);
List<Integer> result = new ArrayList<>();
Spliterator<Integer> spl = list.spliterator();
spl.forEachRemaining(result::add);
assertEquals(5, result.size());
assertEquals("[1, 2, 3, 4, 5]", result.toString());
System.out.println("遍历结果: " + result);
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### trySplit

- 描述: 使用 `trySplit` 拆分 Spliterator
- 断言: 拆分后两个 Spliterator 覆盖全部元素

```java
// 方法体开始
System.out.println("=== trySplit ===");
List<Integer> list = new ArrayList<>();
for (int i = 0; i < 10; i++) list.add(i);
Spliterator<Integer> spl1 = list.spliterator();
Spliterator<Integer> spl2 = spl1.trySplit();  // 拆分
assertNotNull(spl2);
List<Integer> result1 = new ArrayList<>();
List<Integer> result2 = new ArrayList<>();
spl1.forEachRemaining(result1::add);
spl2.forEachRemaining(result2::add);
assertEquals(10, result1.size() + result2.size());
System.out.println("第一部分: " + result2 + ", 第二部分: " + result1);
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### characteristics

- 描述: 检查 ArrayList 的 Spliterator 特性
- 断言: 具有 ORDERED 和 SIZED 特性

```java
// 方法体开始
System.out.println("=== characteristics ===");
List<String> list = Arrays.asList("a", "b", "c");
Spliterator<String> spl = list.spliterator();
int chars = spl.characteristics();
assertTrue((chars & Spliterator.ORDERED) != 0);
assertTrue((chars & Spliterator.SIZED) != 0);
System.out.println("ORDERED: " + (chars & Spliterator.ORDERED));
System.out.println("SIZED: " + (chars & Spliterator.SIZED));
System.out.println("=== 测试通过 ===");
// 方法体结束
```
