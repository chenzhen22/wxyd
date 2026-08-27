---
name: DoubleConsumer
package: java.util.function
order: 77
---

## 介绍

`DoubleConsumer` 是 Java 8 引入的一个**函数式接口**，代表一个接受单个 `double` 参数但不返回结果的操作。它是 `Consumer<Double>` 的原始 double 特化版本，专为处理原始 double 值设计。

DoubleConsumer 的核心特点：
- **避免装箱**：直接操作原始 double 值，无需 Double 装箱拆箱
- **与 DoubleStream 集成**：`DoubleStream.forEach()` 的回调接口
- **链式组合**：通过 `andThen` 组合多个 DoubleConsumer

DoubleConsumer 的两个方法：
- `accept(double value)` — 核心方法，对给定的 double 值执行操作
- `andThen(DoubleConsumer)` — 先执行当前操作，再执行另一个操作

## 方法

### accept

```java
void accept(double value)
```

对给定 double 参数执行操作。

- **参数**: `value` — 输入的 double 值
- **返回**: 无

### andThen

```java
default DoubleConsumer andThen(DoubleConsumer after)
```

返回一个组合 DoubleConsumer，先执行当前操作，再执行 `after` 操作。

- **参数**: `after` — 后执行的操作
- **返回**: `DoubleConsumer` — 组合后的 DoubleConsumer

## 测试

### accept 收集数据

- 描述: 使用 `accept` 方法将 double 值收集到列表中
- 断言: accept 执行后列表包含正确的值

```java
// 方法体开始
System.out.println("=== accept 收集 ===");
List<Double> list = new ArrayList<>();
DoubleConsumer collector = v -> list.add(v);
collector.accept(1.5);
collector.accept(2.5);
collector.accept(3.5);
assertEquals(3, list.size());
assertEquals(Double.valueOf(1.5), list.get(0));
assertEquals(Double.valueOf(3.5), list.get(2));
System.out.println("list: " + list);
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### DoubleStream.forEach

- 描述: 使用 DoubleConsumer 配合 `DoubleStream.forEach` 遍历 double 流
- 断言: 遍历所有元素并累加

```java
// 方法体开始
System.out.println("=== DoubleStream.forEach ===");
double[] sum = {0};
DoubleStream.of(1.5, 2.5, 3.0)
        .forEach(v -> sum[0] += v);
assertEquals(7.0, sum[0], 0.0001);
System.out.println("总和: " + sum[0]);
System.out.println("=== 测试通过 ===");
// 方法体结束
```
