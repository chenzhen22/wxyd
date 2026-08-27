---
name: BigDecimal
package: java.math
order: 24
---

## 介绍

`java.math.BigDecimal` 是 Java 中用于**高精度十进制数计算**的不可变类，可以精确表示任意大小和小数位数的十进制数，避免了 `double` 和 `float` 的精度丢失问题。

常见用途：
- **财务计算**：金额精确计算
- **精确运算**：加减乘除，控制精度和舍入
- **比较和格式化**：比较大小、设置小数位数

## 方法

### BigDecimal(int)

```java
public BigDecimal(int val)
```

从 int 构造 BigDecimal。

- **参数**: `val` — int 值

### BigDecimal(double)

```java
public BigDecimal(double val)
```

从 double 构造 BigDecimal（存在精度问题，一般不推荐）。

- **参数**: `val` — double 值

### BigDecimal(String)

```java
public BigDecimal(String val)
```

从字符串构造 BigDecimal，推荐使用这种方式，没有精度问题。

- **参数**: `val` — 数字字符串，如 "123.45"

### valueOf

```java
public static BigDecimal valueOf(double val)
```

将 double 转换为 BigDecimal（推荐使用，内部做了优化）。

- **参数**: `val` — double 值
- **返回**: `BigDecimal`

### valueOf(long, int)

```java
public static BigDecimal valueOf(long unscaledVal, int scale)
```

根据 unscaled 值和小数位数创建 BigDecimal。

- **参数**: `unscaledVal` — 无标度值；`scale` — 小数位数
- **返回**: `BigDecimal`

### add

```java
public BigDecimal add(BigDecimal augend)
```

加法。

- **参数**: `augend` — 加数
- **返回**: `BigDecimal`

### subtract

```java
public BigDecimal subtract(BigDecimal subtrahend)
```

减法。

- **参数**: `subtrahend` — 减数
- **返回**: `BigDecimal`

### multiply

```java
public BigDecimal multiply(BigDecimal multiplicand)
```

乘法。

- **参数**: `multiplicand` — 乘数
- **返回**: `BigDecimal`

### divide

```java
public BigDecimal divide(BigDecimal divisor, RoundingMode roundingMode)
```

除法，指定舍入模式。

- **参数**: `divisor` — 除数；`roundingMode` — 舍入模式
- **返回**: `BigDecimal`

### divide(BigDecimal, int, RoundingMode)

```java
public BigDecimal divide(BigDecimal divisor, int scale, RoundingMode roundingMode)
```

除法，指定结果小数位数和舍入模式。

- **参数**: `divisor` — 除数；`scale` — 小数位数；`roundingMode` — 舍入模式
- **返回**: `BigDecimal`

### setScale

```java
public BigDecimal setScale(int newScale, RoundingMode roundingMode)
```

设置小数位数并指定舍入模式。

- **参数**: `newScale` — 新小数位数；`roundingMode` — 舍入模式
- **返回**: `BigDecimal`

### compareTo

```java
public int compareTo(BigDecimal val)
```

比较两个 BigDecimal 的大小（忽略精度差异）。

- **参数**: `val` — 比较值
- **返回**: `int` — 负数表示小于，0 表示等于，正数表示大于

### equals

```java
public boolean equals(Object obj)
```

判断两个 BigDecimal 是否相等（要求值和精度都相同）。

- **参数**: `obj` — 比较对象
- **返回**: `boolean`

### doubleValue

```java
public double doubleValue()
```

转换为 double。

- **返回**: `double`

### toString

```java
public String toString()
```

返回字符串表示。

- **返回**: `String`

### toPlainString

```java
public String toPlainString()
```

返回不带指数的字符串表示。

- **返回**: `String`

### abs

```java
public BigDecimal abs()
```

绝对值。

- **返回**: `BigDecimal`

### negate

```java
public BigDecimal negate()
```

取负值。

- **返回**: `BigDecimal`

### signum

```java
public int signum()
```

返回符号：负数返回 -1，0 返回 0，正数返回 1。

- **返回**: `int`

## 测试

### BigDecimal

- 描述: 从字符串、int 构造 BigDecimal
- 断言: 值正确

```java
// 方法体开始
System.out.println("=== BigDecimal ===");
BigDecimal fromStr = new BigDecimal("123.45");
BigDecimal fromInt = new BigDecimal(42);
assertEquals(0, fromStr.compareTo(new BigDecimal("123.45")));
assertEquals(0, fromInt.compareTo(new BigDecimal("42")));
System.out.println("字符串: " + fromStr + ", int: " + fromInt + ", double: " + new BigDecimal(10.5));
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### equals

- 描述: 判断两个 BigDecimal 是否相等
- 断言: 相同值和精度返回 true

```java
// 方法体开始
System.out.println("=== equals ===");
BigDecimal a = new BigDecimal("10.0");
BigDecimal b = new BigDecimal("10.0");
BigDecimal c = new BigDecimal("20");
assertTrue(a.equals(b));
assertFalse(a.equals(c));
assertFalse(a.equals(new BigDecimal("10.00"))); // 精度不同
System.out.println("10.0 equals 10.0: " + a.equals(b));
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### doubleValue

- 描述: 转换为 double
- 断言: 转换值正确

