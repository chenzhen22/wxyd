---
name: Spliterators
package: java.util
order: 136
---

## 介绍

`java.util.Spliterators` 是 Java 8 引入的**Spliterator 工具类**，包含用于创建 Spliterator 的静态工厂方法和抽象类，与 `Spliterator` 接口配对使用。

Spliterators 提供了：
- **抽象类**：`Spliterators.AbstractSpliterator` — 方便实现自定义 Spliterator
- **工厂方法**：创建原始类型的 OfInt、OfLong、OfDouble 的 Spliterator
- **适配方法**：从 Iterator 适配为 Spliterator

## 方法

### spliterator 数组

```java
public static Spliterator.OfInt spliterator(int[] array, int additionalCharacteristics)
public static Spliterator.OfLong spliterator(long[] array, int additionalCharacteristics)
public static Spliterator.OfDouble spliterator(double[] array, int additionalCharacteristics)
```

从数组创建原始类型 Spliterator。

### spliterator 适配 Iterator

```java
public static <T> Spliterator<T> spliterator(Iterator<? extends T> iterator, long size, int characteristics)
```

从 Iterator 适配为 Spliterator（含精确大小）。

### spliteratorUnknownSize

```java
public static <T> Spliterator<T> spliteratorUnknownSize(Iterator<? extends T> iterator, int characteristics)
```

从 Iterator 适配为 Spliterator（未知大小）。

## 测试

### spliterator 数组

- 描述: 从 int 数组创建 Spliterator 并遍历
- 断言: 遍历所有元素

```java
// 方法体开始
System.out.println("=== spliterator 数组 ===");
int[] array = {1, 2, 3, 4, 5};
Spliterator.OfInt spl = Spliterators.spliterator(array, Spliterator.SIZED);
int[] result = StreamSupport.intStream(spl, false).toArray();
assertArrayEquals(array, result);
System.out.println("数组元素: " + java.util.Arrays.toString(result));
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### 从 Iterator 适配

- 描述: 从 Iterator 创建 Spliterator
- 断言: 转换为列表正确

```java
// 方法体开始
System.out.println("=== Iterator 适配 ===");
Iterator<String> it = Arrays.asList("A", "B", "C").iterator();
Spliterator<String> spl = Spliterators.spliterator(it, 3, Spliterator.ORDERED);
List<String> result = new ArrayList<>();
spl.forEachRemaining(result::add);
assertEquals("[A, B, C]", result.toString());
System.out.println("结果: " + result);
System.out.println("=== 测试通过 ===");
// 方法体结束
```
