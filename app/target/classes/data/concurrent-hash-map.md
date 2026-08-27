---
name: ConcurrentHashMap
package: java.util.concurrent
order: 16
---

## 介绍

`ConcurrentHashMap` 是 Java 5 引入的线程安全的 `HashMap` 实现，Java 8 重写了内部实现，采用 CAS + `synchronized` + 红黑树，替代了原来的分段锁（Segment）机制。

与 `Hashtable`（全表锁）和 `Collections.synchronizedMap()` 相比，`ConcurrentHashMap` 的优势：
- **高并发读**：读操作完全无锁
- **细粒度写**：仅锁住桶的头部节点，允许多线程并发写入不同桶
- **弱一致性迭代器**：迭代器不会抛出 `ConcurrentModificationException`
- **原子方法**：提供 `putIfAbsent`、`replace` 等原子复合操作

## 方法

### put

```java
public V put(K key, V value)
```

将指定键与值关联。如果键已存在，则替换旧值并返回旧值。

- **参数**: `key` — 键；`value` — 值
- **返回**: `V` — 旧值（如果键不存在则返回 null）

### get

```java
public V get(Object key)
```

返回指定键映射的值，如果不存在则返回 null。

- **参数**: `key` — 键
- **返回**: `V` — 对应的值，不存在时返回 null

### remove

```java
public V remove(Object key)
```

移除指定键的映射关系。

- **参数**: `key` — 键
- **返回**: `V` — 被移除的值（如果键不存在则返回 null）

### containsKey

```java
public boolean containsKey(Object key)
```

判断是否包含指定键。

- **参数**: `key` — 键
- **返回**: `boolean` — 如果包含该键则返回 true

### size

```java
public int size()
```

返回键值对的数量。

- **返回**: `int` — 键值对数量

### isEmpty

```java
public boolean isEmpty()
```

判断此映射是否不包含键值对。

- **返回**: `boolean` — 如果为空则返回 true

### clear

```java
public void clear()
```

移除所有键值对。

### putIfAbsent

```java
public V putIfAbsent(K key, V value)
```

如果指定键尚未与值关联（或被映射为 null），则将其与给定值关联。

- **参数**: `key` — 键；`value` — 值
- **返回**: `V` — 已存在的值（如果键不存在则返回 null）
- **说明**: 原子操作，比先检查再 put 更安全

### replace(K, V)

```java
public boolean replace(K key, V oldValue, V newValue)
```

仅当键当前映射为指定旧值时，才替换为新值。

- **参数**: `key` — 键；`oldValue` — 期望的旧值；`newValue` — 新值
- **返回**: `boolean` — 如果替换成功则返回 true

### replace(K, V)

```java
public V replace(K key, V value)
```

仅当键当前映射到某个值时，才替换为新值。

- **参数**: `key` — 键；`value` — 新值
- **返回**: `V` — 旧值（如果键不存在则返回 null）

### keySet

```java
public Set<K> keySet()
```

返回此映射中包含的键的 `Set` 视图。

- **返回**: `Set<K>` — 键集合

### values

```java
public Collection<V> values()
```

返回此映射中包含的值的 `Collection` 视图。

- **返回**: `Collection<V>` — 值集合

### entrySet

```java
public Set<Map.Entry<K, V>> entrySet()
```

返回此映射中包含的键值对的 `Set` 视图。

- **返回**: `Set<Map.Entry<K, V>>` — 键值对集合

## 测试

### put

- 描述: 测试 put 方法
- 断言: put 后 map 包含对应键值对，覆盖时返回旧值

