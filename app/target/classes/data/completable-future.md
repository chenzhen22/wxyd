---
name: CompletableFuture
package: java.util.concurrent
order: 1
---

## 介绍

`CompletableFuture<T>` 是 Java 8 引入的一个强大的异步编程工具类。它实现了 `Future<T>` 和 `CompletionStage<T>` 接口，提供了函数式编程风格的异步操作编排能力。

与传统的 `Future` 相比，`CompletableFuture` 支持：
- **显式完成**：手动设置结果或异常
- **回调驱动**：通过 `thenApply`、`thenAccept`、`thenRun` 等注册回调
- **组合编排**：链式调用多个异步操作
- **异常处理**：通过 `exceptionally`、`handle` 等处理异常
- **并行聚合**：组合多个异步任务的结果

## 方法

### completedFuture

```java
public static <U> CompletableFuture<U> completedFuture(U value)
```

返回一个已完成的 `CompletableFuture`，其结果值为给定值。

- **参数**: `value` — 结果值
- **返回**: `CompletableFuture<U>` — 已完成的 Future

### supplyAsync

```java
public static <U> CompletableFuture<U> supplyAsync(Supplier<U> supplier)
```

在 `ForkJoinPool.commonPool()` 中异步执行一个供给型函数，返回一个包含计算结果的 `CompletableFuture`。

- **参数**: `supplier` — 供给型函数，无输入，有返回值
- **返回**: `CompletableFuture<U>` — 包含异步计算结果的 Future

### supplyAsync(Executor)

```java
public static <U> CompletableFuture<U> supplyAsync(Supplier<U> supplier, Executor executor)
```

在指定线程池中异步执行一个供给型函数。

- **参数**: `supplier` — 供给型函数；`executor` — 线程池
- **返回**: `CompletableFuture<U>` — 包含异步计算结果的 Future

### runAsync

```java
public static CompletableFuture<Void> runAsync(Runnable runnable)
```

在 `ForkJoinPool.commonPool()` 中异步执行一个没有返回值的任务。

- **参数**: `runnable` — 可运行任务
- **返回**: `CompletableFuture<Void>` — 任务完成后返回 null

### runAsync(Executor)

```java
public static CompletableFuture<Void> runAsync(Runnable runnable, Executor executor)
```

在指定线程池中异步执行一个没有返回值的任务。

- **参数**: `runnable` — 可运行任务；`executor` — 线程池
- **返回**: `CompletableFuture<Void>`

### allOf

```java
public static CompletableFuture<Void> allOf(CompletableFuture<?>... cfs)
```

等待所有给定的 `CompletableFuture` 完成。如果任意一个异常完成，则返回的 Future 也异常完成。

- **参数**: `cfs` — 可变参数，多个 CompletableFuture
- **返回**: `CompletableFuture<Void>`

### anyOf

```java
public static CompletableFuture<Object> anyOf(CompletableFuture<?>... cfs)
```

等待任意一个给定的 `CompletableFuture` 完成，返回其结果。

- **参数**: `cfs` — 可变参数，多个 CompletableFuture
- **返回**: `CompletableFuture<Object>` — 最先完成的任务的结果

### complete

```java
public boolean complete(T value)
```

手动完成该 Future，设置其结果值。如果该 Future 已经被完成，则此调用无效。

- **参数**: `value` — 结果值
- **返回**: `boolean` — 如果此调用导致该 Future 完成则返回 true

### completeExceptionally

```java
public boolean completeExceptionally(Throwable ex)
```

手动异常完成该 Future。如果该 Future 已经被完成，则此调用无效。

- **参数**: `ex` — 异常对象
- **返回**: `boolean` — 如果此调用导致该 Future 异常完成则返回 true

### obtrudeValue

```java
public void obtrudeValue(T value)
```

强制设置结果值，即使该 Future 已经被完成。

- **参数**: `value` — 结果值
- **说明**: 无论 Future 是否已完成，都会覆盖设置结果

### obtrudeException

```java
public void obtrudeException(Throwable ex)
```

强制设置异常结果，即使该 Future 已经被完成。

- **参数**: `ex` — 异常对象
- **说明**: 无论 Future 是否已完成，都会覆盖设置异常

### isDone

```java
public boolean isDone()
```

如果该 Future 已完成（正常、异常或取消），则返回 true。

- **返回**: `boolean`

### isCancelled

```java
public boolean isCancelled()
```

如果该 Future 在正常完成前被取消，则返回 true。

- **返回**: `boolean`

### isCompletedExceptionally

```java
public boolean isCompletedExceptionally()
```

如果该 Future 异常完成，则返回 true。

- **返回**: `boolean`

### cancel

```java
public boolean cancel(boolean mayInterruptIfRunning)
```

