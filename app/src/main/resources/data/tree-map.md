---
name: TreeMap
package: java.util
order: 52
---

## 介绍

`java.util.TreeMap` 是基于**红黑树**的 `NavigableMap` 实现，键按自然顺序或指定比较器排序。所有操作的时间复杂度为 `O(log n)`。

常见用途：
- **排序 Map**：按键排序存储
- **范围查询**：`subMap`、`headMap`、`tailMap`
- **导航方法**：`lowerKey`、`higherKey`、`floorKey`、`ceilingKey`

## 方法

### TreeMap()

```java
public TreeMap()
```

创建按自然顺序排序的 TreeMap。

### put

```java
public V put(K key, V value)
```

添加键值对。

- **参数**: `key` — 键；`value` — 值
- **返回**: `V` — 旧值

### get

```java
public V get(Object key)
```

获取值。

- **参数**: `key` — 键
- **返回**: `V`

### remove

```java
public V remove(Object key)
```

移除键值对。

- **参数**: `key` — 键
- **返回**: `V` — 被移除的值

### firstKey

```java
public K firstKey()
```

返回最小的键。

- **返回**: `K`

### lastKey

```java
public K lastKey()
```

返回最大的键。

- **返回**: `K`

### lowerKey

```java
public K lowerKey(K key)
```

返回严格小于给定键的最大键。

- **参数**: `key` — 键
- **返回**: `K`

### higherKey

```java
public K higherKey(K key)
```

返回严格大于给定键的最小键。

- **参数**: `key` — 键
- **返回**: `K`

### floorKey

```java
public K floorKey(K key)
```

返回小于等于给定键的最大键。

- **参数**: `key` — 键
- **返回**: `K`

### ceilingKey

```java
public K ceilingKey(K key)
```

返回大于等于给定键的最小键。

- **参数**: `key` — 键
- **返回**: `K`

### subMap

```java
public SortedMap<K,V> subMap(K fromKey, K toKey)
```

返回 [fromKey, toKey) 范围的子 Map。

- **参数**: `fromKey` — 起始键；`toKey` — 结束键
- **返回**: `SortedMap<K,V>`

### headMap

```java
public SortedMap<K,V> headMap(K toKey)
```

返回小于 toKey 的子 Map。

- **参数**: `toKey` — 上界
- **返回**: `SortedMap<K,V>`

### tailMap

```java
public SortedMap<K,V> tailMap(K fromKey)
```

返回大于等于 fromKey 的子 Map。

- **参数**: `fromKey` — 下界
- **返回**: `SortedMap<K,V>`

### size

```java
public int size()
```

返回键值对数量。

- **返回**: `int`

### containsKey

```java
public boolean containsKey(Object key)
```

判断是否包含指定键。

- **参数**: `key` — 键
- **返回**: `boolean`

## 测试

### TreeMap

- 描述: 创建 TreeMap 并添加元素
- 断言: 按键自然排序

```java
// 方法体开始
System.out.println("=== treeMap ===");
TreeMap<String, Integer> map = new TreeMap<>();
map.put("b", 2);
map.put("a", 1);
map.put("c", 3);
assertEquals("{a=1, b=2, c=3}", map.toString());
assertEquals("a", map.firstKey());
assertEquals("c", map.lastKey());
System.out.println("TreeMap: " + map);
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### put

- 描述: 获取和移除元素
- 断言: 操作正确

```java
// 方法体开始
System.out.println("=== getAndRemove ===");
TreeMap<String, Integer> map = new TreeMap<>();
map.put("x", 10);
map.put("y", 20);
assertEquals(Integer.valueOf(10), map.get("x"));
assertEquals(Integer.valueOf(20), map.remove("y"));
assertNull(map.get("y"));
assertEquals(1, map.size());
System.out.println("get x: " + map.get("x") + ", remove y: " + map.get("y"));
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### get

- 描述: 导航键方法
- 断言: lowerKey/higherKey/floorKey/ceilingKey 正确

```java
// 方法体开始
System.out.println("=== navigationKeys ===");
TreeMap<Integer, String> map = new TreeMap<>();
map.put(10, "a");
map.put(20, "b");
map.put(30, "c");
assertEquals(Integer.valueOf(20), map.lowerKey(25));
assertEquals(Integer.valueOf(30), map.higherKey(25));
assertEquals(Integer.valueOf(20), map.floorKey(25));
assertEquals(Integer.valueOf(30), map.ceilingKey(25));
System.out.println("lowerKey(25): " + map.lowerKey(25) + ", higherKey(25): " + map.higherKey(25));
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### subMap

- 描述: 子 Map 范围查询
- 断言: subMap/headMap/tailMap 正确

```java
// 方法体开始
System.out.println("=== subMap ===");
TreeMap<Integer, String> map = new TreeMap<>();
map.put(1, "a"); map.put(2, "b"); map.put(3, "c"); map.put(4, "d"); map.put(5, "e");
assertEquals(2, map.subMap(2, 4).size());
assertEquals(2, map.headMap(3).size());
assertEquals(3, map.tailMap(3).size());
System.out.println("subMap(2,4): " + map.subMap(2, 4));
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### containsKey

