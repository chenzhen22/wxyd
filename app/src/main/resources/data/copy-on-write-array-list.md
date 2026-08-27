---
name: CopyOnWriteArrayList
package: java.util.concurrent
order: 41
---

## 介绍

`java.util.concurrent.CopyOnWriteArrayList` 是 Java 5 引入的线程安全的 `List` 实现，在**读多写少**的场景下性能优异。它的实现原理是：每次修改（add、set、remove 等）都会创建底层数组的新副本，因此读操作不需要加锁，完全并发安全。

适用场景：
- **读多写少**：如事件监听器列表、缓存配置
- **遍历频繁**：`for-each` 或迭代器遍历时不需要加锁
- **不允许 ConcurrentModificationException**：迭代器遍历时可以并发修改

## 方法

### CopyOnWriteArrayList()

```java
public CopyOnWriteArrayList()
```

创建一个空的列表。

### CopyOnWriteArrayList(Collection)

```java
public CopyOnWriteArrayList(Collection<? extends E> c)
```

创建一个包含指定集合元素的列表。

### add

```java
public boolean add(E e)
```

添加元素到列表末尾。

- **参数**: `e` — 元素
- **返回**: `boolean` — 始终为 true

### add(int, E)

```java
public void add(int index, E element)
```

在指定位置插入元素。

- **参数**: `index` — 索引；`element` — 元素

### get

```java
public E get(int index)
```

获取指定位置的元素（无加锁，性能极高）。

- **参数**: `index` — 索引
- **返回**: `E`

### set

```java
public E set(int index, E element)
```

替换指定位置的元素。

- **参数**: `index` — 索引；`element` — 新元素
- **返回**: `E` — 旧元素

### remove

```java
public E remove(int index)
```

移除指定位置的元素。

- **参数**: `index` — 索引
- **返回**: `E` — 被移除的元素

### size

```java
public int size()
```

返回列表元素数量。

- **返回**: `int`

### isEmpty

```java
public boolean isEmpty()
```

判断列表是否为空。

- **返回**: `boolean`

### contains

```java
public boolean contains(Object o)
```

判断列表是否包含指定元素。

- **参数**: `o` — 元素
- **返回**: `boolean`

### clear

```java
public void clear()
```

清空列表。

### toArray

```java
public Object[] toArray()
```

转换为数组。

- **返回**: `Object[]`

### forEach

```java
public void forEach(Consumer<? super E> action)
```

遍历所有元素（Java 8 新增）。

- **参数**: `action` — 消费函数

## 测试

### CopyOnWriteArrayList

- 描述: 构造 CopyOnWriteArrayList
- 断言: 空列表 size 为 0

```java
// 方法体开始
System.out.println("=== CopyOnWriteArrayList ===");
CopyOnWriteArrayList<String> list = new CopyOnWriteArrayList<>();
assertEquals(0, list.size());
assertTrue(list.isEmpty());
System.out.println("空列表 size: " + list.size());
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### add

- 描述: 添加元素
- 断言: add 后 size 增加

```java
// 方法体开始
System.out.println("=== add ===");
CopyOnWriteArrayList<String> list = new CopyOnWriteArrayList<>();
assertTrue(list.add("a"));
assertTrue(list.add("b"));
assertEquals(2, list.size());
System.out.println("添加元素后 size: " + list.size());
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### get

- 描述: 获取元素
- 断言: get 返回正确元素

```java
// 方法体开始
System.out.println("=== get ===");
CopyOnWriteArrayList<String> list = new CopyOnWriteArrayList<>();
list.add("hello");
list.add("world");
assertEquals("hello", list.get(0));
assertEquals("world", list.get(1));
System.out.println("get(0): " + list.get(0) + ", get(1): " + list.get(1));
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### set

- 描述: 替换元素
- 断言: set 返回旧元素

```java
// 方法体开始
System.out.println("=== set ===");
CopyOnWriteArrayList<String> list = new CopyOnWriteArrayList<>();
list.add("old");
String old = list.set(0, "new");
assertEquals("old", old);
assertEquals("new", list.get(0));
System.out.println("旧值: " + old + ", 新值: " + list.get(0));
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### remove

- 描述: 移除元素
- 断言: remove 后 size 减小

```java
// 方法体开始
System.out.println("=== remove ===");
CopyOnWriteArrayList<String> list = new CopyOnWriteArrayList<>();
list.add("a");
list.add("b");
list.add("c");
String removed = list.remove(1);
assertEquals("b", removed);
assertEquals(2, list.size());
assertEquals("a", list.get(0));
assertEquals("c", list.get(1));
System.out.println("移除: " + removed + ", 剩余 size: " + list.size());
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### contains

- 描述: 判断是否包含元素
- 断言: 存在返回 true

```java
// 方法体开始
System.out.println("=== contains ===");
CopyOnWriteArrayList<String> list = new CopyOnWriteArrayList<>();
list.add("x");
list.add("y");
assertTrue(list.contains("x"));
assertTrue(list.contains("y"));
assertFalse(list.contains("z"));
System.out.println("包含 x: " + list.contains("x") + ", 包含 z: " + list.contains("z"));
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### clear

- 描述: 清空列表
- 断言: clear 后 size 为 0

```java
// 方法体开始
System.out.println("=== clear ===");
CopyOnWriteArrayList<String> list = new CopyOnWriteArrayList<>();
list.add("a");
list.add("b");
list.clear();
assertEquals(0, list.size());
assertTrue(list.isEmpty());
System.out.println("clear 后 size: " + list.size());
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### forEach

- 描述: 遍历所有元素
- 断言: 遍历到所有元素

```java
// 方法体开始
System.out.println("=== forEach ===");
CopyOnWriteArrayList<String> list = new CopyOnWriteArrayList<>();
list.add("a");
list.add("b");
list.add("c");
StringBuilder sb = new StringBuilder();
list.forEach(s -> sb.append(s));
assertEquals("abc", sb.toString());
System.out.println("forEach 结果: " + sb.toString());
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### size

- 描述: 获取元素数量
- 断言: size 返回正确

```java
// 方法体开始
System.out.println("=== size ===");
CopyOnWriteArrayList<String> list = new CopyOnWriteArrayList<>();
assertEquals(0, list.size());
list.add("a");
assertEquals(1, list.size());
System.out.println("size: " + list.size());
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### isEmpty

- 描述: 判断是否为空
- 断言: 空时返回 true

```java
// 方法体开始
System.out.println("=== isEmpty ===");
CopyOnWriteArrayList<String> list = new CopyOnWriteArrayList<>();
assertTrue(list.isEmpty());
list.add("a");
assertFalse(list.isEmpty());
System.out.println("isEmpty: " + list.isEmpty());
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### toArray

- 描述: 转换为数组
- 断言: 数组内容正确

```java
// 方法体开始
System.out.println("=== toArray ===");
CopyOnWriteArrayList<String> list = new CopyOnWriteArrayList<>();
list.add("a");
list.add("b");
Object[] arr = list.toArray();
assertEquals(2, arr.length);
assertEquals("a", arr[0]);
assertEquals("b", arr[1]);
System.out.println("toArray: " + java.util.Arrays.toString(arr));
System.out.println("=== 测试通过 ===");
// 方法体结束
```