尝试取消该 Future 的执行。

- **参数**: `mayInterruptIfRunning` — 是否允许中断正在执行的线程
- **返回**: `boolean` — 如果取消失败则返回 false

### get

```java
public T get() throws InterruptedException, ExecutionException
```

阻塞等待 Future 完成，并返回结果。

- **返回**: `T` — 计算结果
- **异常**: `InterruptedException` — 当前线程被中断；`ExecutionException` — 计算抛出异常

### get(timeout, unit)

```java
public T get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException
```

在指定时间内阻塞等待 Future 完成，超时则抛出 `TimeoutException`。

- **参数**: `timeout` — 超时时间；`unit` — 时间单位
- **返回**: `T` — 计算结果
- **异常**: `TimeoutException` — 超时

### join

```java
public T join()
```

等待 Future 完成并返回结果，与 `get()` 的区别是 `join()` 不抛出受检异常。

- **返回**: `T` — 计算结果
- **异常**: `CancellationException` — 任务被取消；`CompletionException` — 计算抛出异常

### getNow

```java
public T getNow(T valueIfAbsent)
```

如果已完成则返回结果，否则返回给定的默认值。

- **参数**: `valueIfAbsent` — 默认值
- **返回**: `T` — 结果值或默认值

### thenApply

```java
public <U> CompletableFuture<U> thenApply(Function<? super T,? extends U> fn)
```

当上一个阶段正常完成时，对其结果应用给定函数，返回一个新的 `CompletableFuture`。

- **参数**: `fn` — 转换函数，接收上一个阶段的结果，返回新值
- **返回**: `CompletableFuture<U>` — 转换后的结果

### thenApplyAsync

```java
public <U> CompletableFuture<U> thenApplyAsync(Function<? super T,? extends U> fn)
```

异步地对其结果应用给定函数，在 `ForkJoinPool` 中执行。

- **参数**: `fn` — 转换函数
- **返回**: `CompletableFuture<U>`

### thenApplyAsync(Executor)

```java
public <U> CompletableFuture<U> thenApplyAsync(Function<? super T,? extends U> fn, Executor executor)
```

在指定线程池中异步地对其结果应用给定函数。

- **参数**: `fn` — 转换函数；`executor` — 线程池
- **返回**: `CompletableFuture<U>`

### thenAccept

```java
public CompletableFuture<Void> thenAccept(Consumer<? super T> action)
```

当上一个阶段正常完成时，消费其结果，不返回新值。

- **参数**: `action` — 消费型函数，接收结果，无返回值
- **返回**: `CompletableFuture<Void>`

### thenAcceptAsync

```java
public CompletableFuture<Void> thenAcceptAsync(Consumer<? super T> action)
```

异步地消费其结果。

- **参数**: `action` — 消费型函数
- **返回**: `CompletableFuture<Void>`

### thenAcceptAsync(Executor)

```java
public CompletableFuture<Void> thenAcceptAsync(Consumer<? super T> action, Executor executor)
```

在指定线程池中异步地消费其结果。

- **参数**: `action` — 消费型函数；`executor` — 线程池
- **返回**: `CompletableFuture<Void>`

### thenRun

```java
public CompletableFuture<Void> thenRun(Runnable action)
```

当上一个阶段正常完成时，执行给定的操作，不依赖前一个阶段的结果。

- **参数**: `action` — 可运行操作
- **返回**: `CompletableFuture<Void>`

### thenRunAsync

```java
public CompletableFuture<Void> thenRunAsync(Runnable action)
```

异步地执行给定的操作。

- **参数**: `action` — 可运行操作
- **返回**: `CompletableFuture<Void>`

### thenRunAsync(Executor)

```java
public CompletableFuture<Void> thenRunAsync(Runnable action, Executor executor)
```

在指定线程池中异步地执行给定的操作。

- **参数**: `action` — 可运行操作；`executor` — 线程池
- **返回**: `CompletableFuture<Void>`

### thenCompose

```java
public <U> CompletableFuture<U> thenCompose(Function<? super T,? extends CompletionStage<U>> fn)
```

当上一个阶段正常完成时，对其结果应用返回 `CompletionStage` 的函数——与 `thenApply` 的区别在于 `thenCompose` 可以扁平化嵌套的 Future。

- **参数**: `fn` — 返回 CompletionStage 的函数
- **返回**: `CompletableFuture<U>`
- **说明**: 类似于 `Stream.flatMap`，用于避免 `CompletableFuture<CompletableFuture<U>>` 的嵌套

### thenComposeAsync

```java
public <U> CompletableFuture<U> thenComposeAsync(Function<? super T,? extends CompletionStage<U>> fn)
```

异步地执行扁平化组合操作。

