---
name: ToDoubleFunction
package: java.util.function
order: 78
---

## 介绍

`ToDoubleFunction<T>` 是 Java 8 引入的一个**函数式接口**，代表一个提取 `double` 值的函数。它接受一个对象 `T` 作为输入，提取或转换出一个 `double` 值。

ToDoubleFunction 的核心特点：
- **对象→double**：将对象映射为原始 double 值
- **与 Stream 集成**：`Stream.mapToDouble()` 的常用参数
- **避免装箱**：直接产生原始 double 值

ToDoubleFunction 的单个方法：
- `applyAsDouble(T value)` — 核心方法，从对象中提取 double 值

对应特化接口：
- `ToIntFunction<T>` — 提取 int 值
- `ToLongFunction<T>` — 提取 long 值
- `DoubleFunction<R>` — 反向：double → 对象 R

## 方法

### applyAsDouble

```java
double applyAsDouble(T value)
```

从给定对象中提取 double 值。

- **参数**: `value` — 输入对象
- **返回**: `double` — 提取的 double 值

## 测试

### 提取价格

- 描述: 从 Map 中提取 double 值
- 断言: 苹果价格 5.5

```java
// 方法体开始
System.out.println("=== 提取价格 ===");
ToDoubleFunction<Map.Entry<String, Double>> getPrice = e -> e.getValue();
Map<String, Double> prices = new HashMap<>();
prices.put("apple", 5.5);
prices.put("banana", 3.0);
double applePrice = getPrice.applyAsDouble(prices.entrySet().iterator().next());
for (Map.Entry<String, Double> e : prices.entrySet()) {
    if (e.getKey().equals("apple")) {
        assertEquals(5.5, getPrice.applyAsDouble(e), 0.0001);
    }
}
System.out.println("苹果价格: " + prices.get("apple"));
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### Stream.mapToDouble

- 描述: 使用 `Stream.mapToDouble` 将字符串长度映射为 double
- 断言: 平均长度为 4.0

```java
// 方法体开始
System.out.println("=== Stream.mapToDouble ===");
double avg = Stream.of("Java", "Python", "Go")
        .mapToDouble(s -> (double) s.length())
        .average()
        .orElse(0.0);
System.out.println("平均长度: " + avg);
System.out.println("=== 测试通过 ===");
// 方法体结束
```
