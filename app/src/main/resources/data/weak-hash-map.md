---
name: WeakHashMap
package: java.util
order: 121
---

## 介绍

`java.util.WeakHashMap` 是 Java 中一种特殊的 Map，其中的键使用**弱引用（WeakReference）**。当键不再有外部强引用时，GC 会自动回收，对应的条目也会在下次操作时被清理。

Java 8 为 WeakHashMap 新增了 `computeIfAbsent`、`computeIfPresent`、`compute`、`merge`、`forEach`、`replaceAll` 等 Map 接口的默认方法支持。

WeakHashMap 的核心用途：
- **缓存**：当键可能被回收时自动清理缓存条目
- **元数据**：附加在对象上的元数据，对象回收时自动清理
- **避免内存泄漏**：与普通 HashMap 不同，不会因缓存条目导致对象无法 GC

## 方法

构造方法：
```java
public WeakHashMap()
public WeakHashMap(int initialCapacity)
```

常用方法（继承自 Map）：
- `put(K, V)` / `get(Object)` / `remove(Object)` — 基础操作
- `computeIfAbsent(K, Function)` — 键不存在时计算并插入
- `computeIfPresent(K, BiFunction)` — 键存在时计算
- `forEach(BiConsumer)` — 遍历
- `size()` / `clear()`

## 测试

### 基本操作

- 描述: WeakHashMap 的基本 put/get 操作
- 断言: 正常存取

```java
// 方法体开始
System.out.println("=== 基本操作 ===");
WeakHashMap<String, Integer> map = new WeakHashMap<>();
map.put("A", 1);
map.put("B", 2);
assertEquals(Integer.valueOf(1), map.get("A"));
assertEquals(2, map.size());
System.out.println("map: " + map);
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### computeIfAbsent

- 描述: 使用 computeIfAbsent 延迟计算
- 断言: 键不存在时才计算

```java
// 方法体开始
System.out.println("=== computeIfAbsent ===");
WeakHashMap<String, Integer> map = new WeakHashMap<>();
map.put("A", 10);
Integer val = map.computeIfAbsent("A", k -> 100);
assertEquals(Integer.valueOf(10), val);  // 已存在，不计算
val = map.computeIfAbsent("B", k -> 200);
assertEquals(Integer.valueOf(200), val); // 不存在，计算
assertEquals(2, map.size());
System.out.println("A=" + map.get("A") + ", B=" + map.get("B"));
System.out.println("=== 测试通过 ===");
// 方法体结束
```
