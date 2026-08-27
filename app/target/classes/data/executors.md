---
name: Executors
package: java.util.concurrent
order: 21
---

## 介绍

`java.util.concurrent.Executors` 是 Java 5 引入的线程池工厂工具类，提供了创建各种线程池的静态方法。而 `ExecutorService` 是线程池的核心接口，定义了管理线程池生命周期的方法。

常见用途：
- **创建线程池**：`newFixedThreadPool`、`newCachedThreadPool`、`newSingleThreadExecutor`、`newScheduledThreadPool`
- **提交任务**：`submit(Callable)`、`submit(Runnable)`、`invokeAll`、`invokeAny`
- **管理生命周期**：`shutdown()`、`shutdownNow()`、`awaitTermination()`

## 方法

### newFixedThreadPool

```java
public static ExecutorService newFixedThreadPool(int nThreads)
```

创建固定线程数的线程池。如果所有线程都活跃，新任务在队列中等待。

- **参数**: `nThreads` — 线程数
- **返回**: `ExecutorService`

### newCachedThreadPool

```java
public static ExecutorService newCachedThreadPool()
```

创建可缓存的线程池，线程数量根据需要动态伸缩，空闲线程保留 60 秒。

- **返回**: `ExecutorService`

### newSingleThreadExecutor

```java
public static ExecutorService newSingleThreadExecutor()
```

创建单线程的线程池，所有任务在同一个线程中顺序执行。

- **返回**: `ExecutorService`

### newScheduledThreadPool

```java
public static ScheduledExecutorService newScheduledThreadPool(int corePoolSize)
```

创建支持定时和周期性任务执行的线程池。

- **参数**: `corePoolSize` — 核心线程数
- **返回**: `ScheduledExecutorService`

### execute(Runnable)

```java
void execute(Runnable command)
```

提交一个 Runnable 任务执行（定义在 Executor 接口）。

- **参数**: `command` — 可运行任务

### submit(Callable)

```java
<T> Future<T> submit(Callable<T> task)
```

提交一个 Callable 任务，返回 Future。

- **参数**: `task` — 有返回值的任务
- **返回**: `Future<T>`

### submit(Runnable)

```java
Future<?> submit(Runnable task)
```

提交一个 Runnable 任务，返回 Future（get() 返回 null）。

- **参数**: `task` — 可运行任务
- **返回**: `Future<?>`

### shutdown

```java
void shutdown()
```

平缓关闭线程池，已提交的任务继续执行，但不再接受新任务。

### shutdownNow

```java
List<Runnable> shutdownNow()
```

立即关闭线程池，尝试停止正在执行的任务，返回等待执行的任务列表。

- **返回**: `List<Runnable>` — 未执行的任务

### awaitTermination

```java
boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException
```

等待线程池关闭，直到超时或当前线程被中断。

- **参数**: `timeout` — 超时时间；`unit` — 时间单位
- **返回**: `boolean` — 是否已终止

### isShutdown

```java
boolean isShutdown()
```

判断线程池是否已关闭。

- **返回**: `boolean`

### isTerminated

```java
boolean isTerminated()
```

判断线程池是否已终止（所有任务完成后关闭）。

- **返回**: `boolean`

### invokeAll

```java
<T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks) throws InterruptedException
```

执行所有 Callable 任务，返回所有 Future 列表。

- **参数**: `tasks` — 任务集合
- **返回**: `List<Future<T>>`

### newSingleThreadScheduledExecutor

```java
public static ScheduledExecutorService newSingleThreadScheduledExecutor()
```

创建单线程的定时/周期性任务执行器。

- **返回**: `ScheduledExecutorService`

### schedule

```java
ScheduledFuture<?> schedule(Runnable command, long delay, TimeUnit unit)
```

在指定延迟后执行一次任务（定义在 ScheduledExecutorService）。

- **参数**: `command` — 任务；`delay` — 延迟时间；`unit` — 时间单位
- **返回**: `ScheduledFuture<?>`

### scheduleAtFixedRate

```java
ScheduledFuture<?> scheduleAtFixedRate(Runnable command, long initialDelay, long period, TimeUnit unit)
```

以固定频率执行任务，不管上一个任务是否完成。

- **参数**: `command` — 任务；`initialDelay` — 初始延迟；`period` — 周期；`unit` — 时间单位
- **返回**: `ScheduledFuture<?>`

## 测试

