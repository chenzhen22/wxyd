---
name: ConcurrentSkipListSet
package: java.util.concurrent
order: 119
---

## 介绍

`java.util.concurrent.ConcurrentSkipListSet` 是 Java 6 引入的**并发可排序 Set**，基于 `ConcurrentSkipListMap` 实现。它在 Java 8 中获得了完整的 Stream、Lambda 和 Collection 增强方法支持。

ConcurrentSkipListSet 的核心特点：
- **线程安全**：高并发下的 NavigableSet 实现
- **可排序**：元素按自然顺序或 Comparator 排序
- **无锁读**：读取操作不需要加锁
- **范围操作**：支持 subSet、headSet、tailSet 等范围视图

## 方法

构造方法：
```java
public ConcurrentSkipListSet()
public ConcurrentSkipListSet(Comparator<? super E> comparator)
public ConcurrentSkipListSet(Collection<? extends E> c)
```

NavigableSet 方法：
- `add(E)` / `remove(Object)` — 添加/移除元素
- `ceiling(E)` / `floor(E)` — 大于等于/小于等于的最近元素
- `higher(E)` / `lower(E)` — 大于/小于的最近元素
- `first()` / `last()` — 首尾元素
- `pollFirst()` / `pollLast()` — 获取并移除首尾元素
- `subSet(E, E)` — 范围子集视图

## 测试

### 基本操作

- 描述: ConcurrentSkipListSet 基本操作
- 断言: 元素有序且线程安全

```java
// 方法体开始
System.out.println("=== 基本操作 ===");
ConcurrentSkipListSet<Integer> set = new ConcurrentSkipListSet<>();
set.add(3);
set.add(1);
set.add(2);
assertEquals("[1, 2, 3]", set.toString());
assertEquals(Integer.valueOf(1), set.first());
assertEquals(Integer.valueOf(3), set.last());
System.out.println("有序集合: " + set);
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### 范围操作

- 描述: 使用 ceiling 和 floor 查找最近元素
- 断言: 查找结果正确

```java
// 方法体开始
System.out.println("=== 范围操作 ===");
ConcurrentSkipListSet<Integer> set = new ConcurrentSkipListSet<>();
for (int i : new int[]{10, 20, 30, 40, 50}) set.add(i);
assertEquals(Integer.valueOf(20), set.ceiling(15));  // >= 15 的最小元素
assertEquals(Integer.valueOf(10), set.floor(15));     // <= 15 的最大元素
assertEquals(Integer.valueOf(40), set.higher(35));    // > 35 的最小元素
assertEquals(Integer.valueOf(30), set.lower(35));     // < 35 的最大元素
System.out.println("ceiling(15)=" + set.ceiling(15));
System.out.println("floor(15)=" + set.floor(15));
System.out.println("=== 测试通过 ===");
// 方法体结束
```
