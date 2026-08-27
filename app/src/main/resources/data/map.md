---
name: Map
package: java.util
order: 145
---

## 介绍

`java.util.Map` 接口在 Java 8 中新增了多个实用的默认方法，大大提升了 Map 的易用性，特别是**Lambda 风格的键值操作**。

Java 8 新增的核心方法：
- `forEach(BiConsumer)` — 遍历所有键值对
- `computeIfAbsent(K, Function)` — 键不存在时计算并插入
- `computeIfPresent(K, BiFunction)` — 键存在时计算并替换
- `compute(K, BiFunction)` — 键计算并插入或删除
- `merge(K, V, BiFunction)` — 合并键值
- `putIfAbsent(K, V)` — 键不存在时放入
- `replaceAll(BiFunction)` — 批量替换所有值
- `remove(K, V)` — 仅当键值对匹配时删除
- `replace(K, V, V)` — 仅当旧值匹配时替换
- `getOrDefault(Object, V)` — 获取值或默认值

## 方法

### forEach

```java
default void forEach(BiConsumer<? super K, ? super V> action)
```

对所有键值对执行操作。

### computeIfAbsent

```java
default V computeIfAbsent(K key, Function<? super K, ? extends V> mappingFunction)
```

键不存在时计算新值并插入。

### merge

```java
default V merge(K key, V value, BiFunction<? super V, ? super V, ? extends V> remappingFunction)
```

将指定值合并到映射中。

### putIfAbsent

```java
default V putIfAbsent(K key, V value)
```

键不存在时放入。

### getOrDefault

```java
default V getOrDefault(Object key, V defaultValue)
```

获取值，不存在则返回默认值。

## 测试

### forEach

- 描述: 使用 Map.forEach 遍历
- 断言: 遍历所有键值对

```java
// 方法体开始
System.out.println("=== forEach ===");
Map<String, Integer> map = new HashMap<>();
map.put("A", 1);
map.put("B", 2);
map.put("C", 3);
int[] sum = {0};
map.forEach((k, v) -> sum[0] += v);
assertEquals(6, sum[0]);
System.out.println("forEach 求和: " + sum[0]);
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### computeIfAbsent

- 描述: 缓存模式
- 断言: 存在时不计算，不存在才计算

```java
// 方法体开始
System.out.println("=== computeIfAbsent ===");
Map<String, Integer> map = new HashMap<>();
map.put("A", 10);
Integer v1 = map.computeIfAbsent("A", k -> 100);
Integer v2 = map.computeIfAbsent("B", k -> 200);
assertEquals(Integer.valueOf(10), v1);  // 已有值
assertEquals(Integer.valueOf(200), v2); // 计算插入
assertEquals(2, map.size());
System.out.println("A=" + map.get("A") + ", B=" + map.get("B"));
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### merge

- 描述: 使用 merge 统计词频
- 断言: 重复 key 累加

```java
// 方法体开始
System.out.println("=== merge ===");
Map<String, Integer> map = new HashMap<>();
map.merge("apple", 1, Integer::sum);
map.merge("apple", 1, Integer::sum);
map.merge("banana", 1, Integer::sum);
assertEquals(Integer.valueOf(2), map.get("apple"));
assertEquals(Integer.valueOf(1), map.get("banana"));
System.out.println("merge 统计: " + map);
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### getOrDefault

- 描述: 使用 getOrDefault 安全取值
- 断言: 不存在时返回默认值

```java
// 方法体开始
System.out.println("=== getOrDefault ===");
Map<String, Integer> map = new HashMap<>();
map.put("A", 1);
assertEquals(Integer.valueOf(1), map.getOrDefault("A", 0));
assertEquals(Integer.valueOf(0), map.getOrDefault("B", 0));
System.out.println("A=" + map.getOrDefault("A", 0) + ", B=" + map.getOrDefault("B", 0));
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### replaceAll

- 描述: 使用 replaceAll 批量转换值
- 断言: 所有值翻倍

```java
// 方法体开始
System.out.println("=== replaceAll ===");
Map<String, Integer> map = new HashMap<>();
map.put("A", 1);
map.put("B", 2);
map.put("C", 3);
map.replaceAll((k, v) -> v * 10);
assertEquals(Integer.valueOf(10), map.get("A"));
assertEquals(Integer.valueOf(20), map.get("B"));
assertEquals(Integer.valueOf(30), map.get("C"));
System.out.println("replaceAll: " + map);
System.out.println("=== 测试通过 ===");
// 方法体结束
```