- **参数**: `fn` — 返回 CompletionStage 的函数
- **返回**: `CompletableFuture<U>`

### thenComposeAsync(Executor)

```java
public <U> CompletableFuture<U> thenComposeAsync(Function<? super T,? extends CompletionStage<U>> fn, Executor executor)
```

在指定线程池中异步地执行扁平化组合操作。

- **参数**: `fn` — 返回 CompletionStage 的函数；`executor` — 线程池
- **返回**: `CompletableFuture<U>`

### thenCombine

```java
public <U,V> CompletableFuture<V> thenCombine(CompletionStage<? extends U> other, BiFunction<? super T,? super U,? extends V> fn)
```

合并两个独立的 `CompletionStage` 的结果，当两者都完成时应用给定函数。

- **参数**: `other` — 另一个 CompletionStage；`fn` — 合并函数
- **返回**: `CompletableFuture<V>` — 合并后的结果

### thenCombineAsync

```java
public <U,V> CompletableFuture<V> thenCombineAsync(CompletionStage<? extends U> other, BiFunction<? super T,? super U,? extends V> fn)
```

异步地合并两个结果。

- **参数**: `other` — 另一个 CompletionStage；`fn` — 合并函数
- **返回**: `CompletableFuture<V>`

### thenCombineAsync(Executor)

```java
public <U,V> CompletableFuture<V> thenCombineAsync(CompletionStage<? extends U> other, BiFunction<? super T,? super U,? extends V> fn, Executor executor)
```

在指定线程池中异步地合并两个结果。

- **参数**: `other` — 另一个 CompletionStage；`fn` — 合并函数；`executor` — 线程池
- **返回**: `CompletableFuture<V>`

### thenAcceptBoth

```java
public <U> CompletableFuture<Void> thenAcceptBoth(CompletionStage<? extends U> other, BiConsumer<? super T,? super U> action)
```

当两个 `CompletionStage` 都完成时，消费两个结果。

- **参数**: `other` — 另一个 CompletionStage；`action` — 消费两个参数的函数
- **返回**: `CompletableFuture<Void>`

### thenAcceptBothAsync

```java
public <U> CompletableFuture<Void> thenAcceptBothAsync(CompletionStage<? extends U> other, BiConsumer<? super T,? super U> action)
```

异步地消费两个结果。

- **参数**: `other` — 另一个 CompletionStage；`action` — 消费函数
- **返回**: `CompletableFuture<Void>`

### thenAcceptBothAsync(Executor)

```java
public <U> CompletableFuture<Void> thenAcceptBothAsync(CompletionStage<? extends U> other, BiConsumer<? super T,? super U> action, Executor executor)
```

在指定线程池中异步地消费两个结果。

- **参数**: `other` — 另一个 CompletionStage；`action` — 消费函数；`executor` — 线程池
- **返回**: `CompletableFuture<Void>`

### runAfterBoth

```java
public CompletableFuture<Void> runAfterBoth(CompletionStage<?> other, Runnable action)
```

当两个 `CompletionStage` 都完成时，执行给定的操作。

- **参数**: `other` — 另一个 CompletionStage；`action` — 可运行操作
- **返回**: `CompletableFuture<Void>`

### runAfterBothAsync

```java
public CompletableFuture<Void> runAfterBothAsync(CompletionStage<?> other, Runnable action)
```

异步地执行操作。

- **参数**: `other` — 另一个 CompletionStage；`action` — 可运行操作
- **返回**: `CompletableFuture<Void>`

### runAfterBothAsync(Executor)

```java
public CompletableFuture<Void> runAfterBothAsync(CompletionStage<?> other, Runnable action, Executor executor)
```

在指定线程池中异步地执行操作。

- **参数**: `other` — 另一个 CompletionStage；`action` — 可运行操作；`executor` — 线程池
- **返回**: `CompletableFuture<Void>`

### applyToEither

```java
public <U> CompletableFuture<U> applyToEither(CompletionStage<? extends T> other, Function<? super T, U> fn)
```

当任意一个 `CompletionStage` 完成时，将函数应用于其结果。

- **参数**: `other` — 另一个 CompletionStage；`fn` — 转换函数
- **返回**: `CompletableFuture<U>`

### applyToEitherAsync

```java
public <U> CompletableFuture<U> applyToEitherAsync(CompletionStage<? extends T> other, Function<? super T, U> fn)
```

异步地执行"任一完成"转换。

- **参数**: `other` — 另一个 CompletionStage；`fn` — 转换函数
- **返回**: `CompletableFuture<U>`

### applyToEitherAsync(Executor)

```java
public <U> CompletableFuture<U> applyToEitherAsync(CompletionStage<? extends T> other, Function<? super T, U> fn, Executor executor)
```

