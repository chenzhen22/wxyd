---
name: LongStream
package: java.util.stream
order: 36
---

## 介绍

`java.util.stream.LongStream` 是 Java 8 引入的专门处理 `long` 基本类型的特化 Stream。它提供了针对 long 类型的高效数值操作，避免自动装箱/拆箱的开销。

常见用途：
- **数值范围生成**：`range()`、`rangeClosed()` 生成连续整数序列
- **聚合计算**：`sum()`、`average()`、`min()`、`max()`、`count()`
- **类型转换**：`boxed()` 转为 `Stream<Long>`、`mapToInt()` 转为 `IntStream`
- **流水线操作**：`filter()`、`sorted()`、`distinct()` 等中间操作

## 方法

### of

```java
public static LongStream of(long... values)
```

返回元素为指定 long 值的顺序 LongStream。

- **参数**: `values` — 可变参数，要包含的 long 值
- **返回**: `LongStream`

### range

```java
public static LongStream range(long startInclusive, long endExclusive)
```

返回从 `startInclusive`（包含）到 `endExclusive`（不包含）范围内步进为 1 的顺序 LongStream。

- **参数**: `startInclusive` — 起始值（包含）；`endExclusive` — 结束值（不包含）
- **返回**: `LongStream`

### rangeClosed

```java
public static LongStream rangeClosed(long startInclusive, long endInclusive)
```

返回从 `startInclusive`（包含）到 `endInclusive`（包含）范围内步进为 1 的顺序 LongStream。

- **参数**: `startInclusive` — 起始值（包含）；`endInclusive` — 结束值（包含）
- **返回**: `LongStream`

### forEach

```java
public void forEach(LongConsumer action)
```

对 LongStream 中的每个元素执行给定的操作。这是一个终端操作，执行后 Stream 被消费。

- **参数**: `action` — 对每个元素执行的动作
- **返回**: `void`

### sum

```java
public long sum()
```

返回 LongStream 中所有元素的和。

- **参数**: 无
- **返回**: `long` — 元素之和

### average

```java
public OptionalDouble average()
```

返回 LongStream 中所有元素的算术平均值。如果 Stream 为空则返回空的 OptionalDouble。

- **参数**: 无
- **返回**: `OptionalDouble` — 包含平均值的 Optional

### min

```java
public OptionalLong min()
```

返回 LongStream 中最小的元素。如果 Stream 为空则返回空的 OptionalLong。

- **参数**: 无
- **返回**: `OptionalLong` — 包含最小值的 Optional

### max

```java
public OptionalLong max()
```

返回 LongStream 中最大的元素。如果 Stream 为空则返回空的 OptionalLong。

- **参数**: 无
- **返回**: `OptionalLong` — 包含最大值的 Optional

### count

```java
public long count()
```

返回 LongStream 中元素的数量。

- **参数**: 无
- **返回**: `long` — 元素数量

### boxed

```java
public Stream<Long> boxed()
```

将 LongStream 中的每个 long 装箱为 Long，返回 `Stream<Long>`。

- **参数**: 无
- **返回**: `Stream<Long>` — 装箱后的 Stream

### mapToInt

```java
public IntStream mapToInt(LongToIntFunction mapper)
```

将 LongStream 中的每个元素通过映射函数转换为 int，返回 IntStream。

- **参数**: `mapper` — 将 long 映射为 int 的函数
- **返回**: `IntStream`

### filter

```java
public LongStream filter(LongPredicate predicate)
```

返回由满足给定谓词条件的元素组成的新 LongStream。

- **参数**: `predicate` — 判断条件
- **返回**: `LongStream`

### sorted

```java
public LongStream sorted()
```

返回由该 LongStream 元素按自然顺序排序后的新 LongStream。

- **参数**: 无
- **返回**: `LongStream`

### distinct

```java
public LongStream distinct()
```

返回由该 LongStream 中不重复元素组成的新 LongStream。

- **参数**: 无
- **返回**: `LongStream`

### toArray

```java
public long[] toArray()
```

返回包含该 LongStream 元素的 long 数组。

- **参数**: 无
- **返回**: `long[]`

## 测试

### of

- 描述: 使用 of 创建 LongStream
- 断言: 包含指定元素

```java
// 方法体开始
System.out.println("=== of ===");
long[] arr = LongStream.of(10L, 20L, 30L, 40L, 50L).toArray();
System.out.println("LongStream.of 结果: " + Arrays.toString(arr));
assertEquals(5, arr.length);
assertEquals(10L, arr[0]);
assertEquals(30L, arr[2]);
assertEquals(50L, arr[4]);
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### range

- 描述: 使用 range 生成范围序列
- 断言: 生成 1 到 4 的序列（不包含 5）

```java
// 方法体开始
System.out.println("=== range ===");
long[] arr = LongStream.range(1L, 5L).toArray();
System.out.println("range(1,5) 结果: " + Arrays.toString(arr));
assertEquals(4, arr.length);
assertEquals(1L, arr[0]);
assertEquals(4L, arr[3]);
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### rangeClosed

- 描述: 使用 rangeClosed 生成闭合范围序列
- 断言: 生成 1 到 5 的序列（包含 5）

