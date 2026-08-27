---
name: StreamSupport
package: java.util.stream
order: 104
---

## 介绍

`java.util.stream.StreamSupport` 是 Java 8 引入的**底层 Stream 创建工具类**。用于从 Spliterator 或 Iterable 创建 Stream，是集合框架 Stream 方法背后的底层实现。

StreamSupport 的核心用途：
- 为没有 `stream()` 方法的数据源创建 Stream
- 通过 Spliterator 自定义遍历逻辑来创建 Stream
- 控制 Stream 的并行/串行行为

## 方法

### stream(Spliterator)

```java
public static <T> Stream<T> stream(Spliterator<T> spliterator, boolean parallel)
```

从 Spliterator 创建流。

- **参数**: `spliterator` — 可拆分迭代器；`parallel` — 是否并行
- **返回**: `Stream<T>` — 新创建的流

### intStream / longStream / doubleStream

```java
public static IntStream intStream(Spliterator.OfInt spliterator, boolean parallel)
public static LongStream longStream(Spliterator.OfLong spliterator, boolean parallel)
public static DoubleStream doubleStream(Spliterator.OfDouble spliterator, boolean parallel)
```

从原始类型 Spliterator 创建对应的原始类型流。

### stream(Iterable)

```java
public static <T> Stream<T> stream(Iterable<T> iterable, boolean parallel)
```

从 Iterable 创建流。

## 测试

### 从 Spliterator 创建 Stream

- 描述: 使用 StreamSupport 从 Spliterator 创建串行流
- 断言: 流操作正常

```java
// 方法体开始
System.out.println("=== 从 Spliterator 创建 ===");
List<Integer> list = Arrays.asList(1, 2, 3, 4, 5);
Stream<Integer> stream = StreamSupport.stream(list.spliterator(), false);
int sum = stream.mapToInt(Integer::intValue).sum();
assertEquals(15, sum);
System.out.println("sum: " + sum);
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### 从 Iterable 创建 Stream

- 描述: 从 Iterable 创建并行流
- 断言: 流操作正常

```java
// 方法体开始
System.out.println("=== 从 Iterable 创建 ===");
Iterable<String> iterable = Arrays.asList("a", "b", "c", "d", "e");
List<String> upper = StreamSupport.stream(iterable, true)
        .map(String::toUpperCase)
        .collect(Collectors.toList());
assertEquals(5, upper.size());
assertEquals("A", upper.get(0));
System.out.println("并行流结果: " + upper);
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### 创建 IntStream

- 描述: 从 OfInt Spliterator 创建 IntStream
- 断言: 流操作正常

```java
// 方法体开始
System.out.println("=== intStream ===");
int[] array = {2, 4, 6, 8, 10};
Spliterator.OfInt spl = java.util.Spliterators.spliterator(array, Spliterator.SIZED);
IntStream intStream = StreamSupport.intStream(spl, false);
int[] evens = intStream.map(x -> x / 2).toArray();
assertArrayEquals(new int[]{1, 2, 3, 4, 5}, evens);
System.out.println("结果: " + java.util.Arrays.toString(evens));
System.out.println("=== 测试通过 ===");
// 方法体结束
```