在指定线程池中异步地执行"任一完成"转换。

- **参数**: `other` — 另一个 CompletionStage；`fn` — 转换函数；`executor` — 线程池
- **返回**: `CompletableFuture<U>`

### acceptEither

```java
public CompletableFuture<Void> acceptEither(CompletionStage<? extends T> other, Consumer<? super T> action)
```

当任意一个 `CompletionStage` 完成时，消费其结果。

- **参数**: `other` — 另一个 CompletionStage；`action` — 消费函数
- **返回**: `CompletableFuture<Void>`

### acceptEitherAsync

```java
public CompletableFuture<Void> acceptEitherAsync(CompletionStage<? extends T> other, Consumer<? super T> action)
```

异步地消费"任一完成"的结果。

- **参数**: `other` — 另一个 CompletionStage；`action` — 消费函数
- **返回**: `CompletableFuture<Void>`

### acceptEitherAsync(Executor)

```java
public CompletableFuture<Void> acceptEitherAsync(CompletionStage<? extends T> other, Consumer<? super T> action, Executor executor)
```

在指定线程池中异步地消费"任一完成"的结果。

- **参数**: `other` — 另一个 CompletionStage；`action` — 消费函数；`executor` — 线程池
- **返回**: `CompletableFuture<Void>`

### runAfterEither

```java
public CompletableFuture<Void> runAfterEither(CompletionStage<?> other, Runnable action)
```

当任意一个 `CompletionStage` 完成时，执行给定操作。

- **参数**: `other` — 另一个 CompletionStage；`action` — 可运行操作
- **返回**: `CompletableFuture<Void>`

### runAfterEitherAsync

```java
public CompletableFuture<Void> runAfterEitherAsync(CompletionStage<?> other, Runnable action)
```

异步地执行"任一完成"操作。

- **参数**: `other` — 另一个 CompletionStage；`action` — 可运行操作
- **返回**: `CompletableFuture<Void>`

### runAfterEitherAsync(Executor)

```java
public CompletableFuture<Void> runAfterEitherAsync(CompletionStage<?> other, Runnable action, Executor executor)
```

在指定线程池中异步地执行"任一完成"操作。

- **参数**: `other` — 另一个 CompletionStage；`action` — 可运行操作；`executor` — 线程池
- **返回**: `CompletableFuture<Void>`

### exceptionally

```java
public CompletableFuture<T> exceptionally(Function<Throwable, ? extends T> fn)
```

当上一个阶段异常完成时，用给定函数从异常中恢复。

- **参数**: `fn` — 异常恢复函数，接收异常对象，返回替代结果
- **返回**: `CompletableFuture<T>`

### handle

```java
public <U> CompletableFuture<U> handle(BiFunction<? super T, Throwable, ? extends U> fn)
```

无论上一个阶段正常还是异常完成，都对结果或异常应用给定函数。

- **参数**: `fn` — 处理函数，接收结果和异常
- **返回**: `CompletableFuture<U>`
- **说明**: 类似于 `finally` 块，无论成功还是失败都会执行

### handleAsync

```java
public <U> CompletableFuture<U> handleAsync(BiFunction<? super T, Throwable, ? extends U> fn)
```

异步地处理结果或异常。

- **参数**: `fn` — 处理函数
- **返回**: `CompletableFuture<U>`

### handleAsync(Executor)

```java
public <U> CompletableFuture<U> handleAsync(BiFunction<? super T, Throwable, ? extends U> fn, Executor executor)
```

在指定线程池中异步地处理结果或异常。

- **参数**: `fn` — 处理函数；`executor` — 线程池
- **返回**: `CompletableFuture<U>`

### whenComplete

```java
public CompletableFuture<T> whenComplete(BiConsumer<? super T, ? super Throwable> action)

```

无论正常还是异常完成，都在完成时执行给定操作。不改变结果值。

- **参数**: `action` — 回调函数，接收结果和异常
- **返回**: `CompletableFuture<T>` — 不改变结果的 Future

### whenCompleteAsync

```java
public CompletableFuture<T> whenCompleteAsync(BiConsumer<? super T, ? super Throwable> action)
```

异步地执行完成回调。

- **参数**: `action` — 回调函数
- **返回**: `CompletableFuture<T>`

### whenCompleteAsync(Executor)

```java
public CompletableFuture<T> whenCompleteAsync(BiConsumer<? super T, ? super Throwable> action, Executor executor)
```

在指定线程池中异步地执行完成回调。

- **参数**: `action` — 回调函数；`executor` — 线程池
- **返回**: `CompletableFuture<T>`

### toString

```java
public String toString()
```

返回该 `CompletableFuture` 的字符串表示，包括完成状态。

