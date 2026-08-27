---
name: Collectors
package: java.util.stream
order: 20
---

## 介绍

`java.util.stream.Collectors` 是 Java 8 引入的工具类，提供了将 `Stream` 的元素累积到各种容器中的**归约操作**。它和 `Stream.collect()` 方法配合使用，是 Stream API 最重要的搭档。

常见用途：
- **收集到集合**：`toList()`、`toSet()`、`toMap()`
- **字符串拼接**：`joining()`
- **分组/分区**：`groupingBy()`、`partitioningBy()`
- **聚合计算**：`summingInt()`、`averagingDouble()`、`summarizingInt()`

## 方法

### toList

```java
public static <T> Collector<T,?,List<T>> toList()
```

将 Stream 元素收集到 `List`。

- **返回**: `Collector`

### toSet

```java
public static <T> Collector<T,?,Set<T>> toSet()
```

将 Stream 元素收集到 `Set`。

- **返回**: `Collector`

### toMap

```java
public static <T,K,U> Collector<T,?,Map<K,U>> toMap(Function<? super T,? extends K> keyMapper, Function<? super T,? extends U> valueMapper)
```

将 Stream 元素收集到 `Map`，根据 keyMapper 生成键，valueMapper 生成值。

- **参数**: `keyMapper` — 键映射函数；`valueMapper` — 值映射函数
- **返回**: `Collector`

### toMap(BinaryOperator)

```java
public static <T,K,U> Collector<T,?,Map<K,U>> toMap(Function<? super T,? extends K> keyMapper, Function<? super T,? extends U> valueMapper, BinaryOperator<U> mergeFunction)
```

将 Stream 元素收集到 `Map`，如果键冲突则用 mergeFunction 合并值。

- **参数**: `keyMapper` — 键映射函数；`valueMapper` — 值映射函数；`mergeFunction` — 冲突合并函数
- **返回**: `Collector`

### joining

```java
public static Collector<CharSequence,?,String> joining()
```

将 Stream 中的字符串直接拼接。

- **返回**: `Collector`

### joining(CharSequence)

```java
public static Collector<CharSequence,?,String> joining(CharSequence delimiter)
```

将 Stream 中的字符串按分隔符拼接。

- **参数**: `delimiter` — 分隔符
- **返回**: `Collector`

### joining(CharSequence, CharSequence, CharSequence)

```java
public static Collector<CharSequence,?,String> joining(CharSequence delimiter, CharSequence prefix, CharSequence suffix)
```

将 Stream 中的字符串按分隔符拼接，并添加前缀和后缀。

- **参数**: `delimiter` — 分隔符；`prefix` — 前缀；`suffix` — 后缀
- **返回**: `Collector`

### mapping

```java
public static <T,U,A,R> Collector<T,?,R> mapping(Function<? super T,? extends U> mapper, Collector<? super U,A,R> downstream)
```

先对元素应用转换函数，再收集到下游 Collector。

- **参数**: `mapper` — 转换函数；`downstream` — 下游收集器
- **返回**: `Collector`

### groupingBy

```java
public static <T,K> Collector<T,?,Map<K,List<T>>> groupingBy(Function<? super T,? extends K> classifier)
```

按分类函数对元素分组，返回 `Map<K, List<T>>`。

- **参数**: `classifier` — 分类函数
- **返回**: `Collector`

### groupingBy(Function, Collector)

```java
public static <T,K,A,D> Collector<T,?,Map<K,D>> groupingBy(Function<? super T,? extends K> classifier, Collector<? super T,A,D> downstream)
```

按分类函数对元素分组，并用下游 Collector 处理各组元素。

- **参数**: `classifier` — 分类函数；`downstream` — 下游收集器
- **返回**: `Collector`

### partitioningBy

```java
public static <T> Collector<T,?,Map<Boolean,List<T>>> partitioningBy(Predicate<? super T> predicate)
```

按谓词对元素分区（true/false）。

- **参数**: `predicate` — 划分条件
- **返回**: `Collector`

### summingInt

```java
public static <T> Collector<T,?,Integer> summingInt(ToIntFunction<? super T> mapper)
```

对元素应用获取 int 的函数并求和。

- **参数**: `mapper` — 取 int 的函数
- **返回**: `Collector`

### averagingInt

```java
public static <T> Collector<T,?,Double> averagingInt(ToIntFunction<? super T> mapper)
```

对元素应用获取 int 的函数并计算平均值。

- **参数**: `mapper` — 取 int 的函数
- **返回**: `Collector`

