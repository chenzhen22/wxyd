---
name: SynchronousQueue
package: java.util.concurrent
order: 126
---

## 介绍

`java.util.concurrent.SynchronousQueue` 是一个**不存储元素的阻塞队列**。每个 `put` 操作必须等待一个 `take` 操作，反之亦然。它的容量为 0，本质上是一个线程间的手递手传递。

SynchronousQueue 的核心特点：
- **零容量**：内部不存储任何元素
- **直接传递**：生产者直接向消费者交付数据
- **公平模式**：可选的公平/非公平调度策略
- **配合线程池**：`Executors.newCachedThreadPool()` 内部使用它

## 方法

构造方法：
```java
public SynchronousQueue()
public SynchronousQueue(boolean fair)
```

核心方法：
- `put(E)` — 阻塞等待另一个线程 take
- `take()` — 阻塞等待另一个线程 put
- `offer(E)` — 非阻塞尝试放入（有等待线程才成功）
- `poll()` — 非阻塞尝试获取
- `offer(E, long, TimeUnit)` — 限时尝试放入
- `poll(long, TimeUnit)` — 限时尝试获取

## 测试

### put / take

- 描述: 使用 SynchronousQueue 在线程间传递数据
- 断言: 数据正确传递

```java
// 方法体开始
System.out.println("=== put/take ===");
SynchronousQueue<String> queue = new SynchronousQueue<>();
List<String> result = new ArrayList<>();
Thread t = new Thread(() -> {
    try {
        String data = queue.take();
        result.add(data);
    } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
    }
});
t.start();
queue.put("Hello");
t.join();
assertEquals(1, result.size());
assertEquals("Hello", result.get(0));
System.out.println("传递数据: " + result.get(0));
System.out.println("=== 测试通过 ===");
// 方法体结束
```