- **返回**: `String`

## 测试

### supplyAsync

- 描述: 基本异步任务（含 supplyAsync / supplyAsync(Executor)）
- 断言: supplyAsync + join() 结果等于 "Hello, CompletableFuture!"

```java
// 方法体开始
System.out.println("=== supplyAsync ===");
System.out.println("创建异步任务: CompletableFuture.supplyAsync");
CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> "Hello, CompletableFuture!");
String result = future.join();
System.out.println("异步任务结果: " + result);
assertEquals("Hello, CompletableFuture!", result);
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### thenApply

- 描述: 链式转换（含 thenApply / thenApplyAsync / thenApplyAsync(Executor)）
- 断言: supplyAsync + thenApply 转换后结果等于 "HELLO"

```java
// 方法体开始
System.out.println("=== thenApply ===");
CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> "hello")
    .thenApply(String::toUpperCase);
String result = future.join();
System.out.println("原始: hello -> 转换后: " + result);
assertEquals("HELLO", result);
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### thenCombine

- 描述: 合并两个异步任务的结果（含 thenCombine / thenCombineAsync / thenCombineAsync(Executor)）
- 断言: 合并后的结果等于 "Hello World"

```java
// 方法体开始
System.out.println("=== thenCombine ===");
CompletableFuture<String> future1 = CompletableFuture.supplyAsync(() -> {
    System.out.println("任务1: 生成 Hello");
    return "Hello";
});
CompletableFuture<String> future2 = CompletableFuture.supplyAsync(() -> {
    System.out.println("任务2: 生成 World");
    return "World";
});
CompletableFuture<String> combined = future1.thenCombine(future2, (a, b) -> a + " " + b);
System.out.println("合并结果: " + combined.join());
assertEquals("Hello World", combined.join());
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### thenCompose

- 描述: 扁平化组合两个异步任务（含 thenCompose / thenComposeAsync / thenComposeAsync(Executor)）
- 断言: thenCompose 后结果等于 "HELLO WORLD"

```java
// 方法体开始
System.out.println("=== thenCompose ===");
CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> "hello")
    .thenCompose(s -> CompletableFuture.supplyAsync(() -> s.toUpperCase() + " WORLD"));
String result = future.join();
System.out.println("组合结果: " + result);
assertEquals("HELLO WORLD", result);
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### completedFuture

- 描述: 创建已完成的 Future
- 断言: completedFuture 立即返回给定的值

```java
// 方法体开始
System.out.println("=== completedFuture ===");
CompletableFuture<String> future = CompletableFuture.completedFuture("done");
String result = future.join();
System.out.println("已完成 Future 结果: " + result);
assertEquals("done", result);
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### complete

- 描述: 手动完成 Future
- 断言: complete() 方法设置结果值

```java
// 方法体开始
System.out.println("=== complete ===");
CompletableFuture<String> future = new CompletableFuture<>();
boolean completed = future.complete("manual");
System.out.println("手动完成结果: " + completed);
assertTrue(completed);
assertEquals("manual", future.join());
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### exceptionally

- 描述: 异常恢复
- 断言: 发生异常时通过 exceptionally 恢复，结果等于 "recovered"

```java
// 方法体开始
System.out.println("=== exceptionally ===");
CompletableFuture<String> future = CompletableFuture.<String>supplyAsync(() -> {
    System.out.println("任务即将抛出异常");
    throw new RuntimeException("出错了");
}).exceptionally(ex -> {
    System.out.println("捕获异常: " + ex.getMessage());
    return "recovered";
});
String result = future.join();
System.out.println("恢复结果: " + result);
assertEquals("recovered", result);
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### handle

- 描述: 无论成功/失败都处理（含 handle / handleAsync / handleAsync(Executor)）
- 断言: handle 能同时处理正常结果和异常

```java
// 方法体开始
System.out.println("=== handle ===");
CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> "ok")
    .handle((result, ex) -> {
        System.out.println("处理结果: " + result + ", 异常: " + ex);
        return result + " handled";
    });
System.out.println("处理后的结果: " + future.join());
assertEquals("ok handled", future.join());
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### runAsync

- 描述: 异步无返回值的任务（含 runAsync / runAsync(Executor)）
- 断言: runAsync 执行后结果返回 null

