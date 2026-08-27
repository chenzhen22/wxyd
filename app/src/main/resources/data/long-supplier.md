---
name: LongSupplier
package: java.util.function
order: 88
---

## 介绍

`LongSupplier` 是 Java 8 引入的一个**函数式接口**，代表一个不接受参数但返回 `long` 值的供应者。它是 `Supplier<Long>` 的原始 long 特化版本。

LongSupplier 的单个方法：
- `getAsLong()` — 核心方法

对应的特化 Supplier：
- `IntSupplier` — 返回 int
- `DoubleSupplier` — 返回 double
- `BooleanSupplier` — 返回 boolean

## 方法

### getAsLong

```java
long getAsLong()
```

供应一个 long 值。

## 测试

### getAsLong 常量

- 描述: 返回常量 long 值
- 断言: 始终返回 Long.MAX_VALUE

```java
// 方法体开始
System.out.println("=== 常量 ===");
LongSupplier maxVal = () -> Long.MAX_VALUE;
assertEquals(Long.MAX_VALUE, maxVal.getAsLong());
System.out.println("Long.MAX_VALUE: " + maxVal.getAsLong());
System.out.println("=== 测试通过 ===");
// 方法体结束
```
