---
name: ThreadGroup
package: java.lang
order: 250
---

## 介绍

`java.lang.ThreadGroup` 是**线程组**类，用于管理一组线程。

## 方法

构造方法：
```java
public ThreadGroup(String name)
public ThreadGroup(ThreadGroup parent, String name)
```

### activeCount / activeGroupCount

活跃线程数/线程组数。

### enumerate

枚举组内线程。

### interrupt

中断组内所有线程。

### setDaemon / setMaxPriority

设置守护状态/最大优先级。

## 测试

- 描述: 创建线程组并管理线程
- 断言: 线程组操作正常

```java
// 方法体开始
System.out.println("=== ThreadGroup ===");
ThreadGroup group = new ThreadGroup("test-group");
Thread t1 = new Thread(group, () -> {}, "thread-1");
Thread t2 = new Thread(group, () -> {}, "thread-2");
t1.start();
t2.start();
t1.join();
t2.join();
assertEquals(0, group.activeCount());
System.out.println("线程组: " + group.getName() + ", 活跃线程: " + group.activeCount());
System.out.println("=== 测试通过 ===");
// 方法体结束
```