```java
// 方法体开始
System.out.println("=== runAsync ===");
CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
    System.out.println("running in async");
});
future.join();
System.out.println("异步任务已完成");
assertNull(future.join());
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### allOf

- 描述: 等待多个 CompletableFuture 全部完成
- 断言: 三个任务全部完成后，每个结果均符合预期

```java
// 方法体开始
System.out.println("=== allOf ===");
CompletableFuture<String> f1 = CompletableFuture.supplyAsync(() -> {
    System.out.println("任务A: 开始");
    return "A";
});
CompletableFuture<String> f2 = CompletableFuture.supplyAsync(() -> {
    System.out.println("任务B: 开始");
    return "B";
});
CompletableFuture<String> f3 = CompletableFuture.supplyAsync(() -> {
    System.out.println("任务C: 开始");
    return "C";
});
System.out.println("等待所有任务完成...");
CompletableFuture.allOf(f1, f2, f3).join();
System.out.println("所有任务已完成");
assertEquals("A", f1.join());
assertEquals("B", f2.join());
assertEquals("C", f3.join());
System.out.println("结果: " + f1.join() + ", " + f2.join() + ", " + f3.join());
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### anyOf

- 描述: 等待任意一个完成
- 断言: anyOf 返回最先完成的任务结果

```java
// 方法体开始
System.out.println("=== anyOf ===");
CompletableFuture<String> slow = CompletableFuture.supplyAsync(() -> {
    try { Thread.sleep(100); } catch (Exception e) {}
    return "slow";
});
CompletableFuture<String> fast = CompletableFuture.supplyAsync(() -> "fast");
Object result = CompletableFuture.anyOf(slow, fast).join();
System.out.println("最快的任务结果: " + result);
assertEquals("fast", result);
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### whenComplete

- 描述: 完成时回调（含 whenComplete / whenCompleteAsync / whenCompleteAsync(Executor)）
- 断言: whenComplete 能在完成后执行回调

```java
// 方法体开始
System.out.println("=== whenComplete ===");
StringBuilder sb = new StringBuilder();
CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> "done")
    .whenComplete((result, ex) -> {
        sb.append("callback executed");
        System.out.println("回调: result=" + result + ", ex=" + ex);
    });
assertEquals("done", future.join());
assertEquals("callback executed", sb.toString());
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### thenAccept

- 描述: 消费上一个阶段的结果（含 thenAccept / thenAcceptAsync / thenAcceptAsync(Executor)）
- 断言: thenAccept 能正确消费结果值

```java
// 方法体开始
System.out.println("=== thenAccept ===");
StringBuilder sb = new StringBuilder();
CompletableFuture.supplyAsync(() -> "Hello")
    .thenAccept(s -> sb.append(s));
// 等待异步完成
Thread.sleep(100);
System.out.println("消费结果: " + sb.toString());
assertEquals("Hello", sb.toString());
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### thenRun

- 描述: 在上一个阶段完成后执行 Runnable（含 thenRun / thenRunAsync / thenRunAsync(Executor)）
- 断言: thenRun 能正确执行 Runnable

```java
// 方法体开始
System.out.println("=== thenRun ===");
StringBuilder sb = new StringBuilder();
CompletableFuture.supplyAsync(() -> "data")
    .thenRun(() -> sb.append("run after"));
Thread.sleep(100);
System.out.println("thenRun 结果: " + sb.toString());
assertEquals("run after", sb.toString());
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### thenAcceptBoth

- 描述: 消费两个 CompletionStage 的结果（含 thenAcceptBoth / thenAcceptBothAsync / thenAcceptBothAsync(Executor)）
- 断言: thenAcceptBoth 能正确消费两个结果

```java
// 方法体开始
System.out.println("=== thenAcceptBoth ===");
StringBuilder sb = new StringBuilder();
CompletableFuture<String> f1 = CompletableFuture.supplyAsync(() -> "A");
CompletableFuture<String> f2 = CompletableFuture.supplyAsync(() -> "B");
f1.thenAcceptBoth(f2, (a, b) -> sb.append(a).append(b));
Thread.sleep(100);
System.out.println("合并消费结果: " + sb.toString());
assertEquals("AB", sb.toString());
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### runAfterBoth

- 描述: 在两个阶段都完成后执行操作（含 runAfterBoth / runAfterBothAsync / runAfterBothAsync(Executor)）
- 断言: runAfterBoth 在两个任务完成后才执行

```java
// 方法体开始
System.out.println("=== runAfterBoth ===");
StringBuilder sb = new StringBuilder();
CompletableFuture<String> f1 = CompletableFuture.supplyAsync(() -> {
    try { Thread.sleep(50); } catch (Exception e) {}
    return "A";
});
CompletableFuture<String> f2 = CompletableFuture.supplyAsync(() -> {
    try { Thread.sleep(50); } catch (Exception e) {}
    return "B";
});
f1.runAfterBoth(f2, () -> sb.append("both done"));
f1.join();
f2.join();
Thread.sleep(100);
System.out.println("runAfterBoth 结果: " + sb.toString());
assertEquals("both done", sb.toString());
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### runAfterEither

