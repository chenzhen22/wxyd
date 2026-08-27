---
name: ConcurrentLinkedQueue
package: java.util.concurrent
order: 148
---

## 介绍

`java.util.concurrent.ConcurrentLinkedQueue` 是 Java 5 引入的**无锁无界线程安全队列**，基于 Michael-Scott 算法实现的链表。在 Java 8 中获得了完整的 Stream 和 Collection 方法支持。

ConcurrentLinkedQueue 的核心特点：
- **无锁**：基于 CAS 操作，无阻塞算法
- **无界**：容量没有上限
- **FIFO**：先进先出顺序
- **弱一致性**：迭代器和 size() 是弱一致性的

## 方法

构造方法：
```java
public ConcurrentLinkedQueue()
public ConcurrentLinkedQueue(Collection<? extends E> c)
```

核心方法：
- `add(E)` / `offer(E)` — 插入元素
- `poll()` — 获取并移除头元素（空返回 null）
- `peek()` — 获取头元素不移除
- `remove(Object)` — 移除指定元素
- `size()` — 元素数量（O(n) 操作）
- `forEach(Consumer)` — Java 8 遍历

## 测试

### 基本操作

- 描述: ConcurrentLinkedQueue 的 FIFO 顺序
- 断言: 先入先出

```java
// 方法体开始
System.out.println("=== FIFO ===");
ConcurrentLinkedQueue<String> queue = new ConcurrentLinkedQueue<>();
queue.offer("A");
queue.offer("B");
queue.offer("C");
assertEquals("A", queue.poll());
assertEquals("B", queue.poll());
assertEquals("C", queue.poll());
assertNull(queue.poll());
System.out.println("FIFO 顺序正确");
System.out.println("=== 测试通过 ===");
// 方法体结束
```
