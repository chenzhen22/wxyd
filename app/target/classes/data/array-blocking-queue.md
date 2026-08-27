---
name: ArrayBlockingQueue
package: java.util.concurrent
order: 151
---

## 介绍

`java.util.concurrent.ArrayBlockingQueue` 是**有界的并发阻塞队列**，基于数组实现。在 Java 8 中获得了完整的 Stream、Lambda 遍历支持。

ArrayBlockingQueue 的核心特点：
- **有界**：构造时指定容量，不会增长
- **可选公平**：支持公平/非公平的线程访问策略
- **阻塞操作**：`put()`/`take()` 在满/空时阻塞
- **一把锁**：使用单锁 + 两个条件实现

## 方法

构造方法：
```java
public ArrayBlockingQueue(int capacity)
public ArrayBlockingQueue(int capacity, boolean fair)
public ArrayBlockingQueue(int capacity, boolean fair, Collection<? extends E> c)
```

核心方法：
- `put(E)` — 满时阻塞添加
- `take()` — 空时阻塞获取
- `offer(E, long, TimeUnit)` — 限时添加
- `poll(long, TimeUnit)` — 限时获取
- `remainingCapacity()` — 剩余容量
- `drainTo(Collection)` — 批量移出元素

## 测试

### 基本阻塞操作

- 描述: ArrayBlockingQueue 的 put/take
- 断言: FIFO 顺序正确

```java
// 方法体开始
System.out.println("=== 阻塞队列 ===");
ArrayBlockingQueue<Integer> queue = new ArrayBlockingQueue<>(3);
queue.put(1);
queue.put(2);
queue.put(3);
assertEquals(Integer.valueOf(1), queue.take());
assertEquals(Integer.valueOf(2), queue.take());
assertEquals(Integer.valueOf(3), queue.take());
assertEquals(0, queue.size());
System.out.println("FIFO 顺序正确");
System.out.println("=== 测试通过 ===");
// 方法体结束
```
