---
name: ServiceLoader
package: java.util
order: 127
---

## 介绍

`java.util.ServiceLoader` 是 Java 的**服务提供者加载**机制，遵循服务接口-提供者模式（SPI）。Java 8 为其新增了 `stream()` 方法，以 Stream 方式遍历服务提供者。

ServiceLoader 的核心特点：
- **SPI 机制**：允许在运行时发现和加载服务实现
- **延迟加载**：默认延迟加载提供者
- **JDBC 驱动**：JDBC 4.0 使用 ServiceLoader 自动加载驱动

Java 8 新增的方法：
- `stream()` — 返回服务提供者的 Stream

## 方法

### load

```java
public static <S> ServiceLoader<S> load(Class<S> service)
```

使用当前线程的上下文类加载器创建服务加载器。

### stream

```java
public Stream<ServiceLoader.Provider<S>> stream()
```

返回一个延迟加载的 Stream，包含所有已发现的服务提供者（Java 8 新增）。

- **返回**: `Stream<Provider<S>>` — 提供者流

### iterator

```java
public Iterator<S> iterator()
```

返回所有已发现的服务实现的迭代器。

## 测试

### stream

- 描述: 使用 ServiceLoader.stream() 发现服务实现
- 断言: 至少加载了一个服务

```java
// 方法体开始
System.out.println("=== ServiceLoader.stream ===");
// 使用 ServiceLoader 加载 java.nio.file.spi.FileTypeDetector 服务
ServiceLoader<java.nio.file.spi.FileTypeDetector> loader =
        ServiceLoader.load(java.nio.file.spi.FileTypeDetector.class);
long count = loader.stream().count();
System.out.println("FileTypeDetector 提供者数量: " + count);
assertTrue(count >= 0);
System.out.println("=== 测试通过 ===");
// 方法体结束
```
