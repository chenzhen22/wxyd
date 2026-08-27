---
name: Stream
package: java.util.stream
order: 9
---

## 介绍

`java.util.stream.Stream<T>` 是 Java 8 中最重要的新 API，支持函数式风格的数据操作。Stream 不是数据结构，而是对数据源的声明式流水线操作。

## 方法

### filter / map / flatMap

中间操作：过滤、映射、扁平映射。

### sorted / distinct / limit / skip

中间操作：排序、去重、截取、跳过。

### forEach / collect / reduce

终端操作：遍历、收集、归约。

### generate / iterate

```java
public static <T> Stream<T> generate(Supplier<T> s)
public static <T> Stream<T> iterate(T seed, UnaryOperator<T> f)
```

创建无限流。

### of / concat / empty

```java
public static <T> Stream<T> of(T... values)
public static <T> Stream<T> concat(Stream<? extends T> a, Stream<? extends T> b)
```

## 测试

### filter / map / collect

- 描述: 链式操作
- 断言: 结果正确

```java
// 方法体开始
System.out.println("=== filter/map/collect ===");
List<Integer> result = Stream.of(1, 2, 3, 4, 5, 6)
        .filter(n -> n % 2 == 0)
        .map(n -> n * n)
        .collect(Collectors.toList());
assertEquals("[4, 16, 36]", result.toString());
System.out.println("结果: " + result);
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### generate / limit

- 描述: 生成无限流取前 N 个
- 断言: 正确生成序列

```java
// 方法体开始
System.out.println("=== generate/limit ===");
List<Integer> nums = Stream.iterate(1, n -> n + 1)
        .limit(5)
        .collect(Collectors.toList());
assertEquals("[1, 2, 3, 4, 5]", nums.toString());
System.out.println("iterate: " + nums);
System.out.println("=== 测试通过 ===");
// 方法体结束
```
