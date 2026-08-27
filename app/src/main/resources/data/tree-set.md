---
name: TreeSet
package: java.util
order: 53
---

## 介绍

`java.util.TreeSet` 是基于 `TreeMap` 的**排序集合**实现，元素按自然顺序或指定比较器排序。所有操作的时间复杂度为 `O(log n)`。

常见用途：
- **排序去重集合**：保持元素有序且唯一
- **范围查询**：`subSet`、`headSet`、`tailSet`
- **导航方法**：`lower`、`higher`、`floor`、`ceiling`

## 方法

### TreeSet()

```java
public TreeSet()
```

创建按自然顺序排序的 TreeSet。

### add

```java
public boolean add(E e)
```

添加元素。

- **参数**: `e` — 元素
- **返回**: `boolean`

### remove

```java
public boolean remove(Object o)
```

移除元素。

- **参数**: `o` — 元素
- **返回**: `boolean`

### first

```java
public E first()
```

返回最小的元素。

- **返回**: `E`

### last

```java
public E last()
```

返回最大的元素。

- **返回**: `E`

### lower

```java
public E lower(E e)
```

返回严格小于给定元素的最大元素。

- **参数**: `e` — 元素
- **返回**: `E`

### higher

```java
public E higher(E e)
```

返回严格大于给定元素的最小元素。

- **参数**: `e` — 元素
- **返回**: `E`

### floor

```java
public E floor(E e)
```

返回小于等于给定元素的最大元素。

- **参数**: `e` — 元素
- **返回**: `E`

### ceiling

```java
public E ceiling(E e)
```

返回大于等于给定元素的最小元素。

- **参数**: `e` — 元素
- **返回**: `E`

### add

```java
public SortedSet<E> subSet(E fromElement, E toElement)
```

返回 [from, to) 范围的子集合。

- **参数**: `fromElement` — 起始；`toElement` — 结束
- **返回**: `SortedSet<E>`

### size

```java
public int size()
```

返回元素数量。

- **返回**: `int`

### isEmpty

```java
public boolean isEmpty()
```

判断是否为空。

- **返回**: `boolean`

### contains

```java
public boolean contains(Object o)
```

判断是否包含指定元素。

- **参数**: `o` — 元素
- **返回**: `boolean`

## 测试

### TreeSet

- 描述: 创建 TreeSet 并添加元素
- 断言: 元素排序且无重复

```java
// 方法体开始
System.out.println("=== treeSet ===");
TreeSet<Integer> set = new TreeSet<>();
set.add(3); set.add(1); set.add(2); set.add(1);
assertEquals(3, set.size());
assertEquals(Integer.valueOf(1), set.first());
assertEquals(Integer.valueOf(3), set.last());
System.out.println("TreeSet: " + set);
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### remove

- 描述: 移除元素
- 断言: 移除后不存在

```java
// 方法体开始
System.out.println("=== remove ===");
TreeSet<String> set = new TreeSet<>();
set.add("a"); set.add("b"); set.add("c");
assertTrue(set.remove("b"));
assertFalse(set.contains("b"));
assertEquals(2, set.size());
System.out.println("移除 b 后: " + set);
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### first

- 描述: 导航方法
- 断言: lower/higher/floor/ceiling 正确

```java
// 方法体开始
System.out.println("=== navigation ===");
TreeSet<Integer> set = new TreeSet<>();
set.add(10); set.add(20); set.add(30);
assertEquals(Integer.valueOf(20), set.lower(25));
assertEquals(Integer.valueOf(30), set.higher(25));
assertEquals(Integer.valueOf(20), set.floor(25));
assertEquals(Integer.valueOf(30), set.ceiling(25));
System.out.println("lower/higher/floor/ceiling(25): " + set.lower(25) + "/" + set.higher(25) + "/" + set.floor(25) + "/" + set.ceiling(25));
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### add

- 描述: 子集合
- 断言: subSet 包含范围元素

```java
// 方法体开始
System.out.println("=== subSet ===");
TreeSet<Integer> set = new TreeSet<>();
for (int i = 1; i <= 5; i++) set.add(i);
assertEquals(2, set.subSet(2, 4).size());
assertEquals(2, set.headSet(3).size());
assertEquals(3, set.tailSet(3).size());
System.out.println("subSet(2,4): " + set.subSet(2, 4));
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### contains

