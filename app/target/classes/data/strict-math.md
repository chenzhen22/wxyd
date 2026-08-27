---
name: StrictMath
package: java.lang
order: 114
---

## 介绍

`java.lang.StrictMath` 是 Java 中的**严格数学计算**工具类，与 `Math` 类似但保证了**所有平台计算结果一致**。Java 8 在 StrictMath 中也新增了与 Math 对应的精确运算方法。

StrictMath 与 Math 的区别：
- **Math** — 部分方法可能委托给平台的 libm，结果可能因平台而异
- **StrictMath** — 在所有平台上保证相同的计算结果（使用 fdlibm 算法）

Java 8 新增的 StrictMath 方法：
- **溢出安全运算**：`addExact`、`subtractExact`、`multiplyExact`、`incrementExact`、`decrementExact`、`negateExact`
- **安全转换**：`toIntExact`
- **其他**：`floorDiv`、`floorMod`、`nextDown`

## 方法

### addExact / subtractExact / multiplyExact

```java
public static int addExact(int x, int y)
public static int subtractExact(int x, int y)
public static int multiplyExact(int x, int y)
```

精确加减乘法，溢出时抛出 `ArithmeticException`。

### toIntExact

```java
public static int toIntExact(long value)
```

安全将 long 转换为 int。

## 测试

### addExact

- 描述: 精确加法
- 断言: 正常运算和溢出检测

```java
// 方法体开始
System.out.println("=== addExact ===");
assertEquals(300, StrictMath.addExact(100, 200));
assertEquals(-20, StrictMath.addExact(-50, 30));
boolean thrown = false;
try {
    StrictMath.addExact(Integer.MAX_VALUE, 1);
} catch (ArithmeticException e) {
    thrown = true;
}
assertTrue(thrown);
System.out.println("addExact(100, 200)=" + StrictMath.addExact(100, 200));
System.out.println("溢出检测正确");
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### multiplyExact

- 描述: 精确乘法
- 断言: 正常运算和溢出检测

```java
// 方法体开始
System.out.println("=== multiplyExact ===");
assertEquals(42, StrictMath.multiplyExact(6, 7));
boolean thrown = false;
try {
    StrictMath.multiplyExact(100000, 100000);
} catch (ArithmeticException e) {
    thrown = true;
}
assertTrue(thrown);
System.out.println("multiplyExact(6, 7)=" + StrictMath.multiplyExact(6, 7));
System.out.println("溢出检测正确");
System.out.println("=== 测试通过 ===");
// 方法体结束
```
