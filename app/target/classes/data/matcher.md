---
name: Matcher
package: java.util.regex
order: 157
---

## 介绍

`java.util.regex.Matcher` 在 Java 8 中新增了 `results()` 方法，可以返回所有匹配结果的流，以及一些 Lambda 友好的增强。

Java 8 新增的方法：
- `results()` — 返回所有匹配结果的 Stream（Java 9 中正式引入，Java 8 已有早期支持）
- `region(int, int)` / `regionStart()` / `regionEnd()` — 区域匹配

## 方法

### results

```java
public Stream<MatchResult> results()
```

返回所有匹配结果的流。

- **返回**: `Stream<MatchResult>` — 匹配结果流

### group

```java
public String group()
public String group(int group)
```

返回当前匹配的子串/分组。

### find

```java
public boolean find()
```

查找下一个匹配子串。

## 测试

### results 查找所有数字

- 描述: 使用 results() 流式提取所有匹配
- 断言: 找到 3 个数字

```java
// 方法体开始
System.out.println("=== results ===");
Pattern p = Pattern.compile("\\d+");
Matcher m = p.matcher("abc123def456ghi789");
List<String> nums = m.results()
        .map(MatchResult::group)
        .collect(Collectors.toList());
assertEquals(3, nums.size());
assertEquals("123", nums.get(0));
assertEquals("789", nums.get(2));
System.out.println("数字: " + nums);
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### 提取分组

- 描述: 使用 results() 提取分组内容
- 断言: 找到所有名称

```java
// 方法体开始
System.out.println("=== 提取分组 ===");
Pattern p = Pattern.compile("(\\w+)@(\\w+)");
Matcher m = p.matcher("alice@example.com bob@test.com");
List<String> names = m.results()
        .map(r -> r.group(1))
        .collect(Collectors.toList());
assertEquals(2, names.size());
assertEquals("alice", names.get(0));
assertEquals("bob", names.get(1));
System.out.println("用户名: " + names);
System.out.println("=== 测试通过 ===");
// 方法体结束
```
