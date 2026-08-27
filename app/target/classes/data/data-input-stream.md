---
name: DataInputStream
package: java.io
order: 194
---

## 介绍

`java.io.DataInputStream` 是**数据输入流**，以与平台无关的方式从底层流读取 Java 基本数据类型。

## 方法

构造方法：
```java
public DataInputStream(InputStream in)
```

### readInt / readLong / readDouble / readUTF

```java
public int readInt() throws IOException
public long readLong() throws IOException
public double readDouble() throws IOException
public String readUTF() throws IOException
```

读取基本类型数据。

## 测试

- 描述: 读取二进制基本类型数据
- 断言: 读取结果正确

```java
// 方法体开始
System.out.println("=== DataInputStream ===");
ByteArrayOutputStream baos = new ByteArrayOutputStream();
DataOutputStream dos = new DataOutputStream(baos);
dos.writeInt(42);
dos.writeDouble(3.14);
dos.writeUTF("Java");
dos.close();
DataInputStream dis = new DataInputStream(new ByteArrayInputStream(baos.toByteArray()));
assertEquals(42, dis.readInt());
assertEquals(3.14, dis.readDouble(), 0.0001);
assertEquals("Java", dis.readUTF());
dis.close();
System.out.println("DataInputStream 读取完成");
System.out.println("=== 测试通过 ===");
// 方法体结束
```
