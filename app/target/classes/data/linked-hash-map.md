---
name: LinkedHashMap
package: java.util
order: 160
---

## 介绍

`java.util.LinkedHashMap` 是**维护插入顺序或访问顺序**的哈希链表 Map。Java 8 继承了 Map 接口的所有默认方法。

LinkedHashMap 的核心特点：
- **可预测的遍历顺序**：默认按插入顺序，也可设为访问顺序
- **LRU 缓存**：accessOrder + removeEldestEntry() 实现 LRU
- **比 HashMap 稍慢**：维护双向链表的额外开销
- **Java 8 方法**：forEach、computeIfAbsent、merge 等

## 方法

构造方法：
```java
public LinkedHashMap()
public LinkedHashMap(int initialCapacity, float loadFactor, boolean accessOrder)
```

特有方法：
- `get(Object)` — 获取值（accessOrder=true 时会移动该条目到末尾）

### 移除最老条目

```java
protected boolean removeEldestEntry(Map.Entry<K, V> eldest)
```

通常在子类中重写来实现 LRU 缓存。

## 测试

### 插入顺序

- 描述: LinkedHashMap 维护插入顺序
- 断言: 遍历顺序与插入顺序一致

```java
// 方法体开始
System.out.println("=== 插入顺序 ===");
LinkedHashMap<String, Integer> map = new LinkedHashMap<>();
map.put("A", 1);
map.put("B", 2);
map.put("C", 3);
List<String> keys = new ArrayList<>();
map.forEach((k, v) -> keys.add(k));
assertEquals("[A, B, C]", keys.toString());
System.out.println("插入顺序: " + keys);
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### LRU 缓存

- 描述: 使用 accessOrder 实现 LRU 缓存
- 断言: 最久未访问的条目被移除

```java
// 方法体开始
System.out.println("=== LRU 缓存 ===");
LinkedHashMap<Integer, String> cache = new LinkedHashMap<Integer, String>(16, 0.75f, true) {
    protected boolean removeEldestEntry(Map.Entry<Integer, String> eldest) {
        return size() > 3;
    }
};
cache.put(1, "A");
cache.put(2, "B");
cache.put(3, "C");
cache.get(1);  // 访问 1 使其变活跃
cache.put(4, "D");  // 超过容量，移除最久未访问的（2）
assertNull(cache.get(2));  // 2 被移除
assertNotNull(cache.get(1));
assertNotNull(cache.get(3));
assertNotNull(cache.get(4));
assertEquals(3, cache.size());
System.out.println("LRU 缓存: " + cache);
System.out.println("=== 测试通过 ===");
// 方法体结束
```
