---
name: CompilationMXBean
package: java.lang.management
order: 346
---

## 介绍

`java.lang.management.CompilationMXBean` 是**编译管理 Bean**接口，用于监控 JIT 编译信息。

## 方法

### getName / isCompilationTimeMonitoringSupported / getTotalCompilationTime

## 测试

- 描述: 获取 JIT 编译信息
- 断言: 信息获取成功

```java
// 方法体开始
System.out.println("=== CompilationMXBean ===");
CompilationMXBean compBean = ManagementFactory.getCompilationMXBean();
assertNotNull(compBean.getName());
System.out.println("JIT 编译器: " + compBean.getName());
if (compBean.isCompilationTimeMonitoringSupported()) {
    System.out.println("总编译时间: " + compBean.getTotalCompilationTime() + " ms");
}
System.out.println("=== 测试通过 ===");
// 方法体结束
```
