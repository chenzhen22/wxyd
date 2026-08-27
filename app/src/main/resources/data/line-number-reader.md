---
name: LineNumberReader
package: java.io
order: 208
---

## 介绍

`java.io.LineNumberReader` 是**可跟踪行号的字符输入流**，继承自 BufferedReader，能够记录和设置行号。

## 方法

构造方法：
```java
public LineNumberReader(Reader in)
public LineNumberReader(Reader in, int sz)
```

### getLineNumber / setLineNumber

```java
public int getLineNumber()
public void setLineNumber(int lineNumber)
```

获取/设置当前行号。

### readLine

按行读取并自动递增行号。

## 测试

- 描述: 使用 LineNumberReader 跟踪行号
- 断言: 行号递增

```java
// 方法体开始
System.out.println("=== LineNumberReader ===");
StringReader sr = new StringReader("line1\nline2\nline3");
LineNumberReader lnr = new LineNumberReader(sr);
assertEquals("line1", lnr.readLine());
assertEquals(1, lnr.getLineNumber());
assertEquals("line2", lnr.readLine());
assertEquals(2, lnr.getLineNumber());
assertEquals("line3", lnr.readLine());
assertEquals(3, lnr.getLineNumber());
lnr.close();
System.out.println("行号正确: 1,2,3");
System.out.println("=== 测试通过 ===");
// 方法体结束
```
