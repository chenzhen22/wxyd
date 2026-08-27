---
name: ByteArrayOutputStream
package: java.io
order: 193
---

## 介绍

`java.io.ByteArrayOutputStream` 是**内存字节输出流**，数据写入到内存中的字节数组，常用于测试和数据缓冲。

## 方法

构造方法：
```java
public ByteArrayOutputStream()
public ByteArrayOutputStream(int size)
```

### toByteArray / toString

```java
public byte[] toByteArray()
public String toString(String charsetName)
```

获取数据。

### write / writeTo

写入数据 / 写入到另一个输出流。

### size

```java
public int size()
```

获取当前缓冲区大小。

## 测试

- 描述: 写入并读取内存字节
- 断言: 结果正确

```java
// 方法体开始
System.out.println("=== ByteArrayOutputStream ===");
ByteArrayOutputStream baos = new ByteArrayOutputStream();
baos.write("Hello".getBytes("UTF-8"));
assertEquals(5, baos.size());
assertEquals("Hello", baos.toString("UTF-8"));
byte[] data = baos.toByteArray();
assertEquals(5, data.length);
baos.close();
System.out.println("数据: " + baos.toString("UTF-8"));
System.out.println("=== 测试通过 ===");
// 方法体结束
```
