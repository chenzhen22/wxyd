---
name: Collection
package: java.util
order: 144
---

## 介绍

`java.util.Collection` 接口在 Java 8 中新增了多个强大的默认方法，主要包括**Stream 支持**、**Lambda 遍历和删除**。

Java 8 新增的方法：
- `stream()` — 返回顺序 Stream
- `parallelStream()` — 返回并行 Stream
- `removeIf(Predicate)` — 批量删除满足条件的元素
- `spliterator()` — 获取 Spliterator
- `forEach(Consumer)` — 遍历所有元素

## 方法

### stream

```java
default Stream<E> stream()
```

返回以此集合为数据源的顺序 Stream。

- **返回**: `Stream<E>` — 顺序流

### parallelStream

```java
default Stream<E> parallelStream()
```

返回可能并行的 Stream。

- **返回**: `Stream<E>` — 并行流

### removeIf

```java
default boolean removeIf(Predicate<? super E> filter)
```

删除所有满足给定条件的元素。

- **参数**: `filter` — 删除条件
- **返回**: `boolean` — 是否有元素被删除

### spliterator

```java
default Spliterator<E> spliterator()
```

获取集合的 Spliterator。

### forEach

```java
default void forEach(Consumer<? super T> action)
```

对每个元素执行操作。

## 测试

### stream

- 描述: 使用 Collection.stream()
- 断言: 流操作正常

```java
// 方法体开始
System.out.println("=== stream ===");
Collection<String> c = new ArrayList<>(Arrays.asList("a", "b", "c"));
List<String> upper = c.stream()
        .map(String::toUpperCase)
        .collect(Collectors.toList());
assertEquals("[A, B, C]", upper.toString());
System.out.println("stream: " + upper);
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### removeIf

- 描述: 使用 removeIf 批量删除
- 断言: 偶数被删除

```java
// 方法体开始
System.out.println("=== removeIf ===");
Collection<Integer> c = new ArrayList<>(Arrays.asList(1, 2, 3, 4, 5, 6));
boolean removed = c.removeIf(n -> n % 2 == 0);
assertTrue(removed);
assertEquals("[1, 3, 5]", c.toString());
System.out.println("删除偶后: " + c);
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### forEach

- 描述: 使用 Collection.forEach 遍历
- 断言: 累加所有元素

```java
// 方法体开始
System.out.println("=== forEach ===");
Collection<Integer> c = Arrays.asList(1, 2, 3, 4, 5);
int[] sum = {0};
c.forEach(n -> sum[0] += n);
assertEquals(15, sum[0]);
System.out.println("forEach 求和: " + sum[0]);
System.out.println("=== 测试通过 ===");
// 方法体结束
```
