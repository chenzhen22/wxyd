---
name: HashSet
package: java.util
order: 177
---

## 介绍

`java.util.HashSet` 是基于 **HashMap 实现的 Set**，不保证迭代顺序。Java 8 获得了 Collection 接口的所有默认方法支持。

HashSet 的核心特点：
- **基于 HashMap**：内部使用 HashMap 实例
- **O(1) 操作**：add、remove、contains 均为 O(1)
- **无序**：不保证元素顺序
- **线程不安全**

## 方法

构造方法：
```java
public HashSet()
public HashSet(Collection<? extends E> c)
public HashSet(int initialCapacity)
```

Set 标准方法 + Java 8 默认方法：
- `forEach(Consumer)` — 遍历
- `removeIf(Predicate)` — 批量删除
- `stream()` / `parallelStream()` — Stream 支持

## 测试

### 基本操作

- 描述: HashSet 去重特性
- 断言: 重复元素不会被添加

```java
// 方法体开始
System.out.println("=== 去重 ===");
HashSet<String> set = new HashSet<>();
set.add("A");
set.add("B");
set.add("A");  // 重复
assertEquals(2, set.size());
assertTrue(set.contains("A"));
assertTrue(set.contains("B"));
System.out.println("set: " + set);
System.out.println("=== 测试通过 ===");
// 方法体结束
```
