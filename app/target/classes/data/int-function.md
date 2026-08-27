---
name: IntFunction
package: java.util.function
order: 74
---

## 介绍

`IntFunction<R>` 是 Java 8 引入的一个**函数式接口**，代表一个接受 `int` 参数并返回结果的函数。与 `Function<Integer, R>` 不同，它直接操作原始 int，避免了装箱拆箱。

IntFunction 的核心特点：
- **int → 对象**：将原始 int 值映射为任意类型 R
- **与 toArray 集成**：`Stream.toArray(IntFunction)` 的常用参数
- **避免装箱**：直接接受原始 int 值

IntFunction 的单个方法：
- `apply(int value)` — 核心方法，对给定的 int 值执行转换并返回结果

对应的特化接口：
- `LongFunction<R>` — 接受 long 参数
- `DoubleFunction<R>` — 接受 double 参数
- `ToIntFunction<T>` — 反向：对象 → int

## 方法

### apply

```java
R apply(int value)
```

对给定 int 值执行转换并返回结果。

- **参数**: `value` — 输入的 int 值
- **返回**: `R` — 转换结果

## 测试

### apply 创建字符串

- 描述: 使用 `apply` 方法将 int 转为格式化字符串
- 断言: 5 转为 `"数字: 5"`

```java
// 方法体开始
System.out.println("=== apply 创建字符串 ===");
IntFunction<String> formatNum = v -> "数字: " + v;
assertEquals("数字: 5", formatNum.apply(5));
assertEquals("数字: 0", formatNum.apply(0));
System.out.println(formatNum.apply(5));
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### apply 创建对象

- 描述: 使用 IntFunction 根据大小创建 List
- 断言: 创建包含 3 个元素的列表

```java
// 方法体开始
System.out.println("=== apply 创建对象 ===");
IntFunction<List<String>> createList = size -> new ArrayList<>(size);
List<String> list = createList.apply(10);
assertTrue(list.isEmpty());
assertEquals(0, list.size());
System.out.println("创建列表成功, 初始大小: " + list.size());
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### Stream.toArray

- 描述: 使用 IntFunction 配合 `Stream.toArray` 指定数组类型
- 断言: 正确创建 String 数组

```java
// 方法体开始
System.out.println("=== Stream.toArray ===");
String[] array = Stream.of("a", "b", "c")
        .toArray(String[]::new);
assertEquals(3, array.length);
assertEquals("a", array[0]);
assertEquals("c", array[2]);
System.out.println("数组: " + java.util.Arrays.toString(array));
System.out.println("=== 测试通过 ===");
// 方法体结束
```
