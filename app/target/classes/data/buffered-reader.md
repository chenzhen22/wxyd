---
name: BufferedReader
package: java.io
order: 156
---

## 介绍

`java.io.BufferedReader` 是**高效字符输入流**。Java 8 为其新增了 `lines()` 方法，可以按行返回 `Stream<String>`，极大简化了文本文件的行式处理。

## 方法

### lines

```java
public Stream<String> lines()
```

返回一个 Stream，延迟读取 BufferedReader 中的每一行。Stream 是惰性填充的，读取的行不会全部载入内存。

- **返回**: `Stream<String>` — 行的流
- **注意**: 读取完成后应关闭 Stream 以释放底层资源

### readLine

```java
public String readLine() throws IOException
```

读取一行文本。

## 测试

### lines

- 描述: 使用 lines() 逐行读取文本
- 断言: 读取所有行

```java
// 方法体开始
System.out.println("=== lines ===");
StringReader sr = new StringReader("line1\nline2\nline3");
try (BufferedReader br = new BufferedReader(sr)) {
    List<String> lines = br.lines().collect(Collectors.toList());
    assertEquals(3, lines.size());
    assertEquals("line1", lines.get(0));
    assertEquals("line3", lines.get(2));
    System.out.println("行数: " + lines.size() + ", 内容: " + lines);
}
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### 过滤流

- 描述: 使用 lines() 配合 Stream 过滤
- 断言: 过滤结果正确

```java
// 方法体开始
System.out.println("=== 过滤流 ===");
StringReader sr = new StringReader("apple\nbanana\navocado\ncherry");
try (BufferedReader br = new BufferedReader(sr)) {
    List<String> aFruits = br.lines()
            .filter(s -> s.startsWith("a"))
            .collect(Collectors.toList());
    assertEquals(2, aFruits.size());
    assertEquals("[apple, avocado]", aFruits.toString());
    System.out.println("以 a 开头: " + aFruits);
}
System.out.println("=== 测试通过 ===");
// 方法体结束
```
