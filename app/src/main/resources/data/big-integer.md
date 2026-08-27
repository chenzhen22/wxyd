---
name: BigInteger
package: java.math
order: 152
---

## 介绍

`java.math.BigInteger` 是**任意精度的整数运算**类。Java 8 为其新增了多个实用方法，包括最大公约数、模运算、是否为素数等判断。

Java 8 新增的核心方法：
- `nextProbablePrime()` — 大于当前数的下一个可能素数
- `isProbablePrime(int)` — 是否为素数（按 certainty 置信度）
- `modPow(BigInteger, BigInteger)` — 模幂运算
- `modInverse(BigInteger)` — 模逆运算
- `gcd(BigInteger)` — 最大公约数
- `longValueExact()` — 精确转 long，溢出抛异常
- `intValueExact()` — 精确转 int，溢出抛异常

## 方法

### isProbablePrime

```java
public boolean isProbablePrime(int certainty)
```

判断是否为素数。参数 certainty 表示调用者容忍的准确性 — 值越大，误报可能性越小。

### nextProbablePrime

```java
public BigInteger nextProbablePrime()
```

返回大于当前数的第一个素数。

### gcd

```java
public BigInteger gcd(BigInteger val)
```

返回当前数与 val 的绝对值的最大公约数。

### longValueExact / intValueExact

```java
public long longValueExact()
public int intValueExact()
```

精确转换为 long/int，溢出时抛 ArithmeticException。

## 测试

### isProbablePrime

- 描述: 判断素数
- 断言: 17 是素数，15 不是

```java
// 方法体开始
System.out.println("=== 素数判断 ===");
BigInteger prime = BigInteger.valueOf(17);
BigInteger nonPrime = BigInteger.valueOf(15);
assertTrue(prime.isProbablePrime(10));
assertFalse(nonPrime.isProbablePrime(10));
System.out.println("17 是素数: " + prime.isProbablePrime(10));
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### gcd

- 描述: 计算最大公约数
- 断言: gcd(12, 8) = 4

```java
// 方法体开始
System.out.println("=== gcd ===");
BigInteger a = BigInteger.valueOf(12);
BigInteger b = BigInteger.valueOf(8);
assertEquals(BigInteger.valueOf(4), a.gcd(b));
System.out.println("gcd(12, 8) = " + a.gcd(b));
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### intValueExact

- 描述: 安全转换为 int
- 断言: 正常转换和溢出检测

```java
// 方法体开始
System.out.println("=== intValueExact ===");
BigInteger small = BigInteger.valueOf(100);
assertEquals(100, small.intValueExact());
BigInteger large = BigInteger.valueOf(Long.MAX_VALUE);
boolean thrown = false;
try {
    large.intValueExact();
} catch (ArithmeticException e) {
    thrown = true;
}
assertTrue(thrown);
System.out.println("溢出检测正确");
System.out.println("=== 测试通过 ===");
// 方法体结束
```
