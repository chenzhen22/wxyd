---
name: Boolean
package: java.lang
order: 336
---

## 介绍

`java.lang.Boolean` 包装类在 Java 8 中新增了 `logicalAnd`、`logicalOr`、`logicalXor` 等静态方法。

## 方法

### logicalAnd

```java
public static boolean logicalAnd(boolean a, boolean b)
```

### logicalOr / logicalXor

作为方法引用使用。

### compare

```java
public static int compare(boolean x, boolean y)
```

## 测试

- 描述: Boolean 逻辑运算
- 断言: 结果正确

```java
// 方法体开始
System.out.println("=== Boolean ===");
assertTrue(Boolean.logicalAnd(true, true));
assertFalse(Boolean.logicalAnd(true, false));
assertTrue(Boolean.logicalOr(true, false));
assertFalse(Boolean.logicalOr(false, false));
assertTrue(Boolean.logicalXor(true, false));
assertFalse(Boolean.logicalXor(true, true));
System.out.println("logicalAnd(true, true): " + Boolean.logicalAnd(true, true));
System.out.println("=== 测试通过 ===");
// 方法体结束
```
