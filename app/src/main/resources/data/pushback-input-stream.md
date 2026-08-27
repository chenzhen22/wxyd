---
name: PushbackInputStream
package: java.io
order: 212
---

## 介绍

`java.io.PushbackInputStream` 是**推回输入流**，允许将读取的字节"推回"到流中重新读取，常用于解析场景。

## 方法

构造方法：
```java
public PushbackInputStream(InputStream in)
public PushbackInputStream(InputStream in, int size)
```

### unread

```java
public void unread(int b) throws IOException
public void unread(byte[] b) throws IOException
```

推回一个字节或字节数组。

### read

读取字节。

## 测试

- 描述: 读取并推回字节
- 断言: 推回的字节可以重新读取

```java
// 方法体开始
System.out.println("=== PushbackInputStream ===");
byte[] data = {65, 66, 67};  // 'A', 'B', 'C'
PushbackInputStream pbis = new PushbackInputStream(new ByteArrayInputStream(data));
int first = pbis.read();
assertEquals('A', first);
pbis.unread(first);  // 推回 'A'
assertEquals('A', pbis.read());  // 重新读取 'A'
assertEquals('B', pbis.read());  // 继续读取 'B'
assertEquals('C', pbis.read());
pbis.close();
System.out.println("推回读取成功");
System.out.println("=== 测试通过 ===");
// 方法体结束
```
