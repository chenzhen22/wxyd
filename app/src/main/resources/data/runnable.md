---
name: Runnable
package: java.lang
order: 321
---

## 介绍

`java.lang.Runnable` 是**可执行任务接口**，Java 8 中它是 `@FunctionalInterface`，可以用 Lambda 实现。

## 方法

### run

```java
public void run()
```

## 测试

- 描述: 使用 Lambda 创建 Runnable
- 断言: 任务执行成功

```java
// 方法体开始
System.out.println("=== Runnable ===");
List<String> log = new ArrayList<>();
Runnable task = () -> log.add("run");
task.run();
assertEquals(1, log.size());
assertEquals("run", log.get(0));
new Thread(() -> log.add("thread")).start();
Thread.sleep(100);
assertEquals(2, log.size());
System.out.println("Runnable 测试通过");
System.out.println("=== 测试通过 ===");
// 方法体结束
```
