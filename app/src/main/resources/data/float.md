---
name: Float
package: java.lang
order: 339
---

## 介绍

`java.lang.Float` 包装类在 Java 8 中新增了 `isFinite` 和 `sum/max/min` 方法。

## 方法

### isFinite

```java
public static boolean isFinite(float f)
```

### sum / max / min

适合方法引用的静态运算方法。

### isNaN / isInfinite

## 测试

- 描述: Float 方法
- 断言: 正确

```java
// 方法体开始
System.out.println("=== Float ===");
assertTrue(Float.isFinite(3.14f));
assertFalse(Float.isFinite(Float.POSITIVE_INFINITY));
assertFalse(Float.isFinite(Float.NaN));
assertEquals(8.0f, Float.sum(5.0f, 3.0f), 0.0001);
assertEquals(5.0f, Float.max(3.0f, 5.0f), 0.0001);
System.out.println("isFinite(3.14): " + Float.isFinite(3.14f));
System.out.println("sum(5,3): " + Float.sum(5.0f, 3.0f));
System.out.println("=== 测试通过 ===");
// 方法体结束
```
