---
name: RoundingMode
package: java.math
order: 218
---

## 介绍

`java.math.RoundingMode` 是 Java 中用于**舍入模式**的枚举，定义在 `java.math` 包中。Java 8 中常与 `BigDecimal` 配合使用。

舍入模式：
- `UP` — 远离零方向舍入
- `DOWN` — 向零方向舍入
- `CEILING` — 向正无穷方向舍入
- `FLOOR` — 向负无穷方向舍入
- `HALF_UP` — 四舍五入（最常用）
- `HALF_DOWN` — 五舍六入
- `HALF_EVEN` — 银行家舍入
- `UNNECESSARY` — 断言不需要舍入

## 方法

### values / valueOf

```java
public static RoundingMode[] values()
public static RoundingMode valueOf(int mode)
```

## 测试

- 描述: 使用不同舍入模式处理同一个值
- 断言: 不同模式结果不同

```java
// 方法体开始
System.out.println("=== RoundingMode ===");
BigDecimal d = new BigDecimal("2.55");
assertEquals(new BigDecimal("2.5"), d.setScale(1, RoundingMode.DOWN));
assertEquals(new BigDecimal("2.6"), d.setScale(1, RoundingMode.UP));
assertEquals(new BigDecimal("2.6"), d.setScale(1, RoundingMode.HALF_UP));
assertEquals(new BigDecimal("2.6"), d.setScale(1, RoundingMode.HALF_EVEN));
// 2.55 在 HALF_EVEN 下也是 2.6
System.out.println("DOWN: " + d.setScale(1, RoundingMode.DOWN));
System.out.println("UP: " + d.setScale(1, RoundingMode.UP));
System.out.println("HALF_UP: " + d.setScale(1, RoundingMode.HALF_UP));
System.out.println("=== 测试通过 ===");
// 方法体结束
```
