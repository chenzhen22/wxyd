---
name: CompletionStage
package: java.util.concurrent
order: 185
---

## 介绍

`java.util.concurrent.CompletionStage<T>` 是 Java 8 新增的**异步计算阶段接口**，是 `CompletableFuture` 实现的接口。它定义了异步计算中多个阶段的组合方式。

CompletionStage 的核心特点：
- **异步编排**：定义依赖阶段、并行阶段的组合
- **回调式**：完成后自动执行回调
- **异常处理**：链式传递异常
- **组合能力**：`thenCombine`、`thenCompose` 等

## 方法

### thenApply

```java
public <U> CompletionStage<U> thenApply(Function<? super T, ? extends U> fn)
```

当前阶段正常完成后，对其结果应用转换。

### thenAccept

```java
public CompletionStage<Void> thenAccept(Consumer<? super T> action)
```

当前阶段正常完成后，消费其结果。

### thenRun

```java
public CompletionStage<Void> thenRun(Runnable action)
```

当前阶段正常完成后，执行指定操作。

### exceptionally

```java
public CompletionStage<T> exceptionally(Function<Throwable, ? extends T> fn)
```

当前阶段异常时，应用转换恢复。

## 测试

### thenApply

- 描述: 使用 thenApply 链式转换
- 断言: 结果正确

```java
// 方法体开始
System.out.println("=== thenApply ===");
CompletableFuture<Integer> future = CompletableFuture.supplyAsync(() -> 42)
        .thenApply(n -> n * 2)
        .thenApply(n -> n + 1);
assertEquals(Integer.valueOf(85), future.get());
System.out.println("thenApply 结果: " + future.get());
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### exceptionally

- 描述: 使用 exceptionally 处理异常
- 断言: 异常时返回备用值

```java
// 方法体开始
System.out.println("=== exceptionally ===");
CompletableFuture<Integer> future = CompletableFuture.<Integer>completedFuture(42)
        .thenApply(n -> n / 0)  // 会抛出 ArithmeticException
        .exceptionally(e -> -1);
assertEquals(Integer.valueOf(-1), future.get());
System.out.println("异常恢复: " + future.get());
System.out.println("=== 测试通过 ===");
// 方法体结束
```