- 描述: 在任意一个阶段完成后执行操作（含 runAfterEither / runAfterEitherAsync / runAfterEitherAsync(Executor)）
- 断言: runAfterEither 在最快任务完成后立即执行

```java
// 方法体开始
System.out.println("=== runAfterEither ===");
StringBuilder sb = new StringBuilder();
CompletableFuture<String> slow = CompletableFuture.supplyAsync(() -> {
    try { Thread.sleep(100); } catch (Exception e) {}
    return "slow";
});
CompletableFuture<String> fast = CompletableFuture.supplyAsync(() -> {
    return "fast";
});
slow.runAfterEither(fast, () -> sb.append("either done"));
Thread.sleep(200);
System.out.println("runAfterEither 结果: " + sb.toString());
assertEquals("either done", sb.toString());
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### applyToEither

- 描述: 对最先完成的结果应用转换函数（含 applyToEither / applyToEitherAsync / applyToEitherAsync(Executor)）
- 断言: applyToEither 对最快的任务结果进行转换

```java
// 方法体开始
System.out.println("=== applyToEither ===");
CompletableFuture<String> slow = CompletableFuture.supplyAsync(() -> {
    try { Thread.sleep(100); } catch (Exception e) {}
    return "slow";
});
CompletableFuture<String> fast = CompletableFuture.supplyAsync(() -> "fast");
String result = slow.applyToEither(fast, s -> s.toUpperCase()).join();
System.out.println("applyToEither 结果: " + result);
assertEquals("FAST", result);
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### acceptEither

- 描述: 消费最先完成的结果（含 acceptEither / acceptEitherAsync / acceptEitherAsync(Executor)）
- 断言: acceptEither 消费最快任务的结果

```java
// 方法体开始
System.out.println("=== acceptEither ===");
StringBuilder sb = new StringBuilder();
CompletableFuture<String> slow = CompletableFuture.supplyAsync(() -> {
    try { Thread.sleep(100); } catch (Exception e) {}
    return "slow";
});
CompletableFuture<String> fast = CompletableFuture.supplyAsync(() -> "fast");
slow.acceptEither(fast, s -> sb.append(s));
Thread.sleep(200);
System.out.println("acceptEither 消费结果: " + sb.toString());
assertEquals("fast", sb.toString());
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### completeExceptionally

- 描述: 手动异常完成 Future
- 断言: completeExceptionally 完成后抛出 CompletionException

```java
// 方法体开始
System.out.println("=== completeExceptionally ===");
CompletableFuture<String> future = new CompletableFuture<>();
future.completeExceptionally(new RuntimeException("boom"));
try {
    future.join();
    fail("应抛出异常");
} catch (CompletionException e) {
    System.out.println("捕获异常: " + e.getCause().getMessage());
    assertEquals("boom", e.getCause().getMessage());
}
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### isDone

- 描述: 检查 Future 是否已完成（含 isDone / isCancelled / isCompletedExceptionally）
- 断言: 正常完成后 isDone=true, isCancelled=false, isCompletedExceptionally=false

```java
// 方法体开始
System.out.println("=== isDone ===");
CompletableFuture<String> future = CompletableFuture.completedFuture("done");
System.out.println("isDone: " + future.isDone());
System.out.println("isCancelled: " + future.isCancelled());
System.out.println("isCompletedExceptionally: " + future.isCompletedExceptionally());
assertTrue(future.isDone());
assertFalse(future.isCancelled());
assertFalse(future.isCompletedExceptionally());
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### isCancelled

- 描述: 检查 Future 是否被取消
- 断言: 通过 cancel 取消后 isCancelled 返回 true

```java
// 方法体开始
System.out.println("=== isCancelled ===");
CompletableFuture<String> future = new CompletableFuture<>();
assertFalse(future.isCancelled());
future.cancel(true);
assertTrue(future.isCancelled());
System.out.println("取消后 isCancelled: " + future.isCancelled());
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### isCompletedExceptionally

- 描述: 检查 Future 是否异常完成
- 断言: 异常完成后 isCompletedExceptionally 返回 true

```java
// 方法体开始
System.out.println("=== isCompletedExceptionally ===");
CompletableFuture<String> future = new CompletableFuture<>();
assertFalse(future.isCompletedExceptionally());
future.completeExceptionally(new RuntimeException("fail"));
assertTrue(future.isCompletedExceptionally());
System.out.println("异常完成后 isCompletedExceptionally: " + future.isCompletedExceptionally());
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### getNow

- 描述: 获取结果或返回默认值
- 断言: 已完成时返回结果，未完成时返回默认值

```java
// 方法体开始
System.out.println("=== getNow ===");
CompletableFuture<String> completed = CompletableFuture.completedFuture("value");
String result1 = completed.getNow("default");
System.out.println("已完成 Future 的 getNow: " + result1);
assertEquals("value", result1);
CompletableFuture<String> incomplete = new CompletableFuture<>();
String result2 = incomplete.getNow("default");
System.out.println("未完成 Future 的 getNow: " + result2);
assertEquals("default", result2);
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### get(timeout)

