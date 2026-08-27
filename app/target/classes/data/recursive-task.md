---
name: RecursiveTask
package: java.util.concurrent
order: 106
---

## 介绍

`RecursiveTask<V>` 是 Java 7 引入的**分治并行任务**基类，与 `ForkJoinPool` 配合使用。它是 `ForkJoinTask` 的子类，用于有返回值的并行递归计算。

RecursiveTask 的核心特点：
- **分治模式**：在 `compute()` 中将大任务拆分为子任务
- **自动窃取**：工作窃取算法自动平衡负载
- **有返回值**：与 `RecursiveAction`（无返回值）相对
- **与 ForkJoinPool 配合**：通过 `ForkJoinPool.invoke(task)` 或 `fork()/join()` 执行

典型的分治模式：
1. 如果任务足够小，直接计算并返回结果
2. 否则拆分为 N 个子任务，`fork()` 每个子任务
3. `join()` 等待所有子任务并合并结果

## 方法

### compute

```java
protected abstract V compute()
```

核心方法，实现拆分和计算逻辑。

### fork

```java
public final ForkJoinTask<V> fork()
```

在当前池中异步执行子任务。

### join

```java
public final V join()
```

等待子任务完成并获取结果。

## 测试

### 斐波那契计算

- 描述: 使用 RecursiveTask 递归计算斐波那契数列
- 断言: fib(10) = 55

```java
// 方法体开始
System.out.println("=== 斐波那契 ===");
class Fibonacci extends RecursiveTask<Integer> {
    final int n;
    Fibonacci(int n) { this.n = n; }
    protected Integer compute() {
        if (n <= 1) return n;
        Fibonacci f1 = new Fibonacci(n - 1);
        f1.fork();
        Fibonacci f2 = new Fibonacci(n - 2);
        return f2.compute() + f1.join();
    }
}
ForkJoinPool pool = new ForkJoinPool();
int result = pool.invoke(new Fibonacci(10));
assertEquals(55, result);
pool.shutdown();
System.out.println("fib(10) = " + result);
System.out.println("=== 测试通过 ===");
// 方法体结束
```