```java
// 方法体开始
System.out.println("=== doubleValue ===");
BigDecimal bd = new BigDecimal("123.45");
double val = bd.doubleValue();
System.out.println("doubleValue: " + val);
assertEquals(123.45, val, 0.001);
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### toString

- 描述: 字符串表示
- 断言: toString 返回正确字符串

```java
// 方法体开始
System.out.println("=== toString ===");
BigDecimal bd = new BigDecimal("123.45");
assertEquals("123.45", bd.toString());
System.out.println("toString: " + bd.toString());
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### negate

- 描述: 取负值
- 断言: 正数取负后为负数

```java
// 方法体开始
System.out.println("=== negate ===");
BigDecimal bd = new BigDecimal("10");
BigDecimal neg = bd.negate();
System.out.println("10 取负: " + neg);
assertEquals(0, neg.compareTo(new BigDecimal("-10")));
assertEquals(0, bd.compareTo(new BigDecimal("10"))); // 原对象不变
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### valueOf

- 描述: 使用 valueOf 构造
- 断言: 值正确

```java
// 方法体开始
System.out.println("=== valueOf ===");
BigDecimal bd1 = BigDecimal.valueOf(123.45);
BigDecimal bd2 = BigDecimal.valueOf(123L, 2);
System.out.println("valueOf(double): " + bd1 + ", valueOf(long, int): " + bd2);
assertEquals("123.45", bd1.toString());
assertEquals("1.23", bd2.toString());
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### add

- 描述: 加法
- 断言: 10 + 20 = 30

```java
// 方法体开始
System.out.println("=== add ===");
BigDecimal a = new BigDecimal("10");
BigDecimal b = new BigDecimal("20");
BigDecimal result = a.add(b);
System.out.println("10 + 20 = " + result);
assertEquals(0, result.compareTo(new BigDecimal("30")));
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### subtract

- 描述: 减法
- 断言: 50 - 15 = 35

```java
// 方法体开始
System.out.println("=== subtract ===");
BigDecimal a = new BigDecimal("50");
BigDecimal b = new BigDecimal("15");
BigDecimal result = a.subtract(b);
System.out.println("50 - 15 = " + result);
assertEquals(0, result.compareTo(new BigDecimal("35")));
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### multiply

- 描述: 乘法
- 断言: 1.5 * 2 = 3.0

```java
// 方法体开始
System.out.println("=== multiply ===");
BigDecimal a = new BigDecimal("1.5");
BigDecimal b = new BigDecimal("2");
BigDecimal result = a.multiply(b);
System.out.println("1.5 * 2 = " + result);
assertEquals(0, result.compareTo(new BigDecimal("3.0")));
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### divide

- 描述: 除法（指定舍入模式）
- 断言: 10 / 3 = 3.33（保留2位）

```java
// 方法体开始
System.out.println("=== divide ===");
BigDecimal a = new BigDecimal("10");
BigDecimal b = new BigDecimal("3");
BigDecimal result = a.divide(b, 2, BigDecimal.ROUND_HALF_UP);
System.out.println("10 / 3 = " + result + "（保留2位小数）");
assertEquals(0, result.compareTo(new BigDecimal("3.33")));
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### setScale

- 描述: 设置小数位数和舍入
- 断言: 123.456 保留 2 位小数为 123.46（四舍五入）

```java
// 方法体开始
System.out.println("=== setScale ===");
BigDecimal bd = new BigDecimal("123.456");
BigDecimal rounded = bd.setScale(2, BigDecimal.ROUND_HALF_UP);
System.out.println("123.456 保留2位小数: " + rounded);
assertEquals("123.46", rounded.toString());
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### compareTo

- 描述: 比较两个 BigDecimal
- 断言: 比较结果正确

```java
// 方法体开始
System.out.println("=== compareTo ===");
BigDecimal a = new BigDecimal("10.0");
BigDecimal b = new BigDecimal("10.00");
BigDecimal c = new BigDecimal("20");
assertEquals(0, a.compareTo(b));   // 值相等，忽略精度
assertTrue(a.compareTo(c) < 0);
assertTrue(c.compareTo(a) > 0);
System.out.println("10.0 比较 10.00: " + a.compareTo(b) + "（相等）");
System.out.println("10.0 比较 20: " + a.compareTo(c) + "（小于）");
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### abs

- 描述: 绝对值
- 断言: -10 的绝对值为 10

```java
// 方法体开始
System.out.println("=== abs ===");
BigDecimal bd = new BigDecimal("-10");
BigDecimal abs = bd.abs();
System.out.println("-10 的绝对值: " + abs);
assertEquals(0, abs.compareTo(new BigDecimal("10")));
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### signum

- 描述: 符号判断
- 断言: 正数为 1，0 为 0，负数为 -1

```java
// 方法体开始
System.out.println("=== signum ===");
assertEquals(1, new BigDecimal("5").signum());
assertEquals(0, new BigDecimal("0").signum());
assertEquals(-1, new BigDecimal("-3").signum());
System.out.println("5 的符号: " + new BigDecimal("5").signum());
System.out.println("0 的符号: " + new BigDecimal("0").signum());
System.out.println("-3 的符号: " + new BigDecimal("-3").signum());
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### toPlainString

- 描述: 不带指数的字符串表示
- 断言: 科学计数法的值正常输出

```java
// 方法体开始
System.out.println("=== toPlainString ===");
BigDecimal bd = new BigDecimal("1.23E+3");
String plain = bd.toPlainString();
System.out.println("toPlainString: " + plain + ", toString: " + bd.toString());
assertEquals("1230", plain);
System.out.println("=== 测试通过 ===");
// 方法体结束
```
