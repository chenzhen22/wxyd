---
name: Callable
package: java.util.concurrent
order: 257
---

## 介绍

`java.util.concurrent.Callable<V>` 是**有返回值的任务接口**，与 `Runnable` 相对。在 Java 8 中是 `@FunctionalInterface`，可以用 Lambda 表达式创建。

## 方法

### call

```java
public V call() throws Exception
```

计算并返回结果。

## 测试

- 描述: 使用 Lambda 创建 Callable
- 断言: 获取返回结果

```java
// 方法体开始
System.out.println("=== Callable ===");
ExecutorService executor = Executors.newSingleThreadExecutor();
Callable<String> task = () -> {
    Thread.sleep(100);
    return "Hello from Callable";
};
Future<String> future = executor.submit(task);
assertEquals("Hello from Callable", future.get());
executor.shutdown();
System.out.println("Callable 执行结果: " + future.get());
System.out.println("=== 测试通过 ===");
// 方法体结束
```
