---
name: Byte
package: java.lang
order: 337
---

## 介绍

`java.lang.Byte` 包装类在 Java 8 中新增了 `toUnsignedInt` 和 `toUnsignedLong` 方法。

## 方法

### toUnsignedInt

```java
public static int toUnsignedInt(byte x)
```

将 byte 视为无符号值转换为 int（0-255 范围）。

### toUnsignedLong / compare / compareUnsigned

## 测试

- 描述: byte 无符号转换
- 断言: 转换正确

```java
// 方法体开始
System.out.println("=== Byte ===");
byte b = -1;  // 0xFF
assertEquals(255, Byte.toUnsignedInt(b));
assertEquals(255L, Byte.toUnsignedLong(b));
assertEquals(0, Byte.compare((byte) 5, (byte) 5));
assertTrue(Byte.compare((byte) 3, (byte) 5) < 0);
System.out.println("toUnsignedInt(-1): " + Byte.toUnsignedInt((byte)-1));
System.out.println("=== 测试通过 ===");
// 方法体结束
```
