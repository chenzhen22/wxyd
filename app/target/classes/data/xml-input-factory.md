---
name: XMLInputFactory
package: javax.xml.stream
order: 377
---

## 介绍

`javax.xml.stream.XMLInputFactory` 是 **StAX 输入工厂**，用于创建 XML 流读取器。

## 方法

### newInstance / newXMLStreamReader / createXMLEventReader

### setProperty / getProperty

## 测试

- 描述: 创建 StAX 读取器
- 断言: 创建成功

```java
// 方法体开始
System.out.println("=== XMLInputFactory ===");
XMLInputFactory factory = XMLInputFactory.newInstance();
assertNotNull(factory);
String xml = "<?xml version='1.0'?><root><child>data</child></root>";
XMLStreamReader reader = factory.createXMLStreamReader(new ByteArrayInputStream(xml.getBytes("UTF-8")));
assertNotNull(reader);
reader.close();
System.out.println("XMLInputFactory 创建成功");
System.out.println("=== 测试通过 ===");
// 方法体结束
```
