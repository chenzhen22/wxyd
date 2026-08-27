---
name: IntStream
package: java.util.stream
order: 29
---

## 介绍

`java.util.stream.IntStream` 是 Java 8 引入的**原始类型 int 的特化 Stream**，专门用于处理 int 元素的流式操作。与 `Stream<Integer>` 相比，`IntStream` 避免了装箱/拆箱的开销，提供了更高的性能。

`IntStream` 支持 Stream 的所有常用操作（filter、map、sorted 等），同时额外提供了针对 int 类型特有的方法：
- **数值计算**：`sum()`、`average()`、`min()`、`max()`、`count()`
- **范围生成**：`range()`、`rangeClosed()` 生成整数序列
- **类型转换**：`boxed()`、`asLongStream()`、`asDoubleStream()`
- **统计收集**：`summaryStatistics()` 一次性获取多个统计值

## 方法

### of(int...)

```java
public static IntStream of(int... values)
```

返回元素为指定 int 值的顺序 IntStream。

- **参数**: `values` — 可变参数，要包含的 int 值
- **返回**: `IntStream` — 包含指定元素的 IntStream

### range

```java
public static IntStream range(int startInclusive, int endExclusive)
```

返回从 `startInclusive`（包含）到 `endExclusive`（不包含）的**递增** IntStream，步长为 1。

- **参数**: `startInclusive` — 起始值（包含）；`endExclusive` — 结束值（不包含）
- **返回**: `IntStream` — 范围 IntStream

### rangeClosed

```java
public static IntStream rangeClosed(int startInclusive, int endInclusive)
```

返回从 `startInclusive`（包含）到 `endInclusive`（包含）的递增 IntStream，步长为 1。

- **参数**: `startInclusive` — 起始值（包含）；`endInclusive` — 结束值（包含）
- **返回**: `IntStream` — 范围 IntStream

### forEach

```java
public void forEach(IntConsumer action)
```

对 IntStream 中的每个元素执行给定的操作。这是一个终端操作。

- **参数**: `action` — 对每个 int 元素执行的动作
- **返回**: `void`

### sum

```java
public int sum()
```

返回 IntStream 中所有元素的总和。这是一个终端操作。空流返回 0。

- **参数**: 无
- **返回**: `int` — 总和

### average

```java
public OptionalDouble average()
```

返回 IntStream 中所有元素的算术平均值。这是一个终端操作。空流返回 `OptionalDouble.empty()`。

- **参数**: 无
- **返回**: `OptionalDouble` — 平均值

### min

```java
public OptionalInt min()
```

返回 IntStream 中的最小元素。这是一个终端操作。空流返回 `OptionalInt.empty()`。

- **参数**: 无
- **返回**: `OptionalInt` — 最小值

### max

```java
public OptionalInt max()
```

返回 IntStream 中的最大元素。这是一个终端操作。空流返回 `OptionalInt.empty()`。

- **参数**: 无
- **返回**: `OptionalInt` — 最大值

### count

```java
public long count()
```

返回 IntStream 中元素的数量。这是一个终端操作。

- **参数**: 无
- **返回**: `long` — 元素数量

### boxed

```java
public Stream<Integer> boxed()
```

将 IntStream 中的每个 int 装箱为 `Integer`，返回 `Stream<Integer>`。适用于需要泛型 Stream 操作的场景。

- **参数**: 无
- **返回**: `Stream<Integer>` — 装箱后的 Stream

### asLongStream

```java
public LongStream asLongStream()
```

将 IntStream 中的每个 int 转换为 long，返回 `LongStream`。

- **参数**: 无
- **返回**: `LongStream` — 转换后的 LongStream

### map

```java
public IntStream map(IntUnaryOperator mapper)
```

将每个元素通过给定的映射函数转换，返回包含转换后元素的新 IntStream。

- **参数**: `mapper` — 转换函数
- **返回**: `IntStream` — 转换后的 IntStream

### filter

```java
public IntStream filter(IntPredicate predicate)
```

返回由满足给定谓词条件的 int 元素组成的新 IntStream。

- **参数**: `predicate` — 判断条件
- **返回**: `IntStream` — 过滤后的 IntStream

### sorted

```java
public IntStream sorted()
```

返回由该 IntStream 元素按升序排序后的新 IntStream。

- **参数**: 无
- **返回**: `IntStream` — 排序后的 IntStream

### distinct

```java
public IntStream distinct()
```

返回由该 IntStream 中不重复元素组成的新 IntStream。

- **参数**: 无
- **返回**: `IntStream` — 去重后的 IntStream

### limit

