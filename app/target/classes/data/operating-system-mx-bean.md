---
name: OperatingSystemMXBean
package: java.lang.management
order: 345
---

## 介绍

`java.lang.management.OperatingSystemMXBean` 是**操作系统管理 Bean**接口，用于获取 OS 和 JVM 的系统信息。

## 方法

### getName / getArch / getVersion

### getAvailableProcessors / getSystemLoadAverage

## 测试

- 描述: 获取操作系统信息
- 断言: 信息获取成功

```java
// 方法体开始
System.out.println("=== OperatingSystemMXBean ===");
OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
assertNotNull(osBean.getName());
assertNotNull(osBean.getArch());
assertTrue(osBean.getAvailableProcessors() > 0);
System.out.println("OS: " + osBean.getName() + " " + osBean.getVersion());
System.out.println("架构: " + osBean.getArch());
System.out.println("CPU: " + osBean.getAvailableProcessors() + " 核");
System.out.println("=== 测试通过 ===");
// 方法体结束
```
