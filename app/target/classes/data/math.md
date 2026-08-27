---
name: Math
package: java.lang
order: 31
---

## 介绍

`java.lang.Math` 是 Java 中最基础的工具类之一，提供了执行基本数学运算的静态方法。Java 8 为 `Math` 类新增了一系列**精确算术运算**方法，在溢出时抛出 `ArithmeticException` 而非静默回绕。

新增的精确运算方法包括：

- **精确加减乘**：`addExact`、`subtractExact`、`multiplyExact`
- **精确自增/自减/取反**：`incrementExact`、`decrementExact`、`negateExact`
- **安全转换**：`toIntExact` — 将 long 安全转为 int
- **改进的除法与取模**：`floorDiv`、`floorMod` — 向负无穷方向取整
- **小于给定值的最大浮点数**：`nextDown` — 返回比给定值略小的浮点数

这些方法在金融计算、数值控制等场景中非常有用，能避免整数溢出导致的隐蔽 bug。

## 方法

### addExact

```java
public static int addExact(int x, int y)
```

返回两个整数的和，如果溢出则抛出 `ArithmeticException`。

- **参数**: `x` — 第一个值；`y` — 第二个值
- **返回**: `int` — 和
- **异常**: `ArithmeticException` — 如果结果溢出 int 范围

### subtractExact

```java
public static int subtractExact(int x, int y)
```

返回两个整数的差，如果溢出则抛出 `ArithmeticException`。

- **参数**: `x` — 被减数；`y` — 减数
- **返回**: `int` — 差
- **异常**: `ArithmeticException` — 如果结果溢出 int 范围

### multiplyExact

```java
public static int multiplyExact(int x, int y)
```

返回两个整数的积，如果溢出则抛出 `ArithmeticException`。

- **参数**: `x` — 第一个值；`y` — 第二个值
- **返回**: `int` — 积
- **异常**: `ArithmeticException` — 如果结果溢出 int 范围

### incrementExact

```java
public static int incrementExact(int a)
```

返回自增 1 后的值，如果溢出则抛出 `ArithmeticException`。

- **参数**: `a` — 要自增的值
- **返回**: `int` — 自增后的值
- **异常**: `ArithmeticException` — 如果结果溢出 int 范围

### decrementExact

```java
public static int decrementExact(int a)
```

返回自减 1 后的值，如果溢出则抛出 `ArithmeticException`。

- **参数**: `a` — 要自减的值
- **返回**: `int` — 自减后的值
- **异常**: `ArithmeticException` — 如果结果溢出 int 范围

### negateExact

```java
public static int negateExact(int a)
```

返回相反数，如果溢出则抛出 `ArithmeticException`（`Integer.MIN_VALUE` 的相反数超出 int 范围）。

- **参数**: `a` — 要求相反数的值
- **返回**: `int` — 相反数
- **异常**: `ArithmeticException` — 如果结果溢出 int 范围

### toIntExact

```java
public static int toIntExact(long value)
```

将 long 值安全转换为 int，如果溢出则抛出 `ArithmeticException`。

- **参数**: `value` — long 值
- **返回**: `int` — 转换后的 int 值
- **异常**: `ArithmeticException` — 如果值超出 int 范围

### floorDiv

```java
public static int floorDiv(int x, int y)
```

返回向负无穷方向取整的除法结果。与整数除法（向零取整）不同，`floorDiv` 对负数结果也能正确向负无穷取整。

- **参数**: `x` — 被除数；`y` — 除数
- **返回**: `int` — 商
- **说明**: `floorDiv(-3, 2) = -2`，而普通除法 `-3 / 2 = -1`

### floorMod

```java
public static int floorMod(int x, int y)
```

返回 `floorDiv(x, y)` 的余数，满足 `floorDiv(x, y) * y + floorMod(x, y) == x`。结果始终与除数 y 同号或为 0。

- **参数**: `x` — 被除数；`y` — 除数
- **返回**: `int` — 余数

