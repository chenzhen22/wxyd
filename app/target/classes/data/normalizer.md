---
name: Normalizer
package: java.text
order: 231
---

## 介绍

`java.text.Normalizer` 是 **Unicode 文本规范化**类，用于将 Unicode 文本转换为标准范式。

## 四种范式

- **NFC** — 标准组合范式（默认）
- **NFD** — 标准分解范式
- **NFKC** — 兼容组合范式
- **NFKD** — 兼容分解范式

## 方法

### normalize

```java
public static String normalize(CharSequence src, Normalizer.Form form)
```

将文本规范化为指定范式。

### isNormalized

```java
public static boolean isNormalized(CharSequence seq, Normalizer.Form form)
```

检查文本是否已是规范形式。

## 测试

- 描述: 规范化 Unicode 文本
- 断言: 规范化后字符串长度变化

```java
// 方法体开始
System.out.println("=== Normalizer ===");
// é 可以用两种方式表示：é (NFC) 或 e + ́ (NFD)
String composed = "é";  // é (NFC)
String decomposed = "é";  // é (NFD)
assertEquals(1, composed.length());
assertEquals(2, decomposed.length());
String normalized = Normalizer.normalize(decomposed, Normalizer.Form.NFC);
assertEquals(1, normalized.length());
assertEquals(composed, normalized);
System.out.println("NFC: " + composed + ", NFD: " + decomposed + ", 归一化: " + normalized);
System.out.println("=== 测试通过 ===");
// 方法体结束
```