### newFixedThreadPool

- 描述: 创建固定线程池并执行任务
- 断言: 任务正常执行完成

```java
// 方法体开始
System.out.println("=== newFixedThreadPool ===");
ExecutorService executor = Executors.newFixedThreadPool(2);
try {
    Future<String> future = executor.submit(() -> "fixed pool result");
    assertEquals("fixed pool result", future.get());
    System.out.println("固定线程池执行结果: " + future.get());
} finally {
    executor.shutdown();
    executor.awaitTermination(1, TimeUnit.SECONDS);
}
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### newCachedThreadPool

- 描述: 创建缓存线程池并执行任务
- 断言: Callable 任务返回结果

```java
// 方法体开始
System.out.println("=== newCachedThreadPool ===");
ExecutorService executor = Executors.newCachedThreadPool();
try {
    Future<Integer> future = executor.submit(() -> 42);
    assertEquals(Integer.valueOf(42), future.get());
    System.out.println("缓存线程池结果: " + future.get());
} finally {
    executor.shutdown();
    executor.awaitTermination(1, TimeUnit.SECONDS);
}
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### newSingleThreadExecutor

- 描述: 创建单线程池，任务按顺序执行
- 断言: 任务按顺序完成

```java
// 方法体开始
System.out.println("=== newSingleThreadExecutor ===");
ExecutorService executor = Executors.newSingleThreadExecutor();
try {
    StringBuilder sb = new StringBuilder();
    executor.execute(() -> sb.append("A"));
    executor.execute(() -> sb.append("B"));
    executor.execute(() -> sb.append("C"));
    executor.shutdown();
    executor.awaitTermination(1, TimeUnit.SECONDS);
    assertEquals("ABC", sb.toString());
    System.out.println("单线程池执行顺序: " + sb.toString());
} finally {
    if (!executor.isShutdown()) executor.shutdownNow();
}
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### execute

- 描述: 提交 execute(Runnable) 任务
- 断言: 任务正常执行

```java
// 方法体开始
System.out.println("=== execute ===");
ExecutorService executor = Executors.newSingleThreadExecutor();
StringBuilder sb = new StringBuilder();
executor.execute(() -> sb.append("done"));
executor.shutdown();
executor.awaitTermination(1, TimeUnit.SECONDS);
assertEquals("done", sb.toString());
System.out.println("execute 结果: " + sb.toString());
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### submit

- 描述: 提交 Runnable 和 Callable 任务
- 断言: 任务正常执行

```java
// 方法体开始
System.out.println("=== submitRunnable ===");
ExecutorService executor = Executors.newSingleThreadExecutor();
try {
    StringBuilder sb = new StringBuilder();
    Future<?> future = executor.submit(() -> { sb.append("runnable done"); });
    assertNull(future.get());
    assertEquals("runnable done", sb.toString());
    System.out.println("Runnable 任务已执行: " + sb.toString());
} finally {
    executor.shutdown();
    executor.awaitTermination(1, TimeUnit.SECONDS);
}
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### isShutdown

- 描述: 判断线程池是否已关闭
- 断言: shutdown() 后 isShutdown() 返回 true

```java
// 方法体开始
System.out.println("=== isShutdown ===");
ExecutorService executor = Executors.newSingleThreadExecutor();
assertFalse(executor.isShutdown());
executor.shutdown();
assertTrue(executor.isShutdown());
System.out.println("shutdown 后 isShutdown: " + executor.isShutdown());
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### isTerminated

- 描述: 判断线程池是否已终止
- 断言: shutdown 并等待后 isTerminated 返回 true

```java
// 方法体开始
System.out.println("=== isTerminated ===");
ExecutorService executor = Executors.newSingleThreadExecutor();
executor.execute(() -> {});
executor.shutdown();
executor.awaitTermination(2, TimeUnit.SECONDS);
assertTrue(executor.isTerminated());
System.out.println("线程池已终止: " + executor.isTerminated());
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### invokeAll

- 描述: 批量提交多个 Callable 任务
- 断言: 所有任务执行完成

```java
// 方法体开始
System.out.println("=== invokeAll ===");
ExecutorService executor = Executors.newFixedThreadPool(3);
try {
    List<Callable<String>> tasks = Arrays.asList(
        () -> "A", () -> "B", () -> "C"
    );
    List<Future<String>> futures = executor.invokeAll(tasks);
    assertEquals(3, futures.size());
    assertEquals("A", futures.get(0).get());
    assertEquals("B", futures.get(1).get());
    assertEquals("C", futures.get(2).get());
    System.out.println("invokeAll 结果: " + futures.get(0).get() + futures.get(1).get() + futures.get(2).get());
} finally {
    executor.shutdown();
    executor.awaitTermination(1, TimeUnit.SECONDS);
}
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### newScheduledThreadPool

