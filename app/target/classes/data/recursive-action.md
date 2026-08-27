---
name: RecursiveAction
package: java.util.concurrent
order: 107
---

## 介绍

`RecursiveAction` 是 Java 7 引入的**分治并行动作**基类，与 `RecursiveTask` 类似，但**无返回值**。它用于对大数组或集合进行并行变换、初始化等操作。

RecursiveAction 的核心特点：
- **无返回值**：`compute()` 返回 `void`
- **分治模式**：拆分为子任务，fork/join 等待
- **副作用操作**：适合修改数组元素等操作

## 方法

### compute

```java
protected abstract void compute()
```

实现拆分和执行逻辑。

## 测试

### 数组翻倍

- 描述: 使用 RecursiveAction 并行将数组每个元素翻倍（阈值 = 1000）
- 断言: 所有元素翻倍

```java
// 方法体开始
System.out.println("=== 数组翻倍 ===");
int[] array = new int[100];
for (int i = 0; i < array.length; i++) array[i] = i + 1;
class DoubleAction extends RecursiveAction {
    final int lo, hi;
    DoubleAction(int lo, int hi) { this.lo = lo; this.hi = hi; }
    protected void compute() {
        if (hi - lo < 20) {
            for (int i = lo; i < hi; i++) array[i] *= 2;
        } else {
            int mid = (lo + hi) / 2;
            invokeAll(new DoubleAction(lo, mid), new DoubleAction(mid, hi));
        }
    }
}
ForkJoinPool pool = new ForkJoinPool();
pool.invoke(new DoubleAction(0, array.length));
pool.shutdown();
assertEquals(2, array[0]);   // 原来 1
assertEquals(200, array[99]); // 原来 100
System.out.println("array[0]=" + array[0] + ", array[99]=" + array[99]);
System.out.println("=== 测试通过 ===");
// 方法体结束
```
