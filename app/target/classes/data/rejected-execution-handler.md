---
name: RejectedExecutionHandler
package: java.util.concurrent
order: 367
---

## 介绍

`java.util.concurrent.RejectedExecutionHandler` 是**任务拒绝策略接口**，当线程池无法接受新任务时调用。

## 预定义策略

- `ThreadPoolExecutor.AbortPolicy` — 抛出异常（默认）
- `ThreadPoolExecutor.CallerRunsPolicy` — 调用者线程执行
- `ThreadPoolExecutor.DiscardPolicy` — 静默丢弃
- `ThreadPoolExecutor.DiscardOldestPolicy` — 丢弃最旧的

## 测试

- 描述: 自定义拒绝策略
- 断言: 策略生效

```java
// 方法体开始
System.out.println("=== RejectedExecutionHandler ===");
List<String> log = new ArrayList<>();
RejectedExecutionHandler handler = (r, executor) -> log.add("rejected");
ThreadPoolExecutor executor = new ThreadPoolExecutor(1, 1, 0, TimeUnit.SECONDS, 
    new SynchronousQueue<>(), handler);
executor.execute(() -> { try { Thread.sleep(500); } catch (Exception e) {} });
Thread.sleep(100);
executor.execute(() -> {});
executor.shutdown();
assertFalse(log.isEmpty());
System.out.println("拒绝策略已触发: " + log);
System.out.println("=== 测试通过 ===");
// 方法体结束
```
