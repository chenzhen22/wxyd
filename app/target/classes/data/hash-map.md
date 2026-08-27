---
name: HashMap
package: java.util
order: 174
---

## 介绍

`java.util.HashMap` 是 Java 中最常用的哈希表实现。Java 8 对其内部实现进行了重大优化——当链表长度超过 `TREEIFY_THRESHOLD=8` 时，链表会转换为**红黑树**，使最坏情况从 O(n) 降到 O(log n)。

HashMap 在 Java 8 的优化：
- **红黑树化**：哈希冲突严重时，链表→红黑树
- **去重优化**：重写了 `hash()` 函数，高位参与运算减少碰撞
- **Map 默认方法**：forEach、computeIfAbsent、merge 等

## 方法

构造方法：
```java
public HashMap()
public HashMap(int initialCapacity, float loadFactor)
```

Map 接口的所有方法 + Java 8 默认方法。

## 测试

### 基本操作

- 描述: HashMap 的 put/get
- 断言: 存取正确

```java
// 方法体开始
System.out.println("=== 基本操作 ===");
HashMap<String, Integer> map = new HashMap<>();
map.put("A", 1);
map.put("B", 2);
assertEquals(Integer.valueOf(1), map.get("A"));
assertEquals(2, map.size());
System.out.println("map: " + map);
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### computeIfAbsent

- 描述: 使用 computeIfAbsent 延迟加载
- 断言: 仅缺失时计算

```java
// 方法体开始
System.out.println("=== computeIfAbsent ===");
HashMap<String, Integer> map = new HashMap<>();
map.put("A", 1);
map.computeIfAbsent("A", k -> 100);  // 已存在
map.computeIfAbsent("B", k -> 200);  // 不存在
assertEquals(Integer.valueOf(1), map.get("A"));
assertEquals(Integer.valueOf(200), map.get("B"));
System.out.println("computeIfAbsent: " + map);
System.out.println("=== 测试通过 ===");
// 方法体结束
```
