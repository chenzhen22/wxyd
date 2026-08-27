---
name: CompletionService
package: java.util.concurrent
order: 105
---

## 介绍

`java.util.concurrent.CompletionService` 是 Java 8 中用于**解耦异步任务提交与结果获取**的接口。它结合了 `Executor` 的功能和阻塞队列的管理，允许按任务完成顺序获取结果。

CompletionService 的核心特点：
- **生产-消费解耦**：提交任务后，通过 `take()` 按完成顺序获取结果
- **非阻塞提交**：`submit()` 立即返回
- **阻塞获取**：`take()` 在没有完成的任务时阻塞
- **完成顺序**：谁先完成谁先被取走，不同于 Future 数组的顺序遍历

CompletionService 在 Java 8 前就已存在，但在 Java 8 中增加了 `Future` 的新方法支持。常用的唯一实现是 `ExecutorCompletionService`。

## 方法

### submit(Callable)

```java
public Future<V> submit(Callable<V> task)
```

提交一个 Callable 任务。

### submit(Runnable, V)

```java
public Future<V> submit(Runnable task, V result)
```

提交一个 Runnable 任务并指定返回值。

### take

```java
public Future<V> take() throws InterruptedException
```

获取并移除下一个已完成任务的结果（阻塞等待）。

### poll

```java
public Future<V> poll()
public Future<V> poll(long timeout, TimeUnit unit) throws InterruptedException
```

获取并移除下一个已完成任务的结果（非阻塞/超时版本）。

## 测试

### 提交任务并获取结果

- 描述: 使用 ExecutorCompletionService 提交多个任务，按完成顺序获取结果
- 断言: 所有任务结果正确

```java
// 方法体开始
System.out.println("=== 提交并获取结果 ===");
ExecutorService executor = Executors.newFixedThreadPool(3);
CompletionService<String> cs = new ExecutorCompletionService<>(executor);
// 提交 3 个任务
cs.submit(() -> {
    Thread.sleep(200);
    return "任务1（最慢）";
});
cs.submit(() -> {
    Thread.sleep(50);
    return "任务2（最快）";
});
cs.submit(() -> {
    Thread.sleep(100);
    return "任务3";
});
// 按完成顺序获取
List<String> results = new ArrayList<>();
for (int i = 0; i < 3; i++) {
    Future<String> future = cs.take();
    results.add(future.get());
}
executor.shutdown();
assertEquals(3, results.size());
assertEquals("任务2（最快）", results.get(0));  // 最先完成
assertEquals("任务3", results.get(1));
assertEquals("任务1（最慢）", results.get(2));  // 最后完成
System.out.println("完成顺序: " + results);
System.out.println("=== 测试通过 ===");
// 方法体结束
```
