---
name: ArrayList
package: java.util
order: 175
---

## 介绍

`java.util.ArrayList` 是**可扩容数组实现的 List**，是 Java 中最常用的集合类。Java 8 获得了 Collection 和 Iterable 接口的所有默认方法支持。

ArrayList 的核心特点：
- **快速随机访问**：O(1) 的 get/set
- **尾部插入快**：尾部添加平均 O(1)，插入中间 O(n)
- **可扩容**：默认容量 10，满时自动扩容 50%
- **线程不安全**：需要外部同步

## 方法

构造方法：
```java
public ArrayList()
public ArrayList(int initialCapacity)
public ArrayList(Collection<? extends E> c)
```

List/Collection 接口方法 + Java 8 默认方法：
- `forEach(Consumer)` — 遍历
- `removeIf(Predicate)` — 批量删除
- `replaceAll(UnaryOperator)` — 批量替换
- `sort(Comparator)` — 排序

## 测试

### forEach / removeIf

- 描述: ArrayList 的 Java 8 方法
- 断言: 操作正确

```java
// 方法体开始
System.out.println("=== forEach/removeIf ===");
ArrayList<Integer> list = new ArrayList<>(Arrays.asList(1, 2, 3, 4, 5, 6));
list.removeIf(n -> n % 2 == 0);
assertEquals("[1, 3, 5]", list.toString());
int[] sum = {0};
list.forEach(n -> sum[0] += n);
assertEquals(9, sum[0]);
System.out.println("removeIf+forEach: " + list + ", sum=" + sum[0]);
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### replaceAll

- 描述: 使用 replaceAll 批量转换
- 断言: 所有元素翻倍

```java
// 方法体开始
System.out.println("=== replaceAll ===");
ArrayList<Integer> list = new ArrayList<>(Arrays.asList(1, 2, 3));
list.replaceAll(n -> n * 10);
assertEquals("[10, 20, 30]", list.toString());
System.out.println("replaceAll: " + list);
System.out.println("=== 测试通过 ===");
// 方法体结束
```
