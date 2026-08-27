---
name: Long
package: java.lang
order: 113
---

## 介绍

`java.lang.Long` 包装类在 Java 8 中新增了多个实用静态方法，与 Integer 类似，主要围绕**无符号运算**。

Java 8 新增的 Long 方法：
- **无符号操作**：`toUnsignedString()`、`parseUnsignedLong()`、`divideUnsigned()`、`remainderUnsigned()`
- **安全比较**：`sum()`、`max()`、`min()`
- **无符号转换**：`toUnsignedString(long, int)`、`compareUnsigned()`

## 方法

### sum / max / min

```java
public static long sum(long a, long b)
public static long max(long a, long b)
public static long min(long a, long b)
```

返回两个 long 值的和/较大值/较小值。适合作为方法引用。

### toUnsignedString / parseUnsignedLong

```java
public static String toUnsignedString(long i, int radix)
public static String toUnsignedString(long i)
public static long parseUnsignedLong(String s)
```

无符号字符串转换和解析。

### divideUnsigned / remainderUnsigned

```java
public static long divideUnsigned(long dividend, long divisor)
public static long remainderUnsigned(long dividend, long divisor)
```

无符号除法和取余。

## 测试

### sum / max / min

- 描述: 基本运算方法
- 断言: 结果正确

```java
// 方法体开始
System.out.println("=== sum/max/min ===");
assertEquals(15L, Long.sum(10L, 5L));
assertEquals(20L, Long.max(10L, 20L));
assertEquals(5L, Long.min(10L, 5L));
System.out.println("sum(10,5)=" + Long.sum(10L, 5L));
System.out.println("max(10,20)=" + Long.max(10L, 20L));
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### toUnsignedString

- 描述: 负数转为无符号字符串
- 断言: -1 的无符号字符串为 18446744073709551615

```java
// 方法体开始
System.out.println("=== toUnsignedString ===");
assertEquals("18446744073709551615", Long.toUnsignedString(-1L));
assertEquals("ffffffffffffffff", Long.toUnsignedString(-1L, 16));
assertEquals("10", Long.toUnsignedString(16L, 16));
System.out.println("-1 无符号: " + Long.toUnsignedString(-1L));
System.out.println("-1 十六进制: " + Long.toUnsignedString(-1L, 16));
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### divideUnsigned

- 描述: 无符号除法
- 断言: -1 / 2 = 9223372036854775807

```java
// 方法体开始
System.out.println("=== divideUnsigned ===");
assertEquals(9223372036854775807L, Long.divideUnsigned(-1L, 2));
assertEquals(5L, Long.divideUnsigned(10L, 2L));
System.out.println("divideUnsigned(-1, 2)=" + Long.divideUnsigned(-1L, 2L));
System.out.println("=== 测试通过 ===");
// 方法体结束
```