```java
public IntStream limit(long maxSize)
```

截断 IntStream，使其元素不超过给定数量。

- **参数**: `maxSize` — 要截取的元素数量
- **返回**: `IntStream` — 截断后的 IntStream

### skip

```java
public IntStream skip(long n)
```

跳过 IntStream 的前 `n` 个元素，返回由剩余元素组成的新 IntStream。

- **参数**: `n` — 要跳过的元素数量
- **返回**: `IntStream` — 跳过后的 IntStream

### toArray

```java
public int[] toArray()
```

返回包含该 IntStream 元素的 int 数组。这是一个终端操作。

- **参数**: 无
- **返回**: `int[]` — 包含所有元素的数组

## 测试

### of

- 描述: 使用 of(int...) 创建 IntStream
- 断言: 元素为 [1, 2, 3, 4, 5]

```java
// 方法体开始
System.out.println("=== of ===");
int[] result = IntStream.of(1, 2, 3, 4, 5).toArray();
System.out.println("元素: " + Arrays.toString(result));
assertEquals(5, result.length);
assertEquals(1, result[0]);
assertEquals(5, result[4]);
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### range

- 描述: 使用 range() 生成左闭右开区间
- 断言: range(1, 5) 生成 [1, 2, 3, 4]

```java
// 方法体开始
System.out.println("=== range ===");
int[] result = IntStream.range(1, 5).toArray();
System.out.println("range(1,5): " + Arrays.toString(result));
assertEquals(4, result.length);
assertEquals(1, result[0]);
assertEquals(4, result[3]);
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### rangeClosed

- 描述: 使用 rangeClosed() 生成左闭右闭区间
- 断言: rangeClosed(1, 5) 生成 [1, 2, 3, 4, 5]

```java
// 方法体开始
System.out.println("=== rangeClosed ===");
int[] result = IntStream.rangeClosed(1, 5).toArray();
System.out.println("rangeClosed(1,5): " + Arrays.toString(result));
assertEquals(5, result.length);
assertEquals(1, result[0]);
assertEquals(5, result[4]);
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### sum

- 描述: 使用 sum() 求和
- 断言: range(1, 11) 的和为 55

```java
// 方法体开始
System.out.println("=== sum ===");
int sum = IntStream.range(1, 11).sum();
System.out.println("1到10的和: " + sum);
assertEquals(55, sum);
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### average

- 描述: 使用 average() 计算平均值
- 断言: range(1, 6) 的平均值为 3.0

```java
// 方法体开始
System.out.println("=== average ===");
java.util.OptionalDouble avg = IntStream.range(1, 6).average();
System.out.println("1到5的平均值: " + avg.getAsDouble());
assertTrue(avg.isPresent());
assertEquals(3.0, avg.getAsDouble(), 0.0001);
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### min

- 描述: 使用 min() 查找最小值
- 断言: [5, 2, 8, 1, 9] 的最小值为 1

```java
// 方法体开始
System.out.println("=== min ===");
java.util.OptionalInt min = IntStream.of(5, 2, 8, 1, 9).min();
System.out.println("最小值: " + min.getAsInt());
assertTrue(min.isPresent());
assertEquals(1, min.getAsInt());
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### max

- 描述: 使用 max() 查找最大值
- 断言: [5, 2, 8, 1, 9] 的最大值为 9

```java
// 方法体开始
System.out.println("=== max ===");
java.util.OptionalInt max = IntStream.of(5, 2, 8, 1, 9).max();
System.out.println("最大值: " + max.getAsInt());
assertTrue(max.isPresent());
assertEquals(9, max.getAsInt());
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### boxed

- 描述: 使用 boxed() 将 IntStream 装箱为 Stream<Integer>
- 断言: 装箱后可以收集为 List<Integer>

```java
// 方法体开始
System.out.println("=== boxed ===");
List<Integer> list = IntStream.range(1, 6).boxed().collect(Collectors.toList());
System.out.println("装箱结果: " + list);
assertEquals(Arrays.asList(1, 2, 3, 4, 5), list);
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### filter

- 描述: 使用 filter() 筛选偶数
- 断言: [1,2,3,4,5,6] 中偶数为 [2,4,6]

```java
// 方法体开始
System.out.println("=== filter ===");
int[] evens = IntStream.of(1, 2, 3, 4, 5, 6)
    .filter(n -> n % 2 == 0)
    .toArray();
System.out.println("偶数: " + Arrays.toString(evens));
assertEquals(3, evens.length);
assertEquals(2, evens[0]);
assertEquals(6, evens[2]);
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### distinct

- 描述: 使用 distinct() 去重
- 断言: [1,1,2,3,2,3,4] 去重后为 [1,2,3,4]

```java
// 方法体开始
System.out.println("=== distinct ===");
int[] distinct = IntStream.of(1, 1, 2, 3, 2, 3, 4)
    .distinct()
    .sorted()
    .toArray();