```java
// 方法体开始
System.out.println("=== rangeClosed ===");
long[] arr = LongStream.rangeClosed(1L, 5L).toArray();
System.out.println("rangeClosed(1,5) 结果: " + Arrays.toString(arr));
assertEquals(5, arr.length);
assertEquals(1L, arr[0]);
assertEquals(5L, arr[4]);
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### sum

- 描述: 计算 LongStream 元素之和
- 断言: 1+2+3+4+5 = 15

```java
// 方法体开始
System.out.println("=== sum ===");
long result = LongStream.rangeClosed(1L, 5L).sum();
System.out.println("sum: " + result);
assertEquals(15L, result);
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### average

- 描述: 计算 LongStream 元素平均值
- 断言: (1+2+3+4+5)/5 = 3.0

```java
// 方法体开始
System.out.println("=== average ===");
OptionalDouble avg = LongStream.rangeClosed(1L, 5L).average();
System.out.println("average: " + avg);
assertTrue(avg.isPresent());
assertEquals(3.0, avg.getAsDouble(), 0.001);
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### min

- 描述: 找出 LongStream 中的最小值
- 断言: [3,1,4,1,5,9] 最小值为 1

```java
// 方法体开始
System.out.println("=== min ===");
OptionalLong min = LongStream.of(3L, 1L, 4L, 1L, 5L, 9L).min();
System.out.println("min: " + min);
assertTrue(min.isPresent());
assertEquals(1L, min.getAsLong());
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### max

- 描述: 找出 LongStream 中的最大值
- 断言: [3,1,4,1,5,9] 最大值为 9

```java
// 方法体开始
System.out.println("=== max ===");
OptionalLong max = LongStream.of(3L, 1L, 4L, 1L, 5L, 9L).max();
System.out.println("max: " + max);
assertTrue(max.isPresent());
assertEquals(9L, max.getAsLong());
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### boxed

- 描述: 将 LongStream 装箱为 Stream<Long>
- 断言: 装箱后可以用 collect 收集为 List

```java
// 方法体开始
System.out.println("=== boxed ===");
List<Long> list = LongStream.rangeClosed(1L, 5L)
    .boxed()
    .collect(Collectors.toList());
System.out.println("boxed 收集结果: " + list);
assertEquals(5, list.size());
assertEquals(Long.valueOf(1L), list.get(0));
assertEquals(Long.valueOf(5L), list.get(4));
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### filter

- 描述: 过滤出偶数
- 断言: [1,2,3,4,5,6] 过滤出偶数后为 [2,4,6]

```java
// 方法体开始
System.out.println("=== filter ===");
long[] evens = LongStream.of(1L, 2L, 3L, 4L, 5L, 6L)
    .filter(n -> n % 2 == 0)
    .toArray();
System.out.println("过滤出的偶数: " + Arrays.toString(evens));
assertEquals(3, evens.length);
assertEquals(2L, evens[0]);
assertEquals(4L, evens[1]);
assertEquals(6L, evens[2]);
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### pipeline

- 描述: 链式组合多个操作（filter + sorted + mapToInt）
- 断言: 过滤出大于2的奇数，排序后转为 int，结果为 [3,5]

```java
// 方法体开始
System.out.println("=== pipeline ===");
int[] result = LongStream.of(5L, 3L, 1L, 4L, 2L, 6L)
    .filter(n -> n % 2 != 0)
    .filter(n -> n > 2)
    .sorted()
    .mapToInt(n -> (int) n)
    .toArray();
System.out.println("pipeline 结果: " + Arrays.toString(result));
assertEquals(2, result.length);
assertEquals(3, result[0]);
assertEquals(5, result[1]);
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### forEach

- 描述: 遍历元素
- 断言: 遍历所有元素

```java
// 方法体开始
System.out.println("=== forEach ===");
StringBuilder sb = new StringBuilder();
LongStream.of(1, 2, 3).forEach(i -> sb.append(i));
assertEquals("123", sb.toString());
System.out.println("forEach: " + sb.toString());
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### count

- 描述: 统计元素数量
- 断言: 数量正确

```java
// 方法体开始
System.out.println("=== count ===");
long cnt = LongStream.of(10, 20, 30).count();
assertEquals(3, cnt);
System.out.println("count: " + cnt);
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### mapToInt

- 描述: 转换为 IntStream
- 断言: 转换后元素正确

```java
// 方法体开始
System.out.println("=== mapToInt ===");
int sum = LongStream.of(1, 2, 3).mapToInt(l -> (int)l).sum();
assertEquals(6, sum);
System.out.println("mapToInt sum: " + sum);
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### sorted

- 描述: 排序
- 断言: 排序后元素有序

```java
// 方法体开始
System.out.println("=== sorted ===");
long[] arr = LongStream.of(3, 1, 2).sorted().toArray();
assertArrayEquals(new long[]{1, 2, 3}, arr);
System.out.println("sorted: " + java.util.Arrays.toString(arr));
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### distinct

- 描述: 去重
- 断言: 去重后元素唯一

```java
// 方法体开始
System.out.println("=== distinct ===");
long[] arr = LongStream.of(1, 2, 1, 3).distinct().toArray();
assertEquals(3, arr.length);
System.out.println("distinct: " + java.util.Arrays.toString(arr));
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### toArray

- 描述: 转换为 long 数组
- 断言: 数组内容正确

```java
// 方法体开始
System.out.println("=== toArray ===");
long[] arr = LongStream.of(100, 200, 300).toArray();
assertArrayEquals(new long[]{100, 200, 300}, arr);
System.out.println("toArray: " + java.util.Arrays.toString(arr));
System.out.println("=== 测试通过 ===");
// 方法体结束
```

