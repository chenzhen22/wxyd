---
name: DataOutputStream
package: java.io
order: 195
---

## 介绍

`java.io.DataOutputStream` 是**数据输出流**，以平台无关的方式写入 Java 基本数据类型。

## 方法

构造方法：
```java
public DataOutputStream(OutputStream out)
```

### writeInt / writeLong / writeDouble / writeUTF

```java
public void writeInt(int v) throws IOException
public void writeDouble(double v) throws IOException
public void writeUTF(String str) throws IOException
```

写入基本类型数据。

## 测试

- 描述: 写入基本类型数据
- 断言: 写入结果正确

```java
// 方法体开始
System.out.println("=== DataOutputStream ===");
ByteArrayOutputStream baos = new ByteArrayOutputStream();
DataOutputStream dos = new DataOutputStream(baos);
dos.writeBoolean(true);
dos.writeInt(100);
dos.writeUTF("test");
dos.close();
byte[] data = baos.toByteArray();
assertTrue(data.length > 0);
System.out.println("写入数据大小: " + data.length + " 字节");
System.out.println("=== 测试通过 ===");
// 方法体结束
```
