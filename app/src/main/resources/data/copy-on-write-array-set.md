---
name: CopyOnWriteArraySet
package: java.util.concurrent
order: 116
---

## 介绍

`java.util.concurrent.CopyOnWriteArraySet` 是 Java 5 引入的**线程安全 Set** 实现，内部使用 `CopyOnWriteArrayList` 存储数据。在读多写少的场景下性能优异。

CopyOnWriteArraySet 的核心特点：
- **线程安全**：所有可变操作都会创建底层数组的新副本
- **读无锁**：读取操作不需要同步或加锁
- **适合读多写少**：迭代器视图的 snapshot 风格
- **弱一致性迭代器**：迭代时不会抛出 ConcurrentModificationException

## 方法

构造方法：
```java
public CopyOnWriteArraySet()
public CopyOnWriteArraySet(Collection<? extends E> c)
```

Set 标准方法：
- `add(E)` / `remove(Object)` — 添加/移除元素
- `contains(Object)` — 检查包含（效率低于 HashSet）
- `toArray()` / `toArray(T[])` — 转换为数组

## 测试

### 基本操作

- 描述: CopyOnWriteArraySet 的基本操作
- 断言: 添加和去重正常

```java
// 方法体开始
System.out.println("=== 基本操作 ===");
CopyOnWriteArraySet<String> set = new CopyOnWriteArraySet<>();
assertTrue(set.add("A"));
assertTrue(set.add("B"));
assertFalse(set.add("A"));  // 重复元素
assertEquals(2, set.size());
assertTrue(set.contains("A"));
assertFalse(set.contains("C"));
System.out.println("set: " + set);
System.out.println("=== 测试通过 ===");
// 方法体结束
```