- 描述: 判断是否包含元素
- 断言: 存在返回 true

```java
// 方法体开始
System.out.println("=== contains ===");
TreeSet<String> set = new TreeSet<>();
set.add("hello");
assertTrue(set.contains("hello"));
assertFalse(set.contains("world"));
System.out.println("contains hello: " + set.contains("hello"));
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### comparator

- 描述: 使用自定义比较器
- 断言: 按指定顺序排序

```java
// 方法体开始
System.out.println("=== comparator ===");
TreeSet<String> set = new TreeSet<>(Comparator.comparingInt(String::length));
set.add("aaa"); set.add("a"); set.add("bb");
assertEquals("a", set.first());
assertEquals("aaa", set.last());
System.out.println("按长度排序: " + set);
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### TreeSet

- 描述: 创建 TreeSet
- 断言: 对象不为 null

```java
// 方法体开始
System.out.println("=== TreeSet ===");
TreeSet<String> set = new TreeSet<>();
assertNotNull(set);
System.out.println("TreeSet: " + set);
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### last

- 描述: 最大元素
- 断言: 返回最大值

```java
// 方法体开始
System.out.println("=== last ===");
TreeSet<Integer> set = new TreeSet<>();
set.add(5); set.add(1);
assertEquals(Integer.valueOf(5), set.last());
System.out.println("last: " + set.last());
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### lower

- 描述: 小于给定值的最大元素
- 断言: 结果正确

```java
// 方法体开始
System.out.println("=== lower ===");
TreeSet<Integer> set = new TreeSet<>();
set.add(10); set.add(20); set.add(30);
assertEquals(Integer.valueOf(20), set.lower(25));
System.out.println("lower(25): " + set.lower(25));
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### higher

- 描述: 大于给定值的最小元素
- 断言: 结果正确

```java
// 方法体开始
System.out.println("=== higher ===");
TreeSet<Integer> set = new TreeSet<>();
set.add(10); set.add(20); set.add(30);
assertEquals(Integer.valueOf(30), set.higher(25));
System.out.println("higher(25): " + set.higher(25));
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### floor

- 描述: 小于等于给定值的最大元素
- 断言: 结果正确

```java
// 方法体开始
System.out.println("=== floor ===");
TreeSet<Integer> set = new TreeSet<>();
set.add(10); set.add(20); set.add(30);
assertEquals(Integer.valueOf(20), set.floor(25));
System.out.println("floor(25): " + set.floor(25));
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### ceiling

- 描述: 大于等于给定值的最小元素
- 断言: 结果正确

```java
// 方法体开始
System.out.println("=== ceiling ===");
TreeSet<Integer> set = new TreeSet<>();
set.add(10); set.add(20); set.add(30);
assertEquals(Integer.valueOf(30), set.ceiling(25));
System.out.println("ceiling(25): " + set.ceiling(25));
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### size

- 描述: 元素数量
- 断言: 数量正确

```java
// 方法体开始
System.out.println("=== size ===");
TreeSet<String> set = new TreeSet<>();
assertEquals(0, set.size());
set.add("a");
assertEquals(1, set.size());
System.out.println("size: " + set.size());
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### isEmpty

- 描述: 判断是否为空
- 断言: 空返回 true

```java
// 方法体开始
System.out.println("=== isEmpty ===");
TreeSet<String> set = new TreeSet<>();
assertTrue(set.isEmpty());
set.add("a");
assertFalse(set.isEmpty());
System.out.println("isEmpty: " + set.isEmpty());
System.out.println("=== 测试通过 ===");
// 方法体结束
```

