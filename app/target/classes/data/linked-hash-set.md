---
name: LinkedHashSet
package: java.util
order: 165
---

## 介绍

`java.util.LinkedHashSet` 是**维护插入顺序的 Set**，在 HashSet 的基础上增加了双向链表维护迭代顺序。Java 8 获得了 Collection 所有默认方法的支持。

LinkedHashSet 的核心特点：
- **插入顺序**：迭代顺序与元素插入顺序一致
- **性能**：与 HashSet 接近，O(1) 的基本操作
- **线程不安全**：需要外部同步

## 方法

构造方法：
```java
public LinkedHashSet()
public LinkedHashSet(int initialCapacity)
public LinkedHashSet(Collection<? extends E> c)
```

继承自 HashSet/Collection 的标准方法。

## 测试

### 插入顺序

- 描述: LinkedHashSet 维护插入顺序
- 断言: 迭代顺序与插入顺序一致

```java
// 方法体开始
System.out.println("=== 插入顺序 ===");
LinkedHashSet<String> set = new LinkedHashSet<>();
set.add("C");
set.add("A");
set.add("B");
List<String> list = new ArrayList<>(set);
assertEquals("C", list.get(0));
assertEquals("A", list.get(1));
assertEquals("B", list.get(2));
System.out.println("插入顺序: " + list);
System.out.println("=== 测试通过 ===");
// 方法体结束
```