```java
// 方法体开始
System.out.println("=== put ===");
ConcurrentHashMap<String, Integer> map = new ConcurrentHashMap<>();
Integer r1 = map.put("a", 1);
assertNull(r1);
assertEquals(Integer.valueOf(1), map.get("a"));
Integer r2 = map.put("a", 10);
assertEquals(Integer.valueOf(1), r2);
assertEquals(Integer.valueOf(10), map.get("a"));
System.out.println("put a=1（新键）返回: " + r1 + ", put a=10（覆盖）返回: " + r2 + ", 当前值: " + map.get("a"));
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### get

- 描述: 测试 get 方法
- 断言: get 返回对应的值，不存在时返回 null

```java
// 方法体开始
System.out.println("=== get ===");
ConcurrentHashMap<String, Integer> map = new ConcurrentHashMap<>();
map.put("a", 1);
map.put("b", 2);
assertEquals(Integer.valueOf(1), map.get("a"));
assertEquals(Integer.valueOf(2), map.get("b"));
assertNull(map.get("missing"));
System.out.println("get a: " + map.get("a") + ", get b: " + map.get("b") + ", get missing: " + map.get("missing"));
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### remove

- 描述: 测试 remove 方法
- 断言: remove 返回被移除的值，移除后 get 返回 null

```java
// 方法体开始
System.out.println("=== remove ===");
ConcurrentHashMap<String, Integer> map = new ConcurrentHashMap<>();
map.put("x", 100);
map.put("y", 200);
System.out.println("删除前: x=" + map.get("x") + ", y=" + map.get("y"));
Integer removed = map.remove("x");
System.out.println("remove x 返回: " + removed);
assertEquals(Integer.valueOf(100), removed);
assertNull(map.get("x"));
assertEquals(Integer.valueOf(200), map.get("y"));
Integer notExist = map.remove("z");
System.out.println("remove 不存在的键 z: " + notExist);
assertNull(notExist);
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### containsKey

- 描述: 测试 containsKey 方法
- 断言: 存在键时返回 true，不存在时返回 false

```java
// 方法体开始
System.out.println("=== containsKey ===");
ConcurrentHashMap<String, Integer> map = new ConcurrentHashMap<>();
map.put("hello", 1);
map.put("world", 2);
System.out.println("containsKey hello: " + map.containsKey("hello"));
assertTrue(map.containsKey("hello"));
System.out.println("containsKey world: " + map.containsKey("world"));
assertTrue(map.containsKey("world"));
System.out.println("containsKey missing: " + map.containsKey("missing"));
assertFalse(map.containsKey("missing"));
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### size

- 描述: 测试 size 方法
- 断言: size 返回正确的键值对数量

```java
// 方法体开始
System.out.println("=== size ===");
ConcurrentHashMap<String, Integer> map = new ConcurrentHashMap<>();
assertEquals(0, map.size());
map.put("a", 1);
assertEquals(1, map.size());
map.put("b", 2);
map.put("c", 3);
assertEquals(3, map.size());
map.remove("a");
assertEquals(2, map.size());
System.out.println("size 变化: 0 -> 1 -> 3 -> 2");
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### isEmpty

- 描述: 测试 isEmpty 方法
- 断言: 空 map 的 isEmpty 返回 true，添加元素后返回 false

```java
// 方法体开始
System.out.println("=== isEmpty ===");
ConcurrentHashMap<String, Integer> map = new ConcurrentHashMap<>();
assertTrue(map.isEmpty());
map.put("a", 1);
assertFalse(map.isEmpty());
System.out.println("isEmpty: 空=" + true + ", 添加后=" + false);
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### clear

- 描述: 测试 clear 方法
- 断言: clear 后 size 为 0，isEmpty 返回 true

