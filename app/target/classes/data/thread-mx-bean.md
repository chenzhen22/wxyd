---
name: ThreadMXBean
package: java.lang.management
order: 344
---

## 介绍

`java.lang.management.ThreadMXBean` 是**线程管理 Bean**接口，用于监控 JVM 线程信息。

## 方法

### getThreadCount

```java
public int getThreadCount()
```

### getPeakThreadCount / getTotalStartedThreadCount

### findDeadlockedThreads / isThreadContentionMonitoringEnabled

## 测试

- 描述: 获取线程信息
- 断言: 信息获取成功

```java
// 方法体开始
System.out.println("=== ThreadMXBean ===");
ThreadMXBean threadBean = ManagementFactory.getThreadMXBean();
assertTrue(threadBean.getThreadCount() > 0);
assertTrue(threadBean.getTotalStartedThreadCount() > 0);
System.out.println("活跃线程: " + threadBean.getThreadCount());
System.out.println("峰值线程: " + threadBean.getPeakThreadCount());
System.out.println("启动总线程: " + threadBean.getTotalStartedThreadCount());
System.out.println("=== 测试通过 ===");
// 方法体结束
```
