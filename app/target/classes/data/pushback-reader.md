---
name: PushbackReader
package: java.io
order: 292
---

## 介绍

`java.io.PushbackReader` 是**推回字符读取器**，允许将读取的字符推回流中。

## 方法

构造方法：
```java
public PushbackReader(Reader in)
public PushbackReader(Reader in, int size)
```

### unread

```java
public void unread(int c) throws IOException
```

### read

读取字符。

## 测试

- 描述: 推回字符重新读取
- 断言: 推回后可以重新读取

```java
// 方法体开始
System.out.println("=== PushbackReader ===");
StringReader sr = new StringReader("ABC");
PushbackReader pbr = new PushbackReader(sr);
int c = pbr.read();
assertEquals('A', c);
pbr.unread('X');
assertEquals('X', pbr.read());
assertEquals('B', pbr.read());
assertEquals('C', pbr.read());
pbr.close();
System.out.println("PushbackReader 推回测试通过");
System.out.println("=== 测试通过 ===");
// 方法体结束
```
