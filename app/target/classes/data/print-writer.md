---
name: PrintWriter
package: java.io
order: 204
---

## 介绍

`java.io.PrintWriter` 是**格式化字符输出流**，与 PrintStream 类似但操作字符而非字节。

## 方法

构造方法：
```java
public PrintWriter(Writer out)
public PrintWriter(OutputStream out)
public PrintWriter(File file) throws FileNotFoundException
```

### print / println / printf / format

标准格式化输出方法。

## 测试

- 描述: 使用 PrintWriter 输出
- 断言: 输出结果正确

```java
// 方法体开始
System.out.println("=== PrintWriter ===");
StringWriter sw = new StringWriter();
PrintWriter pw = new PrintWriter(sw);
pw.printf("Hello %s, score=%d", "Alice", 95);
pw.close();
assertEquals("Hello Alice, score=95", sw.toString());
System.out.println("PrintWriter: " + sw.toString());
System.out.println("=== 测试通过 ===");
// 方法体结束
```
