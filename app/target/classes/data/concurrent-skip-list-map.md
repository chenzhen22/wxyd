---
name: ConcurrentSkipListMap
package: java.util.concurrent
order: 147
---

## 介绍

`java.util.concurrent.ConcurrentSkipListMap` 是 Java 6 引入的**可排序并发 Map**，基于跳表（Skip List）实现。在 Java 8 中获得了完整的 Stream、Lambda 和 Map 默认方法支持。

ConcurrentSkipListMap 的核心特点：
- **线程安全**：高并发下的 NavigableMap
- **可排序**：键按自然顺序或 Comparator 排序
- **无锁读**：读取操作无需加锁
- **范围操作**：subMap、headMap、tailMap 等视图

## 方法

构造方法：
```java
public ConcurrentSkipListMap()
public ConcurrentSkipListMap(Comparator<? super K> comparator)
```

NavigableMap 方法：
- `put(K, V)` / `get(Object)` — 基础存取
- `ceilingKey(K)` / `floorKey(K)` — 最近键查找
- `firstKey()` / `lastKey()` — 首尾键
- `subMap(K, K)` — 子 Map 视图
- `forEach(BiConsumer)` — 遍历
- `putIfAbsent` / `computeIfAbsent` / `merge` — Java 8 默认方法

## 测试

### 基本操作

- 描述: ConcurrentSkipListMap 排序存取
- 断言: 按键排序

```java
// 方法体开始
System.out.println("=== 基本操作 ===");
ConcurrentSkipListMap<Integer, String> map = new ConcurrentSkipListMap<>();
map.put(3, "C");
map.put(1, "A");
map.put(2, "B");
assertEquals("A", map.get(1));
assertEquals("{1=A, 2=B, 3=C}", map.toString());
System.out.println("有序 Map: " + map);
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### 范围查询

- 描述: 使用 ceilingKey 和 floorKey
- 断言: 查找结果正确

```java
// 方法体开始
System.out.println("=== 范围查询 ===");
ConcurrentSkipListMap<Integer, String> map = new ConcurrentSkipListMap<>();
map.put(10, "A"); map.put(20, "B"); map.put(30, "C"); map.put(40, "D");
assertEquals(Integer.valueOf(20), map.ceilingKey(15));
assertEquals(Integer.valueOf(10), map.floorKey(15));
assertEquals(Integer.valueOf(30), map.higherKey(20));
assertEquals(Integer.valueOf(20), map.lowerKey(25));
System.out.println("ceilingKey(15)=" + map.ceilingKey(15));
System.out.println("=== 测试通过 ===");
// 方法体结束
```
