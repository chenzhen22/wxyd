---
name: Short
package: java.lang
order: 338
---

## 介绍

`java.lang.Short` 包装类在 Java 8 中新增了 `toUnsignedInt` 和 `toUnsignedLong` 方法。

## 方法

### toUnsignedInt

```java
public static int toUnsignedInt(short x)
```

### toUnsignedLong / compare / compareUnsigned

## 测试

- 描述: short 无符号转换
- 断言: 转换正确

```java
// 方法体开始
System.out.println("=== Short ===");
short s = -1;
assertEquals(65535, Short.toUnsignedInt(s));
assertEquals(65535L, Short.toUnsignedLong(s));
assertEquals(0, Short.compare((short)5, (short)5));
System.out.println("toUnsignedInt(-1): " + Short.toUnsignedInt((short)-1));
System.out.println("=== 测试通过 ===");
// 方法体结束
```
