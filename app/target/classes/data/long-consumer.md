---
name: LongConsumer
package: java.util.function
order: 76
---

## 介绍

`LongConsumer` 是 Java 8 引入的一个**函数式接口**，代表一个接受单个 `long` 参数但不返回结果的操作。它是 `Consumer<Long>` 的原始 long 特化版本，专为处理原始 long 值设计。

LongConsumer 的核心特点：
- **避免装箱**：直接操作原始 long 值，无需 Long 装箱拆箱
- **与 LongStream 集成**：`LongStream.forEach()` 的回调接口
- **链式组合**：通过 `andThen` 组合多个 LongConsumer

LongConsumer 的两个方法：
- `accept(long value)` — 核心方法，对给定的 long 值执行操作
- `andThen(LongConsumer)` — 先执行当前操作，再执行另一个操作

## 方法

### accept

```java
void accept(long value)
```

对给定 long 参数执行操作。

- **参数**: `value` — 输入的 long 值
- **返回**: 无

### andThen

```java
default LongConsumer andThen(LongConsumer after)
```

返回一个组合 LongConsumer，先执行当前操作，再执行 `after` 操作。

- **参数**: `after` — 后执行的操作
- **返回**: `LongConsumer` — 组合后的 LongConsumer

## 测试

### accept 收集数据

- 描述: 使用 `accept` 方法将 long 值收集到列表中
- 断言: accept 执行后列表包含正确的值

```java
// 方法体开始
System.out.println("=== accept 收集 ===");
List<Long> list = new ArrayList<>();
LongConsumer collector = v -> list.add(v);
collector.accept(100L);
collector.accept(200L);
collector.accept(300L);
assertEquals(3, list.size());
assertEquals(Long.valueOf(100L), list.get(0));
assertEquals(Long.valueOf(300L), list.get(2));
System.out.println("list: " + list);
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### LongStream.forEach

- 描述: 使用 LongConsumer 配合 `LongStream.forEach` 遍历 long 流
- 断言: 遍历所有元素并累加

```java
// 方法体开始
System.out.println("=== LongStream.forEach ===");
long[] sum = {0};
LongStream.of(10L, 20L, 30L)
        .forEach(v -> sum[0] += v);
assertEquals(60L, sum[0]);
System.out.println("总和: " + sum[0]);
System.out.println("=== 测试通过 ===");
// 方法体结束
```
