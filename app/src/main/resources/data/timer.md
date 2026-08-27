---
name: Timer
package: java.util
order: 287
---

## 介绍

`java.util.Timer` 是**定时任务调度器**，安排在后台线程中执行任务。可以与 `TimerTask` 配合实现一次性或周期性任务。

## 方法

### schedule / scheduleAtFixedRate

安排任务执行。

### cancel

终止定时器。

## 测试

- 描述: 使用新 API 风格
- 断言: 任务执行正确

```java
// 方法体开始
System.out.println("=== Timer 测试 ===");
// 此处验证 Timer 基本功能
Timer timer = new Timer("test-timer", true);
CountDownLatch latch = new CountDownLatch(1);
timer.schedule(new TimerTask() {
    public void run() {
        latch.countDown();
    }
}, 100);
assertTrue(latch.await(500, TimeUnit.MILLISECONDS));
timer.cancel();
System.out.println("Timer 测试通过");
System.out.println("=== 测试通过 ===");
// 方法体结束
```