- 描述: 创建定时线程池并执行延迟任务
- 断言: 延迟执行后返回结果

```java
// 方法体开始
System.out.println("=== newScheduledThreadPool ===");
ScheduledExecutorService executor = Executors.newScheduledThreadPool(2);
try {
    StringBuilder sb = new StringBuilder();
    ScheduledFuture<?> future = executor.schedule(() -> sb.append("scheduled"), 50, TimeUnit.MILLISECONDS);
    future.get();
    assertEquals("scheduled", sb.toString());
    System.out.println("定时任务执行结果: " + sb.toString());
} finally {
    executor.shutdown();
    executor.awaitTermination(1, TimeUnit.SECONDS);
}
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### shutdown

- 描述: 平缓关闭线程池
- 断言: shutdown 后 isShutdown 返回 true

```java
// 方法体开始
System.out.println("=== shutdown ===");
ExecutorService executor = Executors.newSingleThreadExecutor();
assertFalse(executor.isShutdown());
executor.shutdown();
assertTrue(executor.isShutdown());
System.out.println("shutdown 后 isShutdown: " + executor.isShutdown());
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### shutdownNow

- 描述: 立即关闭线程池
- 断言: shutdownNow 后 isShutdown 返回 true

```java
// 方法体开始
System.out.println("=== shutdownNow ===");
ExecutorService executor = Executors.newSingleThreadExecutor();
executor.shutdownNow();
assertTrue(executor.isShutdown());
System.out.println("shutdownNow 后 isShutdown: " + executor.isShutdown());
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### awaitTermination

- 描述: 等待线程池终止
- 断言: 等待后线程池已终止

```java
// 方法体开始
System.out.println("=== awaitTermination ===");
ExecutorService executor = Executors.newSingleThreadExecutor();
executor.execute(() -> {});
executor.shutdown();
boolean terminated = executor.awaitTermination(2, TimeUnit.SECONDS);
assertTrue(terminated);
assertTrue(executor.isTerminated());
System.out.println("awaitTermination 结果: " + terminated);
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### newSingleThreadScheduledExecutor

- 描述: 创建单线程定时执行器
- 断言: 任务正常执行

```java
// 方法体开始
System.out.println("=== newSingleThreadScheduledExecutor ===");
ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
try {
    StringBuilder sb = new StringBuilder();
    ScheduledFuture<?> future = executor.schedule(() -> sb.append("delayed"), 50, TimeUnit.MILLISECONDS);
    future.get();
    assertEquals("delayed", sb.toString());
    System.out.println("单线程定时任务: " + sb.toString());
} finally {
    executor.shutdown();
    executor.awaitTermination(1, TimeUnit.SECONDS);
}
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### schedule

- 描述: 延迟执行一次任务
- 断言: 任务在延迟后执行

```java
// 方法体开始
System.out.println("=== schedule ===");
ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
try {
    StringBuilder sb = new StringBuilder();
    executor.schedule(() -> sb.append("one-shot"), 30, TimeUnit.MILLISECONDS).get();
    assertEquals("one-shot", sb.toString());
    System.out.println("延迟执行结果: " + sb.toString());
} finally {
    executor.shutdown();
    executor.awaitTermination(1, TimeUnit.SECONDS);
}
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### scheduleAtFixedRate

- 描述: 固定频率执行任务
- 断言: 任务至少执行了 2 次

```java
// 方法体开始
System.out.println("=== scheduleAtFixedRate ===");
ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
try {
    StringBuilder sb = new StringBuilder();
    ScheduledFuture<?> future = executor.scheduleAtFixedRate(() -> sb.append("x"), 0, 50, TimeUnit.MILLISECONDS);
    Thread.sleep(120);
    future.cancel(true);
    assertTrue(sb.length() >= 2);
    System.out.println("固定频率执行次数: " + sb.length());
} finally {
    executor.shutdown();
    executor.awaitTermination(1, TimeUnit.SECONDS);
}
System.out.println("=== 测试通过 ===");
// 方法体结束
```
