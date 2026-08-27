---
name: Logger
package: java.util.logging
order: 138
---

## 介绍

`java.util.logging.Logger` 是 JDK 自带的日志 API 中的核心类。Java 8 为其新增了 `getGlobal()` 便捷方法和 Lambda 支持的日志方法。

Java 8 新增的核心方法：
- `getGlobal()` — 获取全局日志记录器
- `log(Level, Supplier<String>)` — 延迟日志消息构造（Lambda 友好）
- `info(Supplier<String>)` / `warning(Supplier<String>)` / `fine(Supplier<String>)` 等 — 各等级的 Lambda 版本

## 方法

### getGlobal

```java
public static final Logger getGlobal()
```

返回全局日志记录器（Java 8 新增）。

### info / warning / fine

```java
public void info(Supplier<String> msgSupplier)
public void warning(Supplier<String> msgSupplier)
public void fine(Supplier<String> msgSupplier)
```

使用 Supplier 延迟构造日志消息（Java 8 新增）。

## 测试

### getGlobal

- 描述: 获取全局 Logger
- 断言: 不为 null

```java
// 方法体开始
System.out.println("=== getGlobal ===");
Logger logger = Logger.getGlobal();
assertNotNull(logger);
assertEquals("global", logger.getName());
System.out.println("全局 Logger: " + logger.getName());
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### Supplier 日志

- 描述: 使用 Lambda 记录日志
- 断言: 仅在需要时构造日志字符串

```java
// 方法体开始
System.out.println("=== Supplier 日志 ===");
Logger logger = Logger.getGlobal();
logger.setLevel(Level.FINE);
logger.info(() -> "延迟构造: " + System.currentTimeMillis());
System.out.println("Lambda 日志记录完成");
System.out.println("=== 测试通过 ===");
// 方法体结束
```
