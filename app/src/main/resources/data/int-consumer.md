---
name: IntConsumer
package: java.util.function
order: 64
---

## 介绍

`IntConsumer` 是 Java 8 引入的一个**函数式接口**，代表一个接受单个 `int` 参数但不返回结果的操作。它是 `Consumer` 的 `int` 原始类型特化版本，专为处理原始 int 值设计。

IntConsumer 的核心特点：
- **避免装箱**：直接操作原始 int 值，无需 Integer 装箱拆箱
- **与 Stream 集成**：`IntStream.forEach()` 的回调接口
- **链式组合**：通过 `andThen` 组合多个 IntConsumer

IntConsumer 的两个方法：
- `accept(int value)` — 核心方法，对给定的 int 值执行操作
- `andThen(IntConsumer)` — 先执行当前操作，再执行另一个操作

对应的原始类型特化 Consumer 还有：
- `LongConsumer` — 接受 long 参数
- `DoubleConsumer` — 接受 double 参数

## 方法

### accept

```java
void accept(int value)
```

对给定 int 参数执行操作。

- **参数**: `value` — 输入的 int 值
- **返回**: 无

### andThen

```java
default IntConsumer andThen(IntConsumer after)
```

返回一个组合 IntConsumer，先执行当前操作，再执行 `after` 操作。

- **参数**: `after` — 后执行的操作
- **返回**: `IntConsumer` — 组合后的 IntConsumer

## 测试

### accept 收集数据

- 描述: 使用 `accept` 方法将 int 值收集到列表中
- 断言: accept 执行后列表包含正确的值

```java
// 方法体开始
System.out.println("=== accept 收集 ===");
List<Integer> list = new ArrayList<>();
IntConsumer collector = v -> list.add(v);
collector.accept(1);
collector.accept(2);
collector.accept(3);
assertEquals(3, list.size());
assertEquals(Integer.valueOf(1), list.get(0));
assertEquals(Integer.valueOf(3), list.get(2));
System.out.println("list: " + list);
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### andThen

- 描述: 使用 `andThen` 组合两个 IntConsumer 依次执行
- 断言: 先收集到列表，再累加到总和

```java
// 方法体开始
System.out.println("=== andThen ===");
List<Integer> list = new ArrayList<>();
int[] sum = {0};
IntConsumer collector = v -> list.add(v);
IntConsumer adder = v -> sum[0] += v;
IntConsumer combined = collector.andThen(adder);
combined.accept(5);
combined.accept(10);
assertEquals(2, list.size());
assertEquals(15, sum[0]);
System.out.println("list: " + list + ", sum: " + sum[0]);
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### IntStream.forEach

- 描述: 使用 IntConsumer 配合 `IntStream.forEach` 遍历 int 流
- 断言: 遍历所有元素

```java
// 方法体开始
System.out.println("=== IntStream.forEach ===");
List<Integer> result = new ArrayList<>();
IntStream.of(1, 2, 3, 4, 5)
        .forEach(v -> result.add(v));
assertEquals(5, result.size());
assertEquals(Arrays.asList(1, 2, 3, 4, 5), result);
System.out.println("result: " + result);
System.out.println("=== 测试通过 ===");
// 方法体结束
```