### summarizingInt

```java
public static <T> Collector<T,?,IntSummaryStatistics> summarizingInt(ToIntFunction<? super T> mapper)
```

对元素应用获取 int 的函数并收集统计信息（计数、和、均值、最大、最小）。

- **参数**: `mapper` — 取 int 的函数
- **返回**: `Collector`

### maxBy

```java
public static <T> Collector<T,?,Optional<T>> maxBy(Comparator<? super T> comparator)
```

按比较器找出最大元素。

- **参数**: `comparator` — 比较器
- **返回**: `Collector`

### minBy

```java
public static <T> Collector<T,?,Optional<T>> minBy(Comparator<? super T> comparator)
```

按比较器找出最小元素。

- **参数**: `comparator` — 比较器
- **返回**: `Collector`

### counting

```java
public static <T> Collector<T,?,Long> counting()
```

统计元素数量。

- **返回**: `Collector`

## 测试

### toList

- 描述: 收集 Stream 到 List
- 断言: 收集后的 List 包含所有元素

```java
// 方法体开始
System.out.println("=== toList ===");
List<String> list = Stream.of("a", "b", "c").collect(Collectors.toList());
System.out.println("收集到 List: " + list);
assertEquals(3, list.size());
assertEquals("a", list.get(0));
assertEquals("b", list.get(1));
assertEquals("c", list.get(2));
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### toSet

- 描述: 收集 Stream 到 Set
- 断言: 重复元素被去重

```java
// 方法体开始
System.out.println("=== toSet ===");
Set<String> set = Stream.of("a", "b", "a", "c", "b").collect(Collectors.toSet());
System.out.println("收集到 Set: " + set);
assertEquals(3, set.size());
assertTrue(set.contains("a"));
assertTrue(set.contains("b"));
assertTrue(set.contains("c"));
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### toMap

- 描述: 收集 Stream 到 Map
- 断言: keyMapper 和 valueMapper 正确生成键值对

```java
// 方法体开始
System.out.println("=== toMap ===");
Map<String, Integer> map = Stream.of("a", "b", "c")
    .collect(Collectors.toMap(Function.identity(), s -> s.length()));
System.out.println("收集到 Map: " + map);
assertEquals(Integer.valueOf(1), map.get("a"));
assertEquals(Integer.valueOf(1), map.get("b"));
assertEquals(Integer.valueOf(1), map.get("c"));
assertEquals(3, map.size());
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### toMapWithMerge

- 描述: 带冲突合并的 toMap
- 断言: 键冲突时使用合并函数

```java
// 方法体开始
System.out.println("=== toMapWithMerge ===");
Map<Integer, String> map = Stream.of("aa", "b", "ccc", "dd")
    .collect(Collectors.toMap(
        s -> s.length(),
        s -> s,
        (a, b) -> a + "," + b
    ));
System.out.println("合并后的 Map: " + map);
assertEquals("aa,dd", map.get(2));
assertEquals("b", map.get(1));
assertEquals("ccc", map.get(3));
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### joining

- 描述: 直接拼接字符串
- 断言: 拼接后为 "abc"

```java
// 方法体开始
System.out.println("=== joining ===");
String result = Stream.of("a", "b", "c").collect(Collectors.joining());
System.out.println("直接拼接: " + result);
assertEquals("abc", result);
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### joiningDelimiter

- 描述: 按分隔符拼接字符串
- 断言: 逗号分隔结果为 "a,b,c"

```java
// 方法体开始
System.out.println("=== joiningDelimiter ===");
String result = Stream.of("a", "b", "c").collect(Collectors.joining(","));
System.out.println("逗号拼接: " + result);
assertEquals("a,b,c", result);
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### joiningPrefixSuffix

- 描述: 带前后缀的字符串拼接
- 断言: 结果为 "[a, b, c]"

```java
// 方法体开始
System.out.println("=== joiningPrefixSuffix ===");
String result = Stream.of("a", "b", "c").collect(Collectors.joining(", ", "[", "]"));
System.out.println("带前后缀拼接: " + result);
assertEquals("[a, b, c]", result);
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### mapping

- 描述: 先转换再收集
- 断言: 转换后收集到 List

```java
// 方法体开始
System.out.println("=== mapping ===");
List<Integer> list = Stream.of("hello", "world")
    .collect(Collectors.mapping(String::length, Collectors.toList()));
System.out.println("字符串长度: " + list);
assertEquals(2, list.size());
assertEquals(Integer.valueOf(5), list.get(0));
assertEquals(Integer.valueOf(5), list.get(1));
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### groupingBy

