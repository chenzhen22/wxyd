---
name: Double
package: java.lang
order: 340
---

## 介绍

`java.lang.Double` 包装类在 Java 8 中新增了 `isFinite` 和 `sum/max/min` 方法。

## 方法

### isFinite

```java
public static boolean isFinite(double d)
```

### sum / max / min

## 测试

- 描述: Double 方法
- 断言: 正确

```java
// 方法体开始
System.out.println("=== Double ===");
assertTrue(Double.isFinite(3.14));
assertFalse(Double.isFinite(Double.POSITIVE_INFINITY));
assertEquals(8.0, Double.sum(5.0, 3.0), 0.0001);
assertEquals(5.0, Double.max(3.0, 5.0), 0.0001);
System.out.println("isFinite(3.14): " + Double.isFinite(3.14));
System.out.println("=== 测试通过 ===");
// 方法体结束
```
