---
name: BreakIterator
package: java.text
order: 230
---

## 介绍

`java.text.BreakIterator` 是**文本边界迭代器**，用于检测文本中字符、单词、句子和行的边界。

## 方法

### getWordInstance / getSentenceInstance / getLineInstance

```java
public static BreakIterator getWordInstance(Locale locale)
public static BreakIterator getSentenceInstance(Locale locale)
```

### first / next / current / following

```java
public int first()
public int next()
public int current()
```

边界迭代方法。

## 测试

- 描述: 按单词分割文本
- 断言: 分割结果正确

```java
// 方法体开始
System.out.println("=== BreakIterator ===");
BreakIterator bi = BreakIterator.getWordInstance(Locale.US);
bi.setText("Hello World! Java 8.");
List<String> words = new ArrayList<>();
int start = bi.first();
for (int end = bi.next(); end != BreakIterator.DONE; start = end, end = bi.next()) {
    String w = "Hello World! Java 8.".substring(start, end).trim();
    if (!w.isEmpty()) words.add(w);
}
assertEquals(4, words.size());  // Hello, World!, Java, 8.
System.out.println("单词: " + words);
System.out.println("=== 测试通过 ===");
// 方法体结束
```