- 描述: 按字符串长度分组
- 断言: 分组后每个键对应正确元素列表

```java
// 方法体开始
System.out.println("=== groupingBy ===");
Map<Integer, List<String>> map = Stream.of("a", "bb", "cc", "ddd")
    .collect(Collectors.groupingBy(String::length));
System.out.println("分组结果: " + map);
assertEquals(1, map.get(1).size());
assertEquals(2, map.get(2).size());
assertEquals(1, map.get(3).size());
assertTrue(map.get(2).contains("bb"));
assertTrue(map.get(2).contains("cc"));
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### groupingByDownstream

- 描述: 分组后统计每组元素数量
- 断言: 每组计数正确

```java
// 方法体开始
System.out.println("=== groupingByDownstream ===");
Map<Integer, Long> map = Stream.of("a", "bb", "cc", "dd", "eee")
    .collect(Collectors.groupingBy(String::length, Collectors.counting()));
System.out.println("每组数量: " + map);
assertEquals(Long.valueOf(1), map.get(1));
assertEquals(Long.valueOf(3), map.get(2));
assertEquals(Long.valueOf(1), map.get(3));
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### partitioningBy

- 描述: 按条件分区
- 断言: 分区结果包含 true 和 false 两个组

```java
// 方法体开始
System.out.println("=== partitioningBy ===");
Map<Boolean, List<Integer>> map = Stream.of(1, 2, 3, 4, 5, 6)
    .collect(Collectors.partitioningBy(n -> n % 2 == 0));
System.out.println("偶数: " + map.get(true) + ", 奇数: " + map.get(false));
assertEquals(3, map.get(true).size());
assertEquals(3, map.get(false).size());
assertTrue(map.get(true).contains(2));
assertTrue(map.get(false).contains(1));
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### summingInt

- 描述: 求和
- 断言: 元素长度之和正确

```java
// 方法体开始
System.out.println("=== summingInt ===");
int sum = Stream.of("apple", "banana", "cat")
    .collect(Collectors.summingInt(String::length));
System.out.println("长度之和: " + sum);
assertEquals(5 + 6 + 3, sum);
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### averagingInt

- 描述: 计算平均值
- 断言: 平均值正确

```java
// 方法体开始
System.out.println("=== averagingInt ===");
double avg = Stream.of(10, 20, 30)
    .collect(Collectors.averagingInt(n -> n));
System.out.println("平均值: " + avg);
assertEquals(20.0, avg, 0.001);
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### summarizingInt

- 描述: 收集统计信息
- 断言: 统计信息正确

```java
// 方法体开始
System.out.println("=== summarizingInt ===");
IntSummaryStatistics stats = Stream.of(1, 2, 3, 4, 5)
    .collect(Collectors.summarizingInt(n -> n));
System.out.println("统计: 计数=" + stats.getCount() + " 和=" + stats.getSum() + " 平均=" + stats.getAverage() + " 最大=" + stats.getMax() + " 最小=" + stats.getMin());
assertEquals(5, stats.getCount());
assertEquals(15, stats.getSum());
assertEquals(3.0, stats.getAverage(), 0.001);
assertEquals(5, stats.getMax());
assertEquals(1, stats.getMin());
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### maxBy

- 描述: 找出最大元素
- 断言: 最长字符串为 "banana"

```java
// 方法体开始
System.out.println("=== maxBy ===");
Optional<String> max = Stream.of("apple", "banana", "cat")
    .collect(Collectors.maxBy(Comparator.comparingInt(String::length)));
assertTrue(max.isPresent());
assertEquals("banana", max.get());
System.out.println("最长字符串: " + max.get());
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### minBy

- 描述: 找出最小元素
- 断言: 最短字符串为 "cat"

```java
// 方法体开始
System.out.println("=== minBy ===");
Optional<String> min = Stream.of("apple", "banana", "cat")
    .collect(Collectors.minBy(Comparator.comparingInt(String::length)));
assertTrue(min.isPresent());
assertEquals("cat", min.get());
System.out.println("最短字符串: " + min.get());
System.out.println("=== 测试通过 ===");
// 方法体结束
```



### counting

- 描述: 统计元素数量
- 断言: 数量为 5

```java
// 方法体开始
System.out.println("=== counting ===");
long count = Stream.of("a", "b", "c", "d", "e").collect(Collectors.counting());
System.out.println("元素数量: " + count);
assertEquals(5L, count);
System.out.println("=== 测试通过 ===");
// 方法体结束
```