### nextDown

```java
public static double nextDown(double d)
```

返回比给定 double 值略小的浮点数（向负无穷方向的下一个可表示值）。

- **参数**: `d` — 起始值
- **返回**: `double` — 比 d 小的相邻浮点数

## 测试

### addExact

- 描述: 测试 addExact 精确相加
- 断言: 正常相加返回正确结果，溢出时抛出异常

```java
// 方法体开始
System.out.println("=== addExact ===");
int sum = Math.addExact(100, 200);
System.out.println("100 + 200 = " + sum);
assertEquals(300, sum);
sum = Math.addExact(-50, 30);
assertEquals(-20, sum);
System.out.println("-50 + 30 = " + sum);
boolean thrown = false;
try {
    Math.addExact(Integer.MAX_VALUE, 1);
} catch (ArithmeticException e) {
    thrown = true;
    System.out.println("溢出抛出 ArithmeticException: " + e.getMessage());
}
assertTrue(thrown);
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### subtractExact

- 描述: 测试 subtractExact 精确相减
- 断言: 正常相减返回正确结果，溢出时抛出异常

```java
// 方法体开始
System.out.println("=== subtractExact ===");
int diff = Math.subtractExact(100, 50);
System.out.println("100 - 50 = " + diff);
assertEquals(50, diff);
diff = Math.subtractExact(10, 30);
assertEquals(-20, diff);
System.out.println("10 - 30 = " + diff);
boolean thrown = false;
try {
    Math.subtractExact(Integer.MIN_VALUE, 1);
} catch (ArithmeticException e) {
    thrown = true;
    System.out.println("溢出抛出 ArithmeticException: " + e.getMessage());
}
assertTrue(thrown);
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### multiplyExact

- 描述: 测试 multiplyExact 精确相乘
- 断言: 正常相乘返回正确结果，溢出时抛出异常

```java
// 方法体开始
System.out.println("=== multiplyExact ===");
int product = Math.multiplyExact(6, 7);
System.out.println("6 * 7 = " + product);
assertEquals(42, product);
product = Math.multiplyExact(-3, 4);
assertEquals(-12, product);
System.out.println("-3 * 4 = " + product);
boolean thrown = false;
try {
    Math.multiplyExact(100000, 100000);
} catch (ArithmeticException e) {
    thrown = true;
    System.out.println("溢出抛出 ArithmeticException: " + e.getMessage());
}
assertTrue(thrown);
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### incrementExact

- 描述: 测试 incrementExact 方法
- 断言: 正常自增，溢出时抛出异常

```java
// 方法体开始
System.out.println("=== incrementExact ===");
int val = Math.incrementExact(10);
assertEquals(11, val);
System.out.println("incrementExact(10) = " + val);
boolean thrown = false;
try {
    Math.incrementExact(Integer.MAX_VALUE);
} catch (ArithmeticException e) {
    thrown = true;
    System.out.println("incrementExact 溢出: " + e.getMessage());
}
assertTrue(thrown);
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### decrementExact

- 描述: 测试 decrementExact 方法
- 断言: 正常自减，溢出时抛出异常

