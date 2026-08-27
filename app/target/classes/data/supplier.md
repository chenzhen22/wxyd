---
name: Supplier
package: java.util.function
order: 28
---

## 介绍

`Supplier<T>` 是 Java 8 引入的一个**函数式接口**，代表一个不接受参数但返回结果的供给者（即"提供"一个值）。它是延迟计算和懒加载的理想工具。

Supplier 的核心特点：
- **延迟计算**：直到调用 `get()` 方法时才计算结果
- **无参生成**：不接受参数，只返回结果
- **惰性求值**：常用于 `Optional.orElseGet`、Stream 的 `generate` 等场景

Supplier 只有一个方法：
- `get()` — 核心方法，返回一个结果

## 方法

### get

```java
T get()
```

获取结果。每次调用 `get()` 方法都会执行供给逻辑。

- **返回**: `T` — 供给的结果

## 测试

### get

- 描述: 使用 `get` 方法获取供给的值
- 断言: Supplier 返回 `"hello"`

```java
// 方法体开始
System.out.println("=== get ===");
Supplier<String> hello = () -> "hello";
assertEquals("hello", hello.get());
assertEquals("hello", hello.get());
System.out.println("第一次 get: " + hello.get());
System.out.println("第二次 get: " + hello.get());
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### get 数值计算

- 描述: 使用 Supplier 提供延迟计算的数值
- 断言: Supplier 返回正确的计算结果

```java
// 方法体开始
System.out.println("=== get 数值计算 ===");
Supplier<Integer> sum = () -> 3 + 4;
assertEquals(Integer.valueOf(7), sum.get());
assertEquals(Integer.valueOf(7), sum.get());
System.out.println("3+4 结果: " + sum.get());
System.out.println("=== 测试通过 ===");
// 方法体结束
```