System.out.println("去重结果: " + Arrays.toString(distinct));
assertEquals(4, distinct.length);
assertEquals(1, distinct[0]);
assertEquals(4, distinct[3]);
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### toArray

- 描述: 使用 toArray() 转为 int[]
- 断言: 数组元素正确

```java
// 方法体开始
System.out.println("=== toArray ===");
int[] array = IntStream.of(10, 20, 30, 40, 50).toArray();
System.out.println("数组: " + Arrays.toString(array));
assertEquals(5, array.length);
assertEquals(10, array[0]);
assertEquals(50, array[4]);
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### pipeline

- 描述: 综合流水线：filter + map + sorted + toArray
- 断言: [3,8,1,6,2,9,4,7] 筛选 >3 的偶数、翻倍、排序后为 [8,12,16]

```java
// 方法体开始
System.out.println("=== pipeline ===");
int[] result = IntStream.of(3, 8, 1, 6, 2, 9, 4, 7)
    .filter(n -> n > 3)
    .filter(n -> n % 2 == 0)
    .map(n -> n * 2)
    .sorted()
    .toArray();
System.out.println("管道结果: " + Arrays.toString(result));
assertEquals(3, result.length);
assertEquals(8, result[0]);
assertEquals(12, result[1]);
assertEquals(16, result[2]);
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### forEach

- 描述: 遍历 IntStream
- 断言: 遍历所有元素

```java
// 方法体开始
System.out.println("=== forEach ===");
StringBuilder sb = new StringBuilder();
IntStream.range(1, 4).forEach(i -> sb.append(i));
assertEquals("123", sb.toString());
System.out.println("forEach 结果: " + sb.toString());
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### count

- 描述: 统计 IntStream 元素数量
- 断言: 元素数量正确

```java
// 方法体开始
System.out.println("=== count ===");
long count = IntStream.range(1, 10).count();
System.out.println("range(1,10) 的元素数量: " + count);
assertEquals(9L, count);
count = IntStream.of().count();
System.out.println("空流元素数量: " + count);
assertEquals(0L, count);
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### asLongStream

- 描述: 将 IntStream 转换为 LongStream
- 断言: 转换后元素类型为 long

```java
// 方法体开始
System.out.println("=== asLongStream ===");
long[] longs = IntStream.range(1, 5).asLongStream().toArray();
System.out.println("转换结果: " + Arrays.toString(longs));
assertEquals(4, longs.length);
assertEquals(1L, longs[0]);
assertEquals(4L, longs[3]);
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### map

- 描述: 使用 map 转换元素
- 断言: 每个元素乘以 2

```java
// 方法体开始
System.out.println("=== map ===");
int[] result = IntStream.of(1, 2, 3).map(n -> n * 2).toArray();
System.out.println("map(x2): " + Arrays.toString(result));
assertEquals(3, result.length);
assertEquals(2, result[0]);
assertEquals(6, result[2]);
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### sorted

- 描述: 使用 sorted 排序
- 断言: 升序排序

```java
// 方法体开始
System.out.println("=== sorted ===");
int[] sorted = IntStream.of(5, 3, 1, 4, 2).sorted().toArray();
System.out.println("排序结果: " + Arrays.toString(sorted));
assertEquals(5, sorted.length);
assertEquals(1, sorted[0]);
assertEquals(5, sorted[4]);
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### limit

- 描述: 使用 limit 截取前 N 个元素
- 断言: 截取前 3 个元素

```java
// 方法体开始
System.out.println("=== limit ===");
int[] limited = IntStream.range(1, 10).limit(3).toArray();
System.out.println("limit(3): " + Arrays.toString(limited));
assertEquals(3, limited.length);
assertEquals(1, limited[0]);
assertEquals(3, limited[2]);
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### skip

- 描述: 使用 skip 跳过前 N 个元素
- 断言: 跳过前 3 个元素

```java
// 方法体开始
System.out.println("=== skip ===");
int[] skipped = IntStream.range(1, 8).skip(3).toArray();
System.out.println("skip(3): " + Arrays.toString(skipped));
assertEquals(4, skipped.length);
assertEquals(4, skipped[0]);
assertEquals(7, skipped[3]);
System.out.println("=== 测试通过 ===");
// 方法体结束
```