- 描述: 带超时的阻塞获取结果
- 断言: 正常完成时返回结果，超时时抛出 TimeoutException

```java
// 方法体开始
System.out.println("=== get(timeout) ===");
CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> {
    try { Thread.sleep(50); } catch (Exception e) {}
    return "ok";
});
String result = future.get(1, TimeUnit.SECONDS);
System.out.println("获取结果: " + result);
assertEquals("ok", result);
CompletableFuture<String> slow = CompletableFuture.supplyAsync(() -> {
    try { Thread.sleep(5000); } catch (Exception e) {}
    return "never";
});
try {
    slow.get(100, TimeUnit.MILLISECONDS);
    fail("应抛出 TimeoutException");
} catch (TimeoutException e) {
    System.out.println("超时: " + e.getMessage());
}
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### get

- 描述: 阻塞获取结果（无超时版本）
- 断言: supplyAsync + get() 结果等于 "hello"

```java
// 方法体开始
System.out.println("=== get ===");
CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> "hello");
try {
    String result = future.get();
    System.out.println("get 结果: " + result);
    assertEquals("hello", result);
} catch (Exception e) {
    fail("不应抛出异常: " + e.getMessage());
}
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### join

- 描述: 等待并获取结果（与 get 的区别是不抛出受检异常）
- 断言: join 返回结果，取消时抛出 CancellationException

```java
// 方法体开始
System.out.println("=== join ===");
CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> "hello");
String result = future.join();
System.out.println("join 结果: " + result);
assertEquals("hello", result);
CompletableFuture<String> cancelled = new CompletableFuture<>();
cancelled.cancel(true);
try {
    cancelled.join();
    fail("应抛出 CancellationException");
} catch (CancellationException e) {
    System.out.println("已取消任务抛出 CancellationException");
}
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### obtrudeValue

- 描述: 强制设置结果值（含 obtrudeValue / obtrudeException）
- 断言: obtrudeValue 能覆盖已设置的结果

```java
// 方法体开始
System.out.println("=== obtrudeValue ===");
CompletableFuture<String> future = new CompletableFuture<>();
future.complete("first");
System.out.println("初始结果: " + future.join());
future.obtrudeValue("second");
System.out.println("obtrudeValue 后: " + future.join());
assertEquals("second", future.join());
CompletableFuture<String> obtrudeEx = CompletableFuture.completedFuture("ok");
obtrudeEx.obtrudeException(new RuntimeException("forced"));
try {
    obtrudeEx.join();
    fail("应抛出异常");
} catch (CompletionException e) {
    System.out.println("obtrudeException 后抛出: " + e.getCause().getMessage());
    assertEquals("forced", e.getCause().getMessage());
}
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### obtrudeException

- 描述: 强制设置异常结果
- 断言: obtrudeException 能覆盖已设置的结果并抛出异常

```java
// 方法体开始
System.out.println("=== obtrudeException ===");
CompletableFuture<String> future = new CompletableFuture<>();
future.complete("first");
System.out.println("初始结果: " + future.join());
future.obtrudeException(new RuntimeException("forced"));
try {
    future.join();
    fail("应抛出 CompletionException");
} catch (CompletionException e) {
    System.out.println("obtrudeException 后抛出: " + e.getCause().getMessage());
    assertEquals("forced", e.getCause().getMessage());
}
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### toString

- 描述: Future 的字符串表示
- 断言: toString 包含完成状态信息

```java
// 方法体开始
System.out.println("=== toString ===");
CompletableFuture<String> future = new CompletableFuture<>();
String before = future.toString();
System.out.println("完成前 toString: " + before);
assertTrue(before.contains("Not completed") || before.contains(" incomplete"));
future.complete("done");
String after = future.toString();
System.out.println("完成后 toString: " + after);
assertTrue(after.contains("Completed") || after.contains("complete") || after.contains("done"));
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### cancel

- 描述: 取消任务
- 断言: cancel 成功后 isCancelled 返回 true

```java
// 方法体开始
System.out.println("=== cancel ===");
CompletableFuture<String> future = new CompletableFuture<>();
boolean cancelled = future.cancel(true);
System.out.println("取消结果: " + cancelled);
assertTrue(cancelled);
assertTrue(future.isCancelled());
assertTrue(future.isDone());
assertTrue(future.isCompletedExceptionally());
System.out.println("=== 测试通过 ===");
// 方法体结束
```
