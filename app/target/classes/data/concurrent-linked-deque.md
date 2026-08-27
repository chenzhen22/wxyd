---
name: ConcurrentLinkedDeque
package: java.util.concurrent
order: 115
---

## 介绍

`java.util.concurrent.ConcurrentLinkedDeque` 是 Java 7 引入的**并发双端队列**，但在 Java 8 中获得了新的 Stream 和 Collection 方法支持。它是基于链表的无界线程安全双端队列。

ConcurrentLinkedDeque 的核心特点：
- **线程安全**：无锁算法（CAS），高并发下性能优异
- **双端操作**：支持从头部和尾部插入/移除元素
- **无界队列**：不会因容量限制而阻塞
- **弱一致性**：迭代器和 size() 是弱一致性的

## 方法

构造方法：
```java
public ConcurrentLinkedDeque()
public ConcurrentLinkedDeque(Collection<? extends E> c)
```

核心双端队列方法：
- `addFirst(E)` / `addLast(E)` — 头部/尾部添加
- `getFirst()` / `getLast()` — 获取头部/尾部元素（不删除）
- `pollFirst()` / `pollLast()` — 获取并移除头部/尾部元素
- `removeFirstOccurrence(Object)` / `removeLastOccurrence(Object)` — 移除首次/末次出现的元素
- `descendingIterator()` — 逆序迭代器

## 测试

### 基本操作

- 描述: ConcurrentLinkedDeque 的基本双端操作
- 断言: 头部和尾部操作正常

```java
// 方法体开始
System.out.println("=== 基本操作 ===");
ConcurrentLinkedDeque<String> deque = new ConcurrentLinkedDeque<>();
deque.addLast("B");
deque.addLast("C");
deque.addFirst("A");
assertEquals("A", deque.getFirst());
assertEquals("C", deque.getLast());
assertEquals("[A, B, C]", deque.toString());
System.out.println("deque: " + deque);
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### pollFirst / pollLast

- 描述: 从两端移除元素
- 断言: 移除结果正确

```java
// 方法体开始
System.out.println("=== poll ===");
ConcurrentLinkedDeque<String> deque = new ConcurrentLinkedDeque<>();
deque.addLast("A");
deque.addLast("B");
deque.addLast("C");
assertEquals("A", deque.pollFirst());
assertEquals("C", deque.pollLast());
assertEquals("[B]", deque.toString());
System.out.println("移除后: " + deque);
System.out.println("=== 测试通过 ===");
// 方法体结束
```
