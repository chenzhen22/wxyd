---
name: Serializable
package: java.io
order: 326
---

## 介绍

`java.io.Serializable` 是**可序列化标记接口**，表示允许对象序列化。

## 方法

无方法（标记接口）。

## 测试

- 描述: 检查 Serializable
- 断言: 序列化成功

```java
// 方法体开始
System.out.println("=== Serializable ===");
assertTrue("Hello" instanceof Serializable);
ByteArrayOutputStream baos = new ByteArrayOutputStream();
ObjectOutputStream oos = new ObjectOutputStream(baos);
oos.writeObject("Serializable Test");
oos.close();
ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(baos.toByteArray()));
String result = (String) ois.readObject();
assertEquals("Serializable Test", result);
ois.close();
System.out.println("Serializable 序列化成功");
System.out.println("=== 测试通过 ===");
// 方法体结束
```
