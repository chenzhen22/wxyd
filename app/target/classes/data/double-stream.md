---
name: DoubleStream
package: java.util.stream
order: 37
---

## 介绍

`java.util.stream.DoubleStream` 是 Java 8 引入的专门处理 `double` 基本类型的特化 Stream。它提供了针对 double 类型的高效数值操作，避免自动装箱/拆箱的开销。

常见用途：
- **聚合计算**：`sum()`、`average()`、`min()`、`max()`、`count()`
- **类型转换**：`boxed()` 转为 `Stream<Double>`、`mapToInt()` 转为 `IntStream`
- **流水线操作**：`filter()`、`sorted()`、`distinct()` 等中间操作

## 方法

### of

```java
public static DoubleStream of(double... values)
```

返回元素为指定 double 值的顺序 DoubleStream。

- **参数**: `values` — 可变参数，要包含的 double 值
- **返回**: `DoubleStream`

### average

```java
public OptionalDouble average()
```

返回 DoubleStream 中所有元素的算术平均值。如果 Stream 为空则返回空的 OptionalDouble。

- **参数**: 无
- **返回**: `OptionalDouble`

### sum

```java
public double sum()
```

返回 DoubleStream 中所有元素的和。

- **参数**: 无
- **返回**: `double` — 元素之和

### min

```java
public OptionalDouble min()
```

返回 DoubleStream 中最小的元素。如果 Stream 为空则返回空的 OptionalDouble。

- **参数**: 无
- **返回**: `OptionalDouble`

### max

```java
public OptionalDouble max()
```

返回 DoubleStream 中最大的元素。如果 Stream 为空则返回空的 OptionalDouble。

- **参数**: 无
- **返回**: `OptionalDouble`

### count

```java
public long count()
```

返回 DoubleStream 中元素的数量。

- **参数**: 无
- **返回**: `long`

### boxed

```java
public Stream<Double> boxed()
```

将 DoubleStream 中的每个 double 装箱为 Double，返回 `Stream<Double>`。

- **参数**: 无
- **返回**: `Stream<Double>`

### mapToInt

```java
public IntStream mapToInt(DoubleToIntFunction mapper)
```

将 DoubleStream 中的每个元素通过映射函数转换为 int，返回 IntStream。

- **参数**: `mapper` — 将 double 映射为 int 的函数
- **返回**: `IntStream`

### filter

```java
public DoubleStream filter(DoublePredicate predicate)
```

返回由满足给定谓词条件的元素组成的新 DoubleStream。

- **参数**: `predicate` — 判断条件
- **返回**: `DoubleStream`

### sorted

```java
public DoubleStream sorted()
```

返回由该 DoubleStream 元素按自然顺序排序后的新 DoubleStream。

- **参数**: 无
- **返回**: `DoubleStream`

### distinct

```java
public DoubleStream distinct()
```

返回由该 DoubleStream 中不重复元素组成的新 DoubleStream。

- **参数**: 无
- **返回**: `DoubleStream`

### toArray

```java
public double[] toArray()
```

返回包含该 DoubleStream 元素的 double 数组。

- **参数**: 无
- **返回**: `double[]`

## 测试

### of

- 描述: 使用 of 创建 DoubleStream
- 断言: 包含指定元素

```java
// 方法体开始
System.out.println("=== of ===");
double[] arr = DoubleStream.of(1.1, 2.2, 3.3, 4.4, 5.5).toArray();
System.out.println("DoubleStream.of 结果: " + Arrays.toString(arr));
assertEquals(5, arr.length);
assertEquals(1.1, arr[0], 0.001);
assertEquals(3.3, arr[2], 0.001);
assertEquals(5.5, arr[4], 0.001);
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### average

- 描述: 计算平均值
- 断言: (1.0+2.0+3.0)/3 = 2.0

```java
// 方法体开始
System.out.println("=== average ===");
OptionalDouble avg = DoubleStream.of(1.0, 2.0, 3.0).average();
System.out.println("average: " + avg);
assertTrue(avg.isPresent());
assertEquals(2.0, avg.getAsDouble(), 0.001);
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### sum

- 描述: 计算 DoubleStream 元素之和
- 断言: 1.0+2.0+3.0+4.0+5.0 = 15.0

