---
name: Integer
package: java.lang
order: 101
---

## 介绍

`java.lang.Integer` 包装类在 Java 8 中新增了多个实用静态方法，主要围绕**无符号运算**和**溢出安全运算**。这些方法让 `int` 的处理更加安全和便捷。

Java 8 新增的主要 Integer 方法：
- **无符号操作**：`toUnsignedString()`、`parseUnsignedInt()`、`divideUnsigned()`、`remainderUnsigned()`
- **安全比较**：`sum()`、`max()`、`min()`
- **位运算**：`toUnsignedLong()`、`compareUnsigned()`

## 方法

### sum

```java
public static int sum(int a, int b)
```

返回两个 int 值的和。与 `a + b` 等价，但作为方法引用更方便。

- **参数**: `a`, `b` — 两个 int 值
- **返回**: `int` — 两数之和

### max / min

```java
public static int max(int a, int b)
public static int min(int a, int b)
```

返回两个 int 值的较大值/较小值。

- **返回**: `int` — 较大值/较小值

### toUnsignedString

```java
public static String toUnsignedString(int i, int radix)
public static String toUnsignedString(int i)
```

将 int 值视为无符号数转换为字符串。对于负数，无符号表示会得到一个大的正数。

- **参数**: `i` — int 值；`radix` — 进制（2-36）
- **返回**: `String` — 无符号表示

### parseUnsignedInt

```java
public static int parseUnsignedInt(String s, int radix)
public static int parseUnsignedInt(String s)
```

将字符串解析为无符号 int。

- **参数**: `s` — 字符串；`radix` — 进制
- **返回**: `int` — 解析结果

### divideUnsigned / remainderUnsigned

```java
public static int divideUnsigned(int dividend, int divisor)
public static int remainderUnsigned(int dividend, int divisor)
```

无符号除法和无符号取余。

- **返回**: `int` — 商/余数

## 测试

### sum / max / min

- 描述: 使用 sum/max/min 方法
- 断言: 两个值的运算结果

```java
// 方法体开始
System.out.println("=== sum/max/min ===");
assertEquals(8, Integer.sum(5, 3));
assertEquals(10, Integer.max(5, 10));
assertEquals(3, Integer.min(5, 3));
System.out.println("sum(5,3)=" + Integer.sum(5, 3));
System.out.println("max(5,10)=" + Integer.max(5, 10));
System.out.println("min(5,3)=" + Integer.min(5, 3));
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### toUnsignedString

- 描述: 将负数转为无符号字符串
- 断言: -1 的无符号十六进制为 ffffffff

```java
// 方法体开始
System.out.println("=== toUnsignedString ===");
assertEquals(4294967295L, Integer.toUnsignedLong(-1));
assertEquals("ffffffff", Integer.toUnsignedString(-1, 16));
assertEquals("4294967295", Integer.toUnsignedString(-1));
System.out.println("-1 无符号: " + Integer.toUnsignedString(-1));
System.out.println("-1 无符号十六进制: " + Integer.toUnsignedString(-1, 16));
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### divideUnsigned

- 描述: 无符号除法
- 断言: -1 视为无符号数时除以 2 的结果

```java
// 方法体开始
System.out.println("=== divideUnsigned ===");
// -1 作为无符号数是 4294967295，除以 2 = 2147483647
assertEquals(2147483647, Integer.divideUnsigned(-1, 2));
assertEquals(2, Integer.divideUnsigned(10, 5));
System.out.println("divideUnsigned(-1, 2)=" + Integer.divideUnsigned(-1, 2));
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### 作为方法引用使用

- 描述: Integer.sum 作为 BinaryOperator 方法引用
- 断言: streams reduce 时使用

```java
// 方法体开始
System.out.println("=== 方法引用 ===");
int sum = Stream.of(1, 2, 3, 4, 5)
        .reduce(0, Integer::sum);
assertEquals(15, sum);
int max = Stream.of(3, 7, 2, 9, 5)
        .reduce(Integer.MIN_VALUE, Integer::max);
assertEquals(9, max);
System.out.println("sum: " + sum + ", max: " + max);
System.out.println("=== 测试通过 ===");
// 方法体结束
```
