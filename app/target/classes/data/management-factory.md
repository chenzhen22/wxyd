---
name: ManagementFactory
package: java.lang.management
order: 342
---

## 介绍

`java.lang.management.ManagementFactory` 是**管理工厂类**，用于获取 JVM 的管理 Bean（MXBean）。

## 方法

### getRuntimeMXBean

```java
public static RuntimeMXBean getRuntimeMXBean()
```

### getMemoryMXBean / getMemoryPoolMXBeans / getThreadMXBean

### getOperatingSystemMXBean / getClassLoadingMXBean

### getPlatformMBeanServer

## 测试

- 描述: 获取 JVM 运行时信息
- 断言: 信息获取成功

```java
// 方法体开始
System.out.println("=== ManagementFactory ===");
RuntimeMXBean runtime = ManagementFactory.getRuntimeMXBean();
assertNotNull(runtime);
assertNotNull(runtime.getVmName());
assertNotNull(runtime.getVmVersion());
System.out.println("JVM: " + runtime.getVmName() + " " + runtime.getVmVersion());
System.out.println("启动时间: " + runtime.getStartTime());
System.out.println("=== 测试通过 ===");
// 方法体结束
```