```java
// 方法体开始
System.out.println("=== sum ===");
double result = DoubleStream.of(1.0, 2.0, 3.0, 4.0, 5.0).sum();
System.out.println("sum: " + result);
assertEquals(15.0, result, 0.001);
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### min

- 描述: 找出 DoubleStream 中的最小值
- 断言: [3.5, 1.2, 4.8, 2.1, 9.7] 最小值为 1.2

```java
// 方法体开始
System.out.println("=== min ===");
OptionalDouble minVal = DoubleStream.of(3.5, 1.2, 4.8, 2.1, 9.7).min();
System.out.println("min: " + minVal);
assertTrue(minVal.isPresent());
assertEquals(1.2, minVal.getAsDouble(), 0.001);
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### max

- 描述: 找出 DoubleStream 中的最大值
- 断言: [3.5, 1.2, 4.8, 2.1, 9.7] 最大值为 9.7

```java
// 方法体开始
System.out.println("=== max ===");
OptionalDouble maxVal = DoubleStream.of(3.5, 1.2, 4.8, 2.1, 9.7).max();
System.out.println("max: " + maxVal);
assertTrue(maxVal.isPresent());
assertEquals(9.7, maxVal.getAsDouble(), 0.001);
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### boxed

- 描述: 将 DoubleStream 装箱为 Stream<Double>
- 断言: 装箱后可以用 collect 收集为 List

```java
// 方法体开始
System.out.println("=== boxed ===");
List<Double> list = DoubleStream.of(1.1, 2.2, 3.3)
    .boxed()
    .collect(Collectors.toList());
System.out.println("boxed 收集结果: " + list);
assertEquals(3, list.size());
assertEquals(Double.valueOf(1.1), list.get(0));
assertEquals(Double.valueOf(3.3), list.get(2));
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### filter

- 描述: 过滤出大于 3.0 的元素
- 断言: [1.5, 2.5, 3.5, 4.5, 5.5] 过滤后为 [3.5, 4.5, 5.5]

```java
// 方法体开始
System.out.println("=== filter ===");
double[] filtered = DoubleStream.of(1.5, 2.5, 3.5, 4.5, 5.5)
    .filter(d -> d > 3.0)
    .toArray();
System.out.println("过滤结果: " + Arrays.toString(filtered));
assertEquals(3, filtered.length);
assertEquals(3.5, filtered[0], 0.001);
assertEquals(5.5, filtered[2], 0.001);
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### pipeline

- 描述: 链式组合多个操作（filter + sorted + mapToInt）
- 断言: 过滤出大于 3.0 的元素，排序后转为 int，结果为 [3,4,5,6]

```java
// 方法体开始
System.out.println("=== pipeline ===");
int[] result = DoubleStream.of(5.5, 3.5, 6.5, 4.5, 2.5)
    .filter(d -> d > 3.0)
    .sorted()
    .mapToInt(d -> (int) d)
    .toArray();
System.out.println("pipeline 结果: " + Arrays.toString(result));
assertEquals(4, result.length);
assertEquals(3, result[0]);
assertEquals(4, result[1]);
assertEquals(5, result[2]);
assertEquals(6, result[3]);
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### count

- 描述: 统计元素数量
- 断言: 数量正确

```java
// 方法体开始
System.out.println("=== count ===");
long cnt = DoubleStream.of(1.0, 2.0, 3.0).count();
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
int sum = DoubleStream.of(1.0, 2.0, 3.0).mapToInt(d -> (int)d).sum();
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
double[] arr = DoubleStream.of(3.0, 1.0, 2.0).sorted().toArray();
assertArrayEquals(new double[]{1.0, 2.0, 3.0}, arr, 0.001);
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
double[] arr = DoubleStream.of(1.0, 2.0, 1.0, 3.0).distinct().toArray();
assertEquals(3, arr.length);
System.out.println("distinct: " + java.util.Arrays.toString(arr));
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### toArray

- 描述: 转换为数组
- 断言: 数组内容正确

```java
// 方法体开始
System.out.println("=== toArray ===");
double[] arr = DoubleStream.of(1.0, 2.0, 3.0).toArray();
assertArrayEquals(new double[]{1.0, 2.0, 3.0}, arr, 0.001);
System.out.println("toArray: " + java.util.Arrays.toString(arr));
System.out.println("=== 测试通过 ===");
// 方法体结束
```

