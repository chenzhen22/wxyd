---
name: InputStreamReader
package: java.io
order: 190
---

## 介绍

`java.io.InputStreamReader` 是**字节流到字符流的桥接器**，将字节解码为字符。Java 8 中常与 BufferedReader 配合实现流式行读取。

## 方法

构造方法：
```java
public InputStreamReader(InputStream in)
public InputStreamReader(InputStream in, String charsetName)
```

### read

```java
public int read(char[] cbuf, int offset, int length) throws IOException
```

读取字符到数组中。

## 测试

- 描述: 字节流转字符流
- 断言: 读取结果正确

```java
// 方法体开始
System.out.println("=== InputStreamReader ===");
byte[] data = "你好 Java 8".getBytes("UTF-8");
InputStreamReader isr = new InputStreamReader(new ByteArrayInputStream(data), "UTF-8");
BufferedReader br = new BufferedReader(isr);
String line = br.readLine();
assertEquals("你好 Java 8", line);
br.close();
System.out.println("读取: " + line);
System.out.println("=== 测试通过 ===");
// 方法体结束
```
