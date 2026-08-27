---
name: MemoryMXBean
package: java.lang.management
order: 343
---

## 介绍

`java.lang.management.MemoryMXBean` 是**内存管理 Bean**接口，用于监控 JVM 内存使用情况。

## 方法

### getHeapMemoryUsage / getNonHeapMemoryUsage

```java
public MemoryUsage getHeapMemoryUsage()
```

### gc / isVerbose / setVerbose

## 测试

- 描述: 获取堆内存信息
- 断言: 内存信息获取成功

```java
// 方法体开始
System.out.println("=== MemoryMXBean ===");
MemoryMXBean memBean = ManagementFactory.getMemoryMXBean();
MemoryUsage heap = memBean.getHeapMemoryUsage();
assertTrue(heap.getInit() > 0);
assertTrue(heap.getMax() > 0);
assertTrue(heap.getUsed() > 0);
System.out.println("堆内存: init=" + heap.getInit()/1024/1024 + "MB");
System.out.println("  已用: " + heap.getUsed()/1024/1024 + "MB");
System.out.println("  最大: " + heap.getMax()/1024/1024 + "MB");
System.out.println("=== 测试通过 ===");
// 方法体结束
```
