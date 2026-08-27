---
name: BufferedInputStream
package: java.io
order: 188
---

## 介绍

`java.io.BufferedInputStream` 是**缓冲字节输入流**，为底层输入流添加缓冲功能，减少 I/O 操作次数。Java 8 中常与 Files.newInputStream 配合。

## 方法

构造方法：
```java
public BufferedInputStream(InputStream in)
public BufferedInputStream(InputStream in, int size)
```

### read

```java
public int read() throws IOException
public int read(byte[] b, int off, int len) throws IOException
```

读取一个字节或字节数组。

## 测试

- 描述: 使用 BufferedInputStream 读取字节
- 断言: 读取结果正确

```java
// 方法体开始
System.out.println("=== BufferedInputStream ===");
byte[] data = "Hello".getBytes("UTF-8");
BufferedInputStream bis = new BufferedInputStream(new ByteArrayInputStream(data));
byte[] result = new byte[5];
int bytesRead = bis.read(result);
assertEquals(5, bytesRead);
assertEquals("Hello", new String(result, "UTF-8"));
bis.close();
System.out.println("读取: " + new String(result));
System.out.println("=== 测试通过 ===");
// 方法体结束
```
