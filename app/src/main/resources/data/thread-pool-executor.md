---
name: ThreadPoolExecutor
package: java.util.concurrent
order: 152
---

## 介绍

`java.util.concurrent.ThreadPoolExecutor` 是 Java 中**线程池的核心实现**。Java 8 中它获得了 `allowCoreThreadTimeOut` 增强和 ForkJoinPool 风格的改进。

ThreadPoolExecutor 的核心参数：
- **corePoolSize** — 核心线程数
- **maximumPoolSize** — 最大线程数
- **keepAliveTime** — 空闲线程存活时间
- **workQueue** — 工作队列（BlockingQueue）
- **threadFactory** — 线程工厂
- **handler** — 拒绝策略

## 方法

构造方法：
```java
public ThreadPoolExecutor(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit, BlockingQueue<Runnable> workQueue)
```

核心方法：
- `execute(Runnable)` — 提交任务
- `submit(Callable)` / `submit(Runnable)` — 提交有返回值的任务
- `shutdown()` / `shutdownNow()` — 关闭线程池
- `getPoolSize()` / `getActiveCount()` — 监控方法

## 测试

### 基本使用

- 描述: 使用 ThreadPoolExecutor 执行任务
- 断言: 所有任务完成

```java
// 方法体开始
System.out.println("=== ThreadPoolExecutor ===");
ThreadPoolExecutor executor = new ThreadPoolExecutor(
        2, 4, 60, TimeUnit.SECONDS, new LinkedBlockingQueue<>());
List<Future<Integer>> futures = new ArrayList<>();
for (int i = 0; i < 5; i++) {
    final int taskId = i;
    futures.add(executor.submit(() -> taskId * 2));
}
int sum = 0;
for (Future<Integer> f : futures) {
    sum += f.get();
}
executor.shutdown();
assertEquals(20, sum);  // 0+2+4+6+8
System.out.println("所有任务完成，sum=" + sum);
System.out.println("=== 测试通过 ===");
// 方法体结束
```
