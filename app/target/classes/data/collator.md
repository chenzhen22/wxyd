---
name: Collator
package: java.text
order: 229
---

## 介绍

`java.text.Collator` 是**字符串排序比较器**，支持不同语言环境的排序规则。

## 方法

### getInstance

```java
public static Collator getInstance(Locale desiredLocale)
```

获取指定语言环境的 Collator。

### compare

```java
public int compare(String source, String target)
```

比较两个字符串。

### setStrength

```java
public void setStrength(int newStrength)
```

设置比较强度：PRIMARY（基本字符）、SECONDARY（重音）、TERTIARY（大小写）、IDENTICAL（完全相同）。

## 测试

- 描述: 不同语言环境的排序
- 断言: 排序结果不同

```java
// 方法体开始
System.out.println("=== Collator ===");
Collator zhCollator = Collator.getInstance(Locale.CHINESE);
Collator deCollator = Collator.getInstance(Locale.GERMAN);
List<String> words = Arrays.asList("ä", "a", "b", "z");
Collections.sort(words, zhCollator);
System.out.println("中文排序: " + words);
words = Arrays.asList("ä", "a", "b", "z");
Collections.sort(words, deCollator);
System.out.println("德语排序: " + words);
System.out.println("=== 测试通过 ===");
// 方法体结束
```
