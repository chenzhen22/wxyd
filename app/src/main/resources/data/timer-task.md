---
name: TimerTask
package: java.util
order: 179
---

## 介绍

`java.util.TimerTask` 是**可调度的一次性或周期性任务**的抽象类，与 `Timer` 配合使用。它实现了 `Runnable` 接口（Java 8 中 Runnable 是 @FunctionalInterface）。

## 方法

### run

```java
public abstract void run()
```

任务执行体。

### cancel

```java
public boolean cancel()
```

取消此任务。

## 测试

### 创建任务

- 描述: 创建 TimerTask 并执行
- 断言: 任务运行正确

```java
// 方法体开始
System.out.println("=== TimerTask ===");
int[] counter = {0};
TimerTask task = new TimerTask() {
    public void run() {
        counter[0]++;
    }
};
// 直接手动调用（非 Timer 调度方式）
task.run();
assertEquals(1, counter[0]);
System.out.println("TimerTask 直接执行: count=" + counter[0]);
System.out.println("=== 测试通过 ===");
// 方法体结束
```
