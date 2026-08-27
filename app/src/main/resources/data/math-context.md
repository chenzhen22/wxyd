---
name: MathContext
package: java.math
order: 226
---

## 介绍

`java.math.MathContext` 是**数学上下文**类，封装了精度设置和舍入模式，用于 BigDecimal 等类的精确计算。

## 常量

- `UNLIMITED` — 无限精度
- `DECIMAL32` — 7 位精度
- `DECIMAL64` — 16 位精度
- `DECIMAL128` — 34 位精度

## 方法

构造方法：
```java
public MathContext(int setPrecision)
public MathContext(int setPrecision, RoundingMode setRoundingMode)
```

### getPrecision / getRoundingMode

获取精度和舍入模式。

## 测试

- 描述: 使用 MathContext 控制精度
- 断言: 精度影响计算结果

```java
// 方法体开始
System.out.println("=== MathContext ===");
MathContext mc = new MathContext(5, RoundingMode.HALF_UP);
BigDecimal a = new BigDecimal("1.23456789");
BigDecimal b = new BigDecimal("0.00001111");
BigDecimal sum = a.add(b, mc);
assertEquals(new BigDecimal("1.2346"), sum);  // 保留 5 位有效数字
System.out.println("a+b (5位精度) = " + sum);
MathContext unlimited = MathContext.UNLIMITED;
assertEquals(new BigDecimal("1.23457900"), a.add(b, unlimited));
System.out.println("a+b (不限精度) = " + a.add(b));
System.out.println("=== 测试通过 ===");
// 方法体结束
```
