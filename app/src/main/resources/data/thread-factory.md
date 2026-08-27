---
name: ThreadFactory
package: java.util.concurrent
order: 186
---

## 介绍

`java.util.concurrent.ThreadFactory` 是 Java 5 引入的**线程工厂接口**。Java 8 使其成为 `@FunctionalInterface`，可以使用 Lambda 表达式创建。

## 方法

### newThread

```java
public Thread newThread(Runnable r)
```

创建新线程。

## 测试

### Lambda 实现

- 描述: 使用 Lambda 创建 ThreadFactory
- 断言: 线程名正确

```java
// 方法体开始
System.out.println("=== ThreadFactory ===");
ThreadFactory factory = r -> {
    Thread t = new Thread(r);
    t.setName("custom-pool-1");
    t.setDaemon(true);
    return t;
};
Thread t = factory.newThread(() -> {});
assertEquals("custom-pool-1", t.getName());
assertTrue(t.isDaemon());
System.out.println("线程名: " + t.getName());
System.out.println("=== 测试通过 ===");
// 方法体结束
```
