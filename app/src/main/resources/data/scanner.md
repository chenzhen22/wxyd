---
name: Scanner
package: java.util
order: 123
---

## 介绍

`java.util.Scanner` 是一个用于**解析基本类型和字符串**的文本扫描器。Java 8 为其新增了 `findAll`、`tokens` 等流式方法，使其与 Stream API 更好地集成。

Java 8 新增的方法：
- `findAll(Pattern)` — 返回所有匹配子串的 MatchResult 流
- `findAll(String)` — 同上，从字符串创建 Pattern
- `tokens()` — 返回按分隔符分割的 token 流
- `findInLine(Pattern)` / `findInLine(String)` — 在行内查找匹配

## 方法

### findAll

```java
public Stream<MatchResult> findAll(Pattern pattern)
```

返回扫描器中所有匹配给定模式的结果流。

- **参数**: `pattern` — 匹配模式
- **返回**: `Stream<MatchResult>` — 匹配结果流

### tokens

```java
public Stream<String> tokens()
```

返回按默认分隔符（空白）分割的 token 流。

- **返回**: `Stream<String>` — token 流

## 测试

### findAll 提取数字

- 描述: 使用 `findAll` 从字符串中提取所有数字
- 断言: 找到 3 个数字

```java
// 方法体开始
System.out.println("=== findAll ===");
Scanner sc = new Scanner("abc123def456ghi789");
Pattern p = Pattern.compile("\\d+");
List<String> nums = sc.findAll(p)
        .map(MatchResult::group)
        .collect(Collectors.toList());
assertEquals(3, nums.size());
assertEquals("123", nums.get(0));
assertEquals("789", nums.get(2));
System.out.println("找到的数字: " + nums);
sc.close();
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### tokens

- 描述: 使用 `tokens` 将文本分割为单词流
- 断言: 分割结果正确

```java
// 方法体开始
System.out.println("=== tokens ===");
Scanner sc = new Scanner("Java Python Go C++");
List<String> words = sc.tokens().collect(Collectors.toList());
assertEquals(4, words.size());
assertEquals("Java", words.get(0));
assertEquals("C++", words.get(3));
System.out.println("单词: " + words);
sc.close();
System.out.println("=== 测试通过 ===");
// 方法体结束
```
