---
name: Arrays
package: java.util
order: 23
---

## 介绍

`java.util.Arrays` 是 Java 中的**数组操作工具类**。Java 8 新增了 `parallelSort`、`stream`、`setAll`、`parallelSetAll`、`parallelPrefix` 等并行和函数式方法。

常见用途：
- **排序**：`sort`、`parallelSort`
- **搜索**：`binarySearch`
- **转换**：`asList`、`stream`、`toString`
- **填充/复制**：`fill`、`copyOf`、`copyOfRange`
- **批量赋值**：`setAll`、`parallelSetAll`
- **前缀计算**：`parallelPrefix`

## 方法

### sort

```java
public static void sort(int[] a)
```

对 int 数组进行升序排序。

### parallelSort

```java
public static void parallelSort(int[] a)
```

对 int 数组进行并行排序（Java 8 新增），多核环境下更快。

### parallelPrefix

```java
public static void parallelPrefix(int[] array, IntBinaryOperator op)
```

使用指定运算符对数组进行并行前缀计算（Java 8 新增）。如 `[1,2,3,4]` 求和得 `[1,3,6,10]`。

### setAll

```java
public static <T> void setAll(T[] array, IntFunction<? extends T> generator)
```

使用生成器函数为数组的每个元素赋值（Java 8 新增）。

### stream

```java
public static IntStream stream(int[] array)
```

将 int 数组转换为 `IntStream`（Java 8 新增）。

## 测试

### sort

- 描述: 排序 int 数组
- 断言: 排序后数组元素按升序排列

```java
// 方法体开始
System.out.println("=== sort ===");
int[] arr = {5, 3, 1, 4, 2};
Arrays.sort(arr);
assertArrayEquals(new int[]{1, 2, 3, 4, 5}, arr);
System.out.println("排序: " + Arrays.toString(arr));
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### parallelSort

- 描述: 并行排序
- 断言: 排序后数组正确

```java
// 方法体开始
System.out.println("=== parallelSort ===");
int[] arr = {9, 8, 7, 6, 5, 4, 3, 2, 1, 0};
Arrays.parallelSort(arr);
assertArrayEquals(new int[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9}, arr);
System.out.println("并行排序: " + Arrays.toString(arr));
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### setAll

- 描述: 使用 setAll 生成递增序列
- 断言: 数组元素为 i*i

```java
// 方法体开始
System.out.println("=== setAll ===");
Integer[] arr = new Integer[5];
Arrays.setAll(arr, i -> i * i);
assertEquals(Integer.valueOf(0), arr[0]);
assertEquals(Integer.valueOf(16), arr[4]);
System.out.println("setAll: " + Arrays.toString(arr));
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### parallelPrefix

- 描述: 使用 parallelPrefix 计算累加前缀和
- 断言: 1,2,3,4 → 1,3,6,10

```java
// 方法体开始
System.out.println("=== parallelPrefix ===");
int[] arr = {1, 2, 3, 4};
Arrays.parallelPrefix(arr, Integer::sum);
assertArrayEquals(new int[]{1, 3, 6, 10}, arr);
System.out.println("前缀和: " + Arrays.toString(arr));
System.out.println("=== 测试通过 ===");
// 方法体结束
```
