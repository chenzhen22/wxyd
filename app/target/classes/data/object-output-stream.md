---
name: ObjectOutputStream
package: java.io
order: 197
---

## 介绍

`java.io.ObjectOutputStream` 是**对象输出流**，用于序列化 Java 对象。

## 方法

构造方法：
```java
public ObjectOutputStream(OutputStream out) throws IOException
```

### writeObject

```java
public final void writeObject(Object obj) throws IOException
```

写入对象。

### writeInt / writeUTF

写入基本类型数据。

## 测试

- 描述: 序列化字符串对象
- 断言: 写入成功

```java
// 方法体开始
System.out.println("=== ObjectOutputStream ===");
ByteArrayOutputStream baos = new ByteArrayOutputStream();
ObjectOutputStream oos = new ObjectOutputStream(baos);
oos.writeObject("Hello");
oos.writeInt(99);
oos.close();
assertTrue(baos.size() > 0);
System.out.println("序列化数据大小: " + baos.size() + " 字节");
System.out.println("=== 测试通过 ===");
// 方法体结束
```