```java
// 方法体开始
System.out.println("=== clear ===");
ConcurrentHashMap<String, Integer> map = new ConcurrentHashMap<>();
map.put("a", 1);
map.put("b", 2);
map.put("c", 3);
System.out.println("清空前 size: " + map.size());
assertEquals(3, map.size());
map.clear();
System.out.println("清空后 size: " + map.size());
assertEquals(0, map.size());
assertTrue(map.isEmpty());
assertNull(map.get("a"));
System.out.println("清空后 get a: " + map.get("a"));
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### putIfAbsent

- 描述: 测试 putIfAbsent 方法
- 断言: 键不存在时插入并返回 null，键存在时返回已有值且不覆盖

```java
// 方法体开始
System.out.println("=== putIfAbsent ===");
ConcurrentHashMap<String, Integer> map = new ConcurrentHashMap<>();
Integer r1 = map.putIfAbsent("key", 1);
System.out.println("putIfAbsent(key,1) 首次返回: " + r1);
assertNull(r1);
assertEquals(Integer.valueOf(1), map.get("key"));
Integer r2 = map.putIfAbsent("key", 999);
System.out.println("putIfAbsent(key,999) 已存在返回: " + r2 + ", 当前值仍为: " + map.get("key"));
assertEquals(Integer.valueOf(1), r2);
assertEquals(Integer.valueOf(1), map.get("key"));
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### replace

- 描述: 测试 replace 的两种形式
- 断言: replace(K, V) 替换已有键并返回旧值；replace(K, V, V) 条件替换成功返回 true

```java
// 方法体开始
System.out.println("=== replace ===");
ConcurrentHashMap<String, Integer> map = new ConcurrentHashMap<>();
map.put("count", 1);
System.out.println("初始 count: " + map.get("count"));
Integer old = map.replace("count", 10);
System.out.println("replace(count,10) 旧值: " + old + ", 新值: " + map.get("count"));
assertEquals(Integer.valueOf(1), old);
assertEquals(Integer.valueOf(10), map.get("count"));
Integer noKey = map.replace("missing", 100);
System.out.println("replace 不存在的键: " + noKey);
assertNull(noKey);
boolean cond1 = map.replace("count", 10, 100);
System.out.println("条件替换 count:10->100 结果: " + cond1 + ", 当前值: " + map.get("count"));
assertTrue(cond1);
assertEquals(Integer.valueOf(100), map.get("count"));
boolean cond2 = map.replace("count", 999, -1);
System.out.println("条件替换 count:999->-1（期望不匹配）结果: " + cond2 + ", 当前值: " + map.get("count"));
assertFalse(cond2);
assertEquals(Integer.valueOf(100), map.get("count"));
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### keySet

- 描述: 测试 keySet 方法
- 断言: keySet 返回所有键的集合

```java
// 方法体开始
System.out.println("=== keySet ===");
ConcurrentHashMap<String, Integer> map = new ConcurrentHashMap<>();
map.put("a", 1);
map.put("b", 2);
map.put("c", 3);
Set<String> keys = map.keySet();
assertEquals(3, keys.size());
assertTrue(keys.contains("a"));
assertTrue(keys.contains("b"));
assertTrue(keys.contains("c"));
System.out.println("keySet: " + keys);
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### values

- 描述: 测试 values 方法
- 断言: values 返回所有值的集合

```java
// 方法体开始
System.out.println("=== values ===");
ConcurrentHashMap<String, Integer> map = new ConcurrentHashMap<>();
map.put("a", 1);
map.put("b", 2);
map.put("c", 3);
Collection<Integer> vals = map.values();
assertEquals(3, vals.size());
assertTrue(vals.contains(1));
assertTrue(vals.contains(2));
assertTrue(vals.contains(3));
System.out.println("values: " + vals);
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### entrySet

- 描述: 测试 entrySet 方法
- 断言: entrySet 返回所有键值对的集合

```java
// 方法体开始
System.out.println("=== entrySet ===");
ConcurrentHashMap<String, Integer> map = new ConcurrentHashMap<>();
map.put("a", 1);
map.put("b", 2);
map.put("c", 3);
Set<Map.Entry<String, Integer>> entries = map.entrySet();
assertEquals(3, entries.size());
for (Map.Entry<String, Integer> entry : entries) {
    System.out.println("  " + entry.getKey() + " = " + entry.getValue());
    assertNotNull(entry.getKey());
    assertNotNull(entry.getValue());
}
System.out.println("=== 测试通过 ===");
// 方法体结束
```
