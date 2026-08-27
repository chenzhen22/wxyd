---
name: Thread
package: java.lang
order: 239
---

## 介绍

`java.lang.Thread` 是 Java 中的**执行线程**类。Java 8 使得创建线程更加简洁，可以使用 Lambda 表达式代替匿名内部类。

## 方法

构造方法：
```java
public Thread(Runnable target)
public Thread(Runnable target, String name)
```

### start / run

启动线程 / 线程执行体。

### sleep / join / yield

```java
public static void sleep(long millis) throws InterruptedException
public final void join() throws InterruptedException
```

线程控制方法。

### setName / getName / setPriority / getPriority

线程属性方法。

## 测试

- 描述: 使用 Lambda 创建线程
- 断言: 线程运行正确

```java
// 方法体开始
System.out.println("=== Thread ===");
List<String> result = new ArrayList<>();
Thread t = new Thread(() -> {
    result.add("running");
});
t.start();
t.join();
assertEquals(1, result.size());
assertEquals("running", result.get(0));
System.out.println("Lambda 线程执行成功");
System.out.println("=== 测试通过 ===");
// 方法体结束
```