- 描述: 判断键是否存在
- 断言: 存在返回 true

```java
// 方法体开始
System.out.println("=== containsKey ===");
TreeMap<String, Integer> map = new TreeMap<>();
map.put("key1", 100);
assertTrue(map.containsKey("key1"));
assertFalse(map.containsKey("key2"));
System.out.println("containsKey key1: " + map.containsKey("key1"));
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### comparator

- 描述: 使用自定义比较器
- 断言: 按自定义顺序排序

```java
// 方法体开始
System.out.println("=== comparator ===");
TreeMap<String, Integer> map = new TreeMap<>(Comparator.comparingInt(String::length));
map.put("aaa", 3);
map.put("a", 1);
map.put("bb", 2);
String first = map.firstKey();
assertEquals("a", first);
System.out.println("按长度排序: " + map);
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### TreeMap

- 描述: 创建 TreeMap
- 断言: 对象不为 null

```java
// 方法体开始
System.out.println("=== TreeMap ===");
TreeMap<String, Integer> map = new TreeMap<>();
assertNotNull(map);
System.out.println("TreeMap: " + map);
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### higherKey

- 描述: 大于给定键的最小键
- 断言: 结果正确

```java
// 方法体开始
System.out.println("=== higherKey ===");
TreeMap<Integer, String> map = new TreeMap<>();
map.put(10, "a"); map.put(20, "b"); map.put(30, "c");
assertEquals(Integer.valueOf(30), map.higherKey(25));
System.out.println("higherKey(25): " + map.higherKey(25));
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### floorKey

- 描述: 小于等于给定键的最大键
- 断言: 结果正确

```java
// 方法体开始
System.out.println("=== floorKey ===");
TreeMap<Integer, String> map = new TreeMap<>();
map.put(10, "a"); map.put(20, "b"); map.put(30, "c");
assertEquals(Integer.valueOf(20), map.floorKey(25));
System.out.println("floorKey(25): " + map.floorKey(25));
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### ceilingKey

- 描述: 大于等于给定键的最小键
- 断言: 结果正确

```java
// 方法体开始
System.out.println("=== ceilingKey ===");
TreeMap<Integer, String> map = new TreeMap<>();
map.put(10, "a"); map.put(20, "b"); map.put(30, "c");
assertEquals(Integer.valueOf(30), map.ceilingKey(25));
System.out.println("ceilingKey(25): " + map.ceilingKey(25));
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### headMap

- 描述: 头部子 Map
- 断言: 元素数量正确

```java
// 方法体开始
System.out.println("=== headMap ===");
TreeMap<Integer, String> map = new TreeMap<>();
map.put(1, "a"); map.put(2, "b"); map.put(3, "c");
assertEquals(2, map.headMap(3).size());
System.out.println("headMap(3): " + map.headMap(3));
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### tailMap

- 描述: 尾部子 Map
- 断言: 元素数量正确

```java
// 方法体开始
System.out.println("=== tailMap ===");
TreeMap<Integer, String> map = new TreeMap<>();
map.put(1, "a"); map.put(2, "b"); map.put(3, "c");
assertEquals(2, map.tailMap(2).size());
System.out.println("tailMap(2): " + map.tailMap(2));
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### size

- 描述: 键值对数量
- 断言: 数量正确

```java
// 方法体开始
System.out.println("=== size ===");
TreeMap<String, Integer> map = new TreeMap<>();
assertEquals(0, map.size());
map.put("a", 1);
assertEquals(1, map.size());
System.out.println("size: " + map.size());
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### firstKey

- 描述: 最小键
- 断言: 返回最小值

```java
// 方法体开始
System.out.println("=== firstKey ===");
TreeMap<String, Integer> map = new TreeMap<>();
map.put("b", 2); map.put("a", 1);
assertEquals("a", map.firstKey());
System.out.println("firstKey: " + map.firstKey());
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### lastKey

- 描述: 最大键
- 断言: 返回最大值

```java
// 方法体开始
System.out.println("=== lastKey ===");
TreeMap<String, Integer> map = new TreeMap<>();
map.put("a", 1); map.put("c", 3);
assertEquals("c", map.lastKey());
System.out.println("lastKey: " + map.lastKey());
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### remove

- 描述: 移除键值对
- 断言: 移除后不存在

```java
// 方法体开始
System.out.println("=== remove ===");
TreeMap<String, Integer> map = new TreeMap<>();
map.put("x", 10);
assertEquals(Integer.valueOf(10), map.remove("x"));
assertNull(map.get("x"));
System.out.println("remove 成功");
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### lowerKey

- 描述: 小于给定键的最大键
- 断言: 返回结果正确

```java
// 方法体开始
System.out.println("=== lowerKey ===");
TreeMap<Integer, String> map = new TreeMap<>();
map.put(10, "a"); map.put(20, "b"); map.put(30, "c");
assertEquals(Integer.valueOf(20), map.lowerKey(25));
assertEquals(Integer.valueOf(10), map.lowerKey(15));
System.out.println("lowerKey(25): " + map.lowerKey(25));
System.out.println("=== 测试通过 ===");
// 方法体结束
```
