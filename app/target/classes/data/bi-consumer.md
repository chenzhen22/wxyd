---
name: BiConsumer
package: java.util.function
order: 57
---

## 介绍

`BiConsumer<T, U>` 是 Java 8 引入的一个**函数式接口**，代表一个接受两个参数但不返回结果的操作。它是 `Consumer` 的"双参数版本"，处理两个输入值而不产生返回值。

BiConsumer 的核心特点：
- **消费两个参数**：接受两个参数，执行副作用操作
- **链式执行**：通过 `andThen` 组合多个 BiConsumer
- **广泛使用**：`Map.forEach()`、`Iterable.forEach()` 的回调接口

BiConsumer 的两个方法：
- `accept(T t, U u)` — 核心方法，对给定两个参数执行操作
- `andThen(BiConsumer)` — 先执行当前操作，再执行另一个操作

在实际开发中，最常见的 BiConsumer 用法就是 `Map.forEach((key, value) -> ...)`。

## 方法

### accept

```java
void accept(T t, U u)
```

对给定两个参数执行操作。

- **参数**: `t` — 第一个输入参数；`u` — 第二个输入参数
- **返回**: 无

### andThen

```java
default BiConsumer<T, U> andThen(BiConsumer<? super T, ? super U> after)
```

返回一个组合 BiConsumer，先执行当前操作，再执行 `after` 操作。如果当前操作抛出异常，`after` 不会执行。

- **参数**: `after` — 后执行的操作
- **返回**: `BiConsumer<T, U>` — 组合后的 BiConsumer

## 测试

### accept 收集键值

- 描述: 使用 `accept` 方法将键值对收集到 Map 中
- 断言: accept 执行后 Map 包含正确的键值对

```java
// 方法体开始
System.out.println("=== accept 收集 ===");
Map<String, Integer> map = new HashMap<>();
BiConsumer<String, Integer> putter = (k, v) -> map.put(k, v);
putter.accept("one", 1);
putter.accept("two", 2);
assertEquals(Integer.valueOf(1), map.get("one"));
assertEquals(Integer.valueOf(2), map.get("two"));
assertEquals(2, map.size());
System.out.println("map: " + map);
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### andThen

- 描述: 使用 `andThen` 组合两个 BiConsumer 依次执行
- 断言: 先放入 Map，再追加到列表

```java
// 方法体开始
System.out.println("=== andThen ===");
Map<String, Integer> map = new HashMap<>();
List<String> log = new ArrayList<>();
BiConsumer<String, Integer> putter = (k, v) -> map.put(k, v);
BiConsumer<String, Integer> logger = (k, v) -> log.add(k + "=" + v);
BiConsumer<String, Integer> combined = putter.andThen(logger);
combined.accept("a", 1);
combined.accept("b", 2);
assertEquals(Integer.valueOf(1), map.get("a"));
assertEquals(2, log.size());
assertEquals("a=1", log.get(0));
System.out.println("map: " + map);
System.out.println("log: " + log);
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### Map.forEach

- 描述: 使用 BiConsumer 遍历 Map 的所有键值对
- 断言: 遍历累加所有值

```java
// 方法体开始
System.out.println("=== Map.forEach ===");
Map<String, Integer> scores = new HashMap<>();
scores.put("Alice", 90);
scores.put("Bob", 85);
scores.put("Charlie", 95);
int[] sum = {0};
scores.forEach((name, score) -> {
    sum[0] += score;
    System.out.println(name + ": " + score);
});
assertEquals(270, sum[0]);
System.out.println("总分: " + sum[0]);
System.out.println("=== 测试通过 ===");
// 方法体结束
```
