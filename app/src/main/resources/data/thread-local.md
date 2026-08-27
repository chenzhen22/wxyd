---
name: ThreadLocal
package: java.lang
order: 117
---

## 介绍

`java.lang.ThreadLocal` 类在 Java 8 中新增了核心构造方法 **`withInitial(Supplier)`**，可以更简洁地创建 ThreadLocal 变量。

Java 8 新增的 ThreadLocal API：
- `withInitial(Supplier)` — 工厂方法，用 Supplier 创建 ThreadLocal

## 方法

### withInitial

```java
public static <S> ThreadLocal<S> withInitial(Supplier<? extends S> supplier)
```

创建一个 ThreadLocal，其初始值由 Supplier 提供。

- **参数**: `supplier` — 初始值供应者
- **返回**: `ThreadLocal<S>` — 新创建的 ThreadLocal

## 测试

### withInitial

- 描述: 使用 withInitial 创建 ThreadLocal
- 断言: 初始值正确

```java
// 方法体开始
System.out.println("=== withInitial ===");
ThreadLocal<Integer> tl = ThreadLocal.withInitial(() -> 42);
assertEquals(Integer.valueOf(42), tl.get());
tl.set(100);
assertEquals(Integer.valueOf(100), tl.get());
tl.remove();
assertEquals(Integer.valueOf(42), tl.get());  // 移除后回到初始值
System.out.println("初始值: " + ThreadLocal.withInitial(() -> 0).get());
System.out.println("=== 测试通过 ===");
// 方法体结束
```
