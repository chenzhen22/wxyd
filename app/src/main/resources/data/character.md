---
name: Character
package: java.lang
order: 335
---

## 介绍

`java.lang.Character` 包装类在 Java 8 中新增了一些 Unicode 支持增强方法，包括对增补字符（Supplementary Characters）的支持。

Java 8 新增/增强的方法：
- `isAlphabetic(int codePoint)` — 判断是否为字母
- `isIdeographic(int codePoint)` — 判断是否为表意文字
- `isLetterOrDigit(int codePoint)` — 判断是否为字母或数字
- `isBmpCodePoint(int codePoint)` — 判断是否为 BMP 字符

## 方法

### isAlphabetic

```java
public static boolean isAlphabetic(int codePoint)
```

### isIdeographic

```java
public static boolean isIdeographic(int codePoint)
```

### isLetterOrDigit / toChars / codePointAt

## 测试

- 描述: 字符判断
- 断言: 判断正确

```java
// 方法体开始
System.out.println("=== Character ===");
assertTrue(Character.isAlphabetic('A'));
assertTrue(Character.isAlphabetic('中'));
assertFalse(Character.isAlphabetic('1'));
assertTrue(Character.isLetterOrDigit('1'));
assertTrue(Character.isLetterOrDigit('A'));
assertFalse(Character.isLetterOrDigit('+'));
System.out.println("isAlphabetic('A'): " + Character.isAlphabetic('A'));
System.out.println("isAlphabetic('中'): " + Character.isAlphabetic('中'));
System.out.println("=== 测试通过 ===");
// 方法体结束
```
