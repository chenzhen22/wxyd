---
name: LinkedTransferQueue
package: java.util.concurrent
order: 129
---

## 介绍

`java.util.concurrent.LinkedTransferQueue` 是 Java 7 引入的**高效无锁无界队列**，实现了 `TransferQueue` 接口。它结合了 `SynchronousQueue` 的直接传递特性和 `LinkedBlockingQueue` 的缓冲能力。

LinkedTransferQueue 的核心特点：
- **无锁**：基于 CAS 的链表实现，高并发性能优异
- **传输模式**：支持 `transfer()` 直接等待消费者
- **有界语义**：`tryTransfer()` 提供非阻塞传输尝试
- **弱一致性迭代器**：迭代器不会抛出 ConcurrentModificationException

## 方法

构造方法：
```java
public LinkedTransferQueue()
public LinkedTransferQueue(Collection<? extends E> c)
```

核心方法：
- `transfer(E)` — 传输元素，等待消费者
- `tryTransfer(E)` — 尝试传输，没有消费者等待则返回 false
- `tryTransfer(E, long, TimeUnit)` — 限时传输
- `hasWaitingConsumer()` — 是否有消费者在等待
- `getWaitingConsumerCount()` — 等待消费者数量

Queue 方法（`add`、`offer`、`poll`、`take` 等）也都支持。

## 测试

### 传输-接收

- 描述: 使用 transfer 在线程间直接传递元素
- 断言: 数据正确传递

```java
// 方法体开始
System.out.println("=== transfer ===");
LinkedTransferQueue<String> queue = new LinkedTransferQueue<>();
List<String> received = new ArrayList<>();
Thread consumer = new Thread(() -> {
    try {
        String data = queue.take();
        received.add(data);
    } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
    }
});
consumer.start();
queue.transfer("Hello from producer");
consumer.join();
assertEquals(1, received.size());
assertEquals("Hello from producer", received.get(0));
System.out.println("传输的数据: " + received.get(0));
System.out.println("=== 测试通过 ===");
// 方法体结束
```
