---
name: PriorityQueue
package: java.util
order: 54
---

## 介绍

`java.util.PriorityQueue` 是基于**堆**（完全二叉树）实现的优先级队列，元素按自然顺序或指定比较器排序。头部总是最小的元素。

常见用途：
- **任务调度**：按优先级处理任务
- **Dijkstra 算法**：图最短路径
- **Top K 问题**：海量数据中取最大/最小的 K 个

## 方法

### PriorityQueue()

```java
public PriorityQueue()
```

创建默认容量（11）的优先级队列，元素按自然顺序排序。

### PriorityQueue(Comparator)

```java
public PriorityQueue(Comparator<? super E> comparator)
```

使用指定比较器创建优先级队列。

- **参数**: `comparator` — 比较器

### add

```java
public boolean add(E e)
```

添加元素。

- **参数**: `e` — 元素
- **返回**: `boolean`

### offer

```java
public boolean offer(E e)
```

添加元素。

- **参数**: `e` — 元素
- **返回**: `boolean`

### peek

```java
public E peek()
```

获取头部元素但不移除。队列空时返回 null。

- **返回**: `E`

### poll

```java
public E poll()
```

获取并移除头部元素。队列空时返回 null。

- **返回**: `E`

### remove

```java
public boolean remove(Object o)
```

移除指定元素。

- **参数**: `o` — 元素
- **返回**: `boolean`

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

### clear

```java
public void clear()
```

清空所有元素。

### contains

```java
public boolean contains(Object o)
```

判断是否包含指定元素。

- **参数**: `o` — 元素
- **返回**: `boolean`

## 测试

### PriorityQueue

- 描述: 创建优先队列并添加元素
- 断言: 头部总是最小元素

```java
// 方法体开始
System.out.println("=== priorityQueue ===");
PriorityQueue<Integer> pq = new PriorityQueue<>();
pq.add(5); pq.add(1); pq.add(3);
assertEquals(Integer.valueOf(1), pq.peek());
assertEquals(Integer.valueOf(1), pq.poll());
assertEquals(Integer.valueOf(3), pq.poll());
assertEquals(Integer.valueOf(5), pq.poll());
assertTrue(pq.isEmpty());
System.out.println("元素依次出队: 1, 3, 5");
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### offer

- 描述: offer 和 peek 操作
- 断言: peek 不改变队列

```java
// 方法体开始
System.out.println("=== offerAndPeek ===");
PriorityQueue<String> pq = new PriorityQueue<>();
assertTrue(pq.offer("b"));
assertTrue(pq.offer("a"));
assertEquals("a", pq.peek());
assertEquals("a", pq.peek());
assertEquals(2, pq.size());
System.out.println("peek 两次结果: " + pq.peek());
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### PriorityQueue(Comparator)

- 描述: 使用自定义比较器（最大堆）
- 断言: 最大元素先出队

```java
// 方法体开始
System.out.println("=== comparator ===");
PriorityQueue<Integer> pq = new PriorityQueue<>(Comparator.reverseOrder());
pq.add(1); pq.add(5); pq.add(3);
assertEquals(Integer.valueOf(5), pq.poll());
assertEquals(Integer.valueOf(3), pq.poll());
assertEquals(Integer.valueOf(1), pq.poll());
System.out.println("最大堆出队顺序: 5, 3, 1");
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### remove

- 描述: 移除元素
- 断言: 移除后不存在

```java
// 方法体开始
System.out.println("=== remove ===");
PriorityQueue<String> pq = new PriorityQueue<>();
pq.add("a"); pq.add("b"); pq.add("c");
assertTrue(pq.remove("b"));
assertEquals(2, pq.size());
assertFalse(pq.contains("b"));
System.out.println("移除 b 后: " + pq);
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### contains

- 描述: 判断是否包含
- 断言: 存在返回 true

```java
// 方法体开始
System.out.println("=== contains ===");
PriorityQueue<Integer> pq = new PriorityQueue<>();
pq.add(10); pq.add(20);
assertTrue(pq.contains(10));
assertFalse(pq.contains(30));
System.out.println("contains 10: " + pq.contains(10));
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### clear

- 描述: 清空队列
- 断言: clear 后为空

```java
// 方法体开始
System.out.println("=== clear ===");
PriorityQueue<String> pq = new PriorityQueue<>();
pq.add("a"); pq.add("b");
assertFalse(pq.isEmpty());
pq.clear();
assertTrue(pq.isEmpty());
assertEquals(0, pq.size());
System.out.println("clear 后 size: " + pq.size());
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### PriorityQueue

- 描述: 创建 PriorityQueue
- 断言: 对象不为 null

```java
// 方法体开始
System.out.println("=== PriorityQueue ===");
PriorityQueue<String> pq = new PriorityQueue<>();
assertNotNull(pq);
System.out.println("PriorityQueue: " + pq);
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### add

- 描述: 添加元素
- 断言: 添加后 size 增加

```java
// 方法体开始
System.out.println("=== add ===");
PriorityQueue<String> q = new PriorityQueue<>();
assertTrue(q.add("a"));
assertEquals(1, q.size());
System.out.println("add 后 size: " + q.size());
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### peek

- 描述: 查看头部
- 断言: 不移除元素

```java
// 方法体开始
System.out.println("=== peek ===");
PriorityQueue<Integer> q = new PriorityQueue<>();
q.add(5); q.add(1);
assertEquals(Integer.valueOf(1), q.peek());
assertEquals(2, q.size());
System.out.println("peek: " + q.peek());
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### poll

- 描述: 出队
- 断言: 返回头部并移除

```java
// 方法体开始
System.out.println("=== poll ===");
PriorityQueue<Integer> q = new PriorityQueue<>();
q.add(3); q.add(1); q.add(2);
assertEquals(Integer.valueOf(1), q.poll());
assertEquals(Integer.valueOf(2), q.poll());
System.out.println("poll 正确");
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### size

- 描述: 元素数量
- 断言: 数量正确

```java
// 方法体开始
System.out.println("=== size ===");
PriorityQueue<String> q = new PriorityQueue<>();
assertEquals(0, q.size());
q.add("a");
assertEquals(1, q.size());
System.out.println("size: " + q.size());
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### isEmpty

- 描述: 判断是否为空
- 断言: 空返回 true

```java
// 方法体开始
System.out.println("=== isEmpty ===");
PriorityQueue<String> q = new PriorityQueue<>();
assertTrue(q.isEmpty());
q.add("a");
assertFalse(q.isEmpty());
System.out.println("isEmpty: " + q.isEmpty());
System.out.println("=== 测试通过 ===");
// 方法体结束
```

