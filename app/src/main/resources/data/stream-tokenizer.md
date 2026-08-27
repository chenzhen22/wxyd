---
name: StreamTokenizer
package: java.io
order: 293
---

## 介绍

`java.io.StreamTokenizer` 是**输入流分词器**类，将输入流解析为 token 序列。

## 方法

构造方法：
```java
public StreamTokenizer(Reader r)
```

### nextToken

```java
public int nextToken() throws IOException
```

### TT_WORD / TT_NUMBER / TT_EOL / TT_EOF

Token 类型常量。

### sval / nval

当前 token 的字符串值/数值。

### commentChar / quoteChar / ordinaryChar / whitespaceChars

配置方法。

## 测试

- 描述: 解析文本为 token
- 断言: 正确识别 token

```java
// 方法体开始
System.out.println("=== StreamTokenizer ===");
StringReader sr = new StringReader("hello 42 world");
StreamTokenizer st = new StreamTokenizer(sr);
st.nextToken();
assertEquals(StreamTokenizer.TT_WORD, st.ttype);
assertEquals("hello", st.sval);
st.nextToken();
assertEquals(StreamTokenizer.TT_NUMBER, st.ttype);
assertEquals(42.0, st.nval, 0.0001);
st.nextToken();
assertEquals(StreamTokenizer.TT_WORD, st.ttype);
assertEquals("world", st.sval);
System.out.println("Token 解析完成");
System.out.println("=== 测试通过 ===");
// 方法体结束
```
