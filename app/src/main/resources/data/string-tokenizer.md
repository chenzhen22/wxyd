---
name: StringTokenizer
package: java.util
order: 248
---

## 介绍

`java.util.StringTokenizer` 是**字符串分割工具类**，用于按分隔符拆分字符串。它是遗留类，推荐使用 `String.split` 或 `Scanner` 代替。

## 方法

构造方法：
```java
public StringTokenizer(String str, String delim)
```

### hasMoreTokens / nextToken

```java
public boolean hasMoreTokens()
public String nextToken()
```

### countTokens

```java
public int countTokens()
```

剩余 token 数量。

## 测试

- 描述: 分割逗号分隔的字符串
- 断言: 正确分割

```java
// 方法体开始
System.out.println("=== StringTokenizer ===");
StringTokenizer st = new StringTokenizer("apple,banana,orange", ",");
assertEquals(3, st.countTokens());
assertEquals("apple", st.nextToken());
assertEquals("banana", st.nextToken());
assertEquals("orange", st.nextToken());
assertFalse(st.hasMoreTokens());
System.out.println("StringTokenizer 分割完成");
System.out.println("=== 测试通过 ===");
// 方法体结束
```
