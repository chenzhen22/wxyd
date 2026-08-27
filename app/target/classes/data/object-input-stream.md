---
name: ObjectInputStream
package: java.io
order: 196
---

## 介绍

`java.io.ObjectInputStream` 是**对象输入流**，用于反序列化之前由 ObjectOutputStream 写入的对象。

## 方法

构造方法：
```java
public ObjectInputStream(InputStream in) throws IOException
```

### readObject

```java
public final Object readObject() throws IOException, ClassNotFoundException
```

读取对象。

### readInt / readUTF

读取基本类型数据。

## 测试

- 描述: 反序列化字符串对象
- 断言: 读取结果正确

```java
// 方法体开始
System.out.println("=== ObjectInputStream ===");
ByteArrayOutputStream baos = new ByteArrayOutputStream();
ObjectOutputStream oos = new ObjectOutputStream(baos);
oos.writeObject("Hello Java 8");
oos.writeInt(42);
oos.close();
ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(baos.toByteArray()));
String str = (String) ois.readObject();
assertEquals("Hello Java 8", str);
assertEquals(42, ois.readInt());
ois.close();
System.out.println("反序列化: " + str);
System.out.println("=== 测试通过 ===");
// 方法体结束
```
