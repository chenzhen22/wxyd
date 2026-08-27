---
name: PriorityBlockingQueue
package: java.util.concurrent
order: 128
---

## 介绍

`java.util.concurrent.PriorityBlockingQueue` 是一个**无界的优先级阻塞队列**，基于二叉堆实现。元素按自然顺序或 Comparator 排序出队，是 `PriorityQueue` 的线程安全版本。

PriorityBlockingQueue 的核心特点：
- **线程安全**：所有操作都是线程安全的
- **无界**：容量没有上限（但可能耗尽内存）
- **优先级排序**：出队时总是返回优先级最高的元素
- **阻塞读取**：`take()` 在队列为空时阻塞等待

## 方法

构造方法：
```java
public PriorityBlockingQueue()
public PriorityBlockingQueue(int initialCapacity)
public PriorityBlockingQueue(int initialCapacity, Comparator<? super E> comparator)
public PriorityBlockingQueue(Collection<? extends E> c)
```

核心方法：
- `add(E)` / `offer(E)` — 插入元素
- `put(E)` — 插入元素（由于无界，不会阻塞）
- `take()` — 获取并移除头元素（空时阻塞）
- `poll()` / `poll(long, TimeUnit)` — 非阻塞/限时获取
- `peek()` — 获取头元素但不移除
- `size()` — 元素数量

## 测试

### 基本操作

- 描述: PriorityBlockingQueue 按优先级排序
- 断言: 出队顺序为升序

```java
// 方法体开始
System.out.println("=== 优先级排序 ===");
PriorityBlockingQueue<Integer> queue = new PriorityBlockingQueue<>();
queue.add(5);
queue.add(1);
queue.add(3);
queue.add(2);
queue.add(4);
List<Integer> result = new ArrayList<>();
queue.drainTo(result);
assertEquals("[1, 2, 3, 4, 5]", result.toString());
System.out.println("出队顺序: " + result);
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### 自定义比较器

- 描述: 使用 Comparator 实现降序优先级
- 断言: 大数优先出队

```java
// 方法体开始
System.out.println("=== 自定义比较器 ===");
PriorityBlockingQueue<Integer> queue = new PriorityBlockingQueue<>(11, Comparator.reverseOrder());
queue.add(10);
queue.add(30);
queue.add(20);
assertEquals(Integer.valueOf(30), queue.poll());
assertEquals(Integer.valueOf(20), queue.poll());
assertEquals(Integer.valueOf(10), queue.poll());
System.out.println("降序出队正确");
System.out.println("=== 测试通过 ===");
// 方法体结束
```
