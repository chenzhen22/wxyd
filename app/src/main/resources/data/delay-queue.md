---
name: DelayQueue
package: java.util.concurrent
order: 130
---

## 介绍

`java.util.concurrent.DelayQueue` 是一个**延迟阻塞队列**，只有元素延迟期满后才能被取出。元素必须实现 `Delayed` 接口。

DelayQueue 的核心特点：
- **延迟出队**：元素只有在其延迟时间过期后才能被取出
- **阻塞读取**：`take()` 阻塞直到有元素到期
- **优先级排序**：内部使用 PriorityQueue 排序，最早过期的元素在头部
- **无界队列**：容量没有上限，但过期的元素才能被移除

## 方法

构造方法：
```java
public DelayQueue()
public DelayQueue(Collection<? extends E> c)
```

核心方法：
- `put(E)` — 插入元素（无界，不会阻塞）
- `take()` — 获取并移除已过期的元素（阻塞）
- `poll()` — 非阻塞获取已过期的元素
- `peek()` — 获取头元素但不移除（可能未过期）
- `size()` — 元素数量（包含未过期元素）

## 测试

### 延迟队列基本功能

- 描述: 使用 DelayQueue 模拟延迟 200ms 的任务
- 断言: 取出时已经过至少 200ms

```java
// 方法体开始
System.out.println("=== 延迟队列 ===");
class DelayedItem implements Delayed {
    final String name;
    final long triggerTime;
    DelayedItem(String name, long delayMs) {
        this.name = name;
        this.triggerTime = System.currentTimeMillis() + delayMs;
    }
    public long getDelay(TimeUnit unit) {
        return unit.convert(triggerTime - System.currentTimeMillis(), TimeUnit.MILLISECONDS);
    }
    public int compareTo(Delayed other) {
        DelayedItem that = (DelayedItem) other;
        return Long.compare(this.triggerTime, that.triggerTime);
    }
}
DelayQueue<DelayedItem> queue = new DelayQueue<>();
queue.put(new DelayedItem("A", 200));
queue.put(new DelayedItem("B", 100));
queue.put(new DelayedItem("C", 300));
// B 最先到期（100ms），然后 A（200ms），然后 C（300ms）
List<String> result = new ArrayList<>();
for (int i = 0; i < 3; i++) {
    DelayedItem item = queue.take();
    result.add(item.name);
}
assertEquals("[B, A, C]", result.toString());
System.out.println("取出顺序: " + result);
System.out.println("=== 测试通过 ===");
// 方法体结束
```