```java
// 方法体开始
System.out.println("=== decrementExact ===");
int val = Math.decrementExact(10);
assertEquals(9, val);
System.out.println("decrementExact(10) = " + val);
boolean thrown = false;
try {
    Math.decrementExact(Integer.MIN_VALUE);
} catch (ArithmeticException e) {
    thrown = true;
    System.out.println("decrementExact 溢出: " + e.getMessage());
}
assertTrue(thrown);
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### negateExact

- 描述: 测试 negateExact 取反
- 断言: 正常取反，Integer.MIN_VALUE 抛出异常

```java
// 方法体开始
System.out.println("=== negateExact ===");
int val = Math.negateExact(100);
assertEquals(-100, val);
System.out.println("negateExact(100) = " + val);
val = Math.negateExact(-50);
assertEquals(50, val);
System.out.println("negateExact(-50) = " + val);
boolean thrown = false;
try {
    Math.negateExact(Integer.MIN_VALUE);
} catch (ArithmeticException e) {
    thrown = true;
    System.out.println("negateExact 溢出: " + e.getMessage());
}
assertTrue(thrown);
System.out.println("=== 测试通过 ===");
// 方法体结束
```


### floorDiv

- 描述: 测试 floorDiv 向负无穷取整除法
- 断言: 正负数除法结果正确

```java
// 方法体开始
System.out.println("=== floorDiv ===");
int d1 = Math.floorDiv(7, 3);
int d2 = Math.floorDiv(-7, 3);
System.out.println("floorDiv(7, 3) = " + d1 + ", floorDiv(-7, 3) = " + d2);
assertEquals(2, d1);
assertEquals(-3, d2);
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### floorMod

- 描述: 测试 floorMod 向负无穷取整余数
- 断言: 正负数余数结果正确

```java
// 方法体开始
System.out.println("=== floorMod ===");
int m1 = Math.floorMod(7, 3);
int m2 = Math.floorMod(-7, 3);
System.out.println("floorMod(7, 3) = " + m1 + ", floorMod(-7, 3) = " + m2);
assertEquals(1, m1);
assertEquals(2, m2);
System.out.println("=== 测试通过 ===");
// 方法体结束
```
### floorDivMod

- 描述: 测试 floorDiv 和 floorMod 的向负无穷取整
- 断言: floorDiv 和 floorMod 结果正确

```java
// 方法体开始
System.out.println("=== floorDivMod ===");
int d1 = Math.floorDiv(7, 3);
int d2 = Math.floorDiv(-7, 3);
System.out.println("floorDiv(7, 3) = " + d1 + ", floorDiv(-7, 3) = " + d2);
assertEquals(2, d1);
assertEquals(-3, d2);
int m1 = Math.floorMod(7, 3);
int m2 = Math.floorMod(-7, 3);
System.out.println("floorMod(7, 3) = " + m1 + ", floorMod(-7, 3) = " + m2);
assertEquals(1, m1);
assertEquals(2, m2);
// 验证等式: floorDiv(x, y) * y + floorMod(x, y) == x
assertEquals(7, Math.floorDiv(7, 3) * 3 + Math.floorMod(7, 3));
assertEquals(-7, Math.floorDiv(-7, 3) * 3 + Math.floorMod(-7, 3));
System.out.println("恒等式验证通过");
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### nextDown

- 描述: 测试 nextDown 方法
- 断言: 返回比给定值稍小的浮点数

```java
// 方法体开始
System.out.println("=== nextDown ===");
double d = Math.nextDown(1.0);
System.out.println("nextDown(1.0) = " + d);
assertTrue(d < 1.0);
d = Math.nextDown(0.0);
System.out.println("nextDown(0.0) = " + d);
assertTrue(d < 0.0);
d = Math.nextDown(Double.POSITIVE_INFINITY);
System.out.println("nextDown(POSITIVE_INFINITY) = " + d);
assertTrue(Double.isFinite(d));
assertTrue(d > 0);
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### toIntExact

- 描述: 测试 toIntExact 安全转换
- 断言: 正常转换返回正确值，超出 int 范围抛出异常

```java
// 方法体开始
System.out.println("=== toIntExact ===");
int val = Math.toIntExact(100L);
System.out.println("toIntExact(100L) = " + val);
assertEquals(100, val);
val = Math.toIntExact(-50L);
assertEquals(-50, val);
System.out.println("toIntExact(-50L) = " + val);
boolean thrown = false;
try {
    Math.toIntExact((long) Integer.MAX_VALUE + 1L);
} catch (ArithmeticException e) {
    thrown = true;
    System.out.println("超出 int 范围抛出 ArithmeticException: " + e.getMessage());
}
assertTrue(thrown);
System.out.println("=== 测试通过 ===");
// 方法体结束
```
