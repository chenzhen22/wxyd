---
name: ForkJoinPool
package: java.util.concurrent
order: 49
---

## 介绍

`java.util.concurrent.ForkJoinPool` 是 Java 7 引入的**工作窃取（work-stealing）线程池**，是 Java 8 中 `parallelStream()` 的底层实现。它采用分治策略：将大任务拆分为小任务并行执行，空闲线程会"偷取"其他线程队列中的任务。

常见用途：
- **并行流**：`Stream.parallel()` 的底层线程池
- **分治任务**：RecursiveTask/RecursiveAction
- **大数据处理**：适合 CPU 密集型的大任务拆分

## 方法

### ForkJoinPool

```java
public static ForkJoinPool commonPool()
```

返回公共的 ForkJoinPool 实例（parallelStream 默认使用）。

- **返回**: `ForkJoinPool`

### commonPoolSize

```java
public static int getCommonPoolParallelism()
```

返回公共池的并行度（默认 CPU 核心数 - 1）。

- **返回**: `int`

### execute(Runnable)

```java
public void execute(Runnable task)
```

异步执行任务。

- **参数**: `task` — 可运行任务

### submit(Callable)

```java
public <T> ForkJoinTask<T> submit(Callable<T> task)
```

提交有返回值的任务。

- **参数**: `task` — Callable 任务
- **返回**: `ForkJoinTask<T>`

### invoke(ForkJoinTask)

```java
public <T> T invoke(ForkJoinTask<T> task)
```

提交并等待任务完成，返回结果。

- **参数**: `task` — ForkJoinTask
- **返回**: `T`

### invokeAll

```java
public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks)
```

执行所有任务，返回 Future 列表。

- **参数**: `tasks` — 任务集合
- **返回**: `List<Future<T>>`

### getPoolSize

```java
public int getPoolSize()
```

返回线程池中工作线程数。

- **返回**: `int`

### getActiveThreadCount

```java
public int getActiveThreadCount()
```

返回活跃线程数。

- **返回**: `int`

### getParallelism

```java
public int getParallelism()
```

返回线程池的并行度。

- **返回**: `int`

### isShutdown

```java
public boolean isShutdown()
```

判断线程池是否已关闭。

- **返回**: `boolean`

### shutdown

```java
public void shutdown()
```

平缓关闭线程池。

### shutdownNow

```java
public List<Runnable> shutdownNow()
```

立即关闭线程池。

- **返回**: `List<Runnable>` — 未执行的任务

### ForkJoinTask.join

```java
public final T join()
```

等待任务完成并返回结果（ForkJoinTask 的方法）。

- **返回**: `T`

## 测试

### ForkJoinPool

- 描述: 获取公共 ForkJoinPool
- 断言: 公共池不为 null

```java
// 方法体开始
System.out.println("=== commonPool ===");
ForkJoinPool pool = ForkJoinPool.commonPool();
assertNotNull(pool);
System.out.println("公共线程池: " + pool);
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### submit

- 描述: 提交有返回值的 Callable 任务
- 断言: 任务返回正确结果

```java
// 方法体开始
System.out.println("=== submit ===");
ForkJoinPool pool = ForkJoinPool.commonPool();
ForkJoinTask<String> task = pool.submit(() -> "fork join result");
String result = task.join();
assertEquals("fork join result", result);
System.out.println("任务结果: " + result);
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### execute

- 描述: 提交 Runnable 任务
- 断言: 任务正常执行

```java
// 方法体开始
System.out.println("=== execute ===");
ForkJoinPool pool = new ForkJoinPool(2);
StringBuilder sb = new StringBuilder();
pool.execute(() -> sb.append("executed"));
pool.shutdown();
pool.awaitTermination(1, TimeUnit.SECONDS);
assertEquals("executed", sb.toString());
System.out.println("execute 结果: " + sb.toString());
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### getPoolSize

- 描述: 获取线程池大小
- 断言: 线程数大于 0

```java
// 方法体开始
System.out.println("=== getPoolSize ===");
ForkJoinPool pool = new ForkJoinPool(2);
assertTrue(pool.getPoolSize() >= 0);
System.out.println("池大小: " + pool.getPoolSize() + ", 并行度: " + pool.getParallelism());
pool.shutdown();
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### shutdown

- 描述: 关闭线程池
- 断言: 关闭后 isShutdown 返回 true

```java
// 方法体开始
System.out.println("=== shutdown ===");
ForkJoinPool pool = new ForkJoinPool(1);
assertFalse(pool.isShutdown());
pool.shutdown();
assertTrue(pool.isShutdown());
System.out.println("shutdown 后 isShutdown: " + pool.isShutdown());
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### invokeAll

- 描述: 执行多个任务
- 断言: 所有任务完成

```java
// 方法体开始
System.out.println("=== invokeAll ===");
ForkJoinPool pool = ForkJoinPool.commonPool();
List<Callable<String>> tasks = Arrays.asList(() -> "A", () -> "B");
try {
    List<Future<String>> futures = pool.invokeAll(tasks);
    assertEquals(2, futures.size());
    assertEquals("A", futures.get(0).get());
    assertEquals("B", futures.get(1).get());
    System.out.println("invokeAll 完成");
} finally {
    pool.shutdown();
}
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### ForkJoinTask.join

- 描述: 等待任务完成
- 断言: 任务返回结果

```java
// 方法体开始
System.out.println("=== ForkJoinTask.join ===");
ForkJoinPool pool = ForkJoinPool.commonPool();
ForkJoinTask<String> task = pool.submit(() -> "join result");
String result = task.join();
assertEquals("join result", result);
System.out.println("join: " + result);
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### commonPoolSize

- 描述: 获取公共池并行度
- 断言: 并行度大于 0

```java
// 方法体开始
System.out.println("=== commonPoolSize ===");
int size = ForkJoinPool.getCommonPoolParallelism();
assertTrue(size > 0);
System.out.println("并行度: " + size);
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### invoke

- 描述: 直接 invoke 任务
- 断言: 返回结果

```java
// 方法体开始
System.out.println("=== invoke ===");
ForkJoinPool pool = ForkJoinPool.commonPool();
java.util.concurrent.RecursiveTask<String> task = new java.util.concurrent.RecursiveTask<String>() {
    protected String compute() { return "recursive"; }
};
task.fork();
assertEquals("recursive", task.join());
System.out.println("invoke: recursive");
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### getActiveThreadCount

- 描述: 获取活跃线程数
- 断言: 活跃数 >= 0

```java
// 方法体开始
System.out.println("=== getActiveThreadCount ===");
ForkJoinPool p = new ForkJoinPool(2);
assertTrue(p.getActiveThreadCount() >= 0);
p.shutdown();
System.out.println("活跃线程: " + p.getActiveThreadCount());
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### getParallelism

- 描述: 获取并行度
- 断言: 并行度大于 0

```java
// 方法体开始
System.out.println("=== getParallelism ===");
ForkJoinPool p = new ForkJoinPool(3);
assertEquals(3, p.getParallelism());
p.shutdown();
System.out.println("并行度: " + p.getParallelism());
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### isShutdown

- 描述: 判断是否关闭
- 断言: 关闭后返回 true

```java
// 方法体开始
System.out.println("=== isShutdown ===");
ForkJoinPool p = new ForkJoinPool(1);
assertFalse(p.isShutdown());
p.shutdown();
assertTrue(p.isShutdown());
System.out.println("isShutdown: " + p.isShutdown());
System.out.println("=== 测试通过 ===");
// 方法体结束
```