---
name: ToLongFunction
package: java.util.function
order: 87
---

## 介绍

`ToLongFunction<T>` 是 Java 8 引入的一个**函数式接口**，代表一个从对象中提取 `long` 值的函数。与 `Function<T, Long>` 不同，它直接产生原始 long 值。

ToLongFunction 的单个方法：
- `applyAsLong(T value)` — 核心方法

对应的特化接口：
- `ToIntFunction<T>` — 提取 int 值
- `ToDoubleFunction<T>` — 提取 double 值

## 方法

### applyAsLong

```java
long applyAsLong(T value)
```

从给定对象中提取 long 值。

## 测试

### 提取数值

- 描述: 解析字符串为 long
- 断言: "123456789" 解析正确

```java
// 方法体开始
System.out.println("=== applyAsLong ===");
ToLongFunction<String> parser = s -> Long.parseLong(s);
assertEquals(123456789L, parser.applyAsLong("123456789"));
assertEquals(0L, parser.applyAsLong("0"));
System.out.println("123456789 -> " + parser.applyAsLong("123456789"));
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### Stream.mapToLong

- 描述: 使用 `Stream.mapToLong` 映射
- 断言: 字符串长度总和

```java
// 方法体开始
System.out.println("=== Stream.mapToLong ===");
long total = Stream.of("Java", "Python", "Go")
        .mapToLong(s -> (long) s.length())
        .sum();
assertEquals(12L, total);
System.out.println("总长度: " + total);
System.out.println("=== 测试通过 ===");
// 方法体结束
```
