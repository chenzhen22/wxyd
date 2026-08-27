---
name: Runtime
package: java.lang
order: 237
---

## 介绍

`java.lang.Runtime` 是**运行时环境类**，每个 Java 应用都有一个 Runtime 实例，用于与 JVM 运行环境交互。

## 方法

### getRuntime

```java
public static Runtime getRuntime()
```

获取当前 Runtime 实例。

### availableProcessors

```java
public int availableProcessors()
```

返回可用处理器数量。

### totalMemory / freeMemory / maxMemory

```java
public long totalMemory()
public long freeMemory()
public long maxMemory()
```

获取 JVM 内存信息。

### gc

```java
public void gc()
```

提示 JVM 执行垃圾回收。

## 测试

- 描述: 获取 Runtime 环境信息
- 断言: 信息不为空

```java
// 方法体开始
System.out.println("=== Runtime ===");
Runtime rt = Runtime.getRuntime();
assertTrue(rt.availableProcessors() > 0);
assertTrue(rt.totalMemory() > 0);
assertTrue(rt.freeMemory() > 0);
assertTrue(rt.maxMemory() > 0);
System.out.println("CPU: " + rt.availableProcessors() + " 核");
System.out.println("总内存: " + rt.totalMemory() / (1024*1024) + " MB");
System.out.println("空闲内存: " + rt.freeMemory() / (1024*1024) + " MB");
System.out.println("=== 测试通过 ===");
// 方法体结束
```
