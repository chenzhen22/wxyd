---
name: DocumentBuilderFactory
package: javax.xml.parsers
order: 280
---

## 介绍

`javax.xml.parsers.DocumentBuilderFactory` 是 **XML 解析工厂类**，用于创建 DOM 解析器。

## 方法

### newInstance

```java
public static DocumentBuilderFactory newInstance()
```

### newDocumentBuilder

```java
public DocumentBuilder newDocumentBuilder() throws ParserConfigurationException
```

### setNamespaceAware / setValidating

设置解析器属性。

## 测试

- 描述: 创建 XML DOM 解析器
- 断言: 解析器创建成功

```java
// 方法体开始
System.out.println("=== DocumentBuilderFactory ===");
DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
dbf.setNamespaceAware(true);
DocumentBuilder db = dbf.newDocumentBuilder();
assertNotNull(db);
String xml = "<root><item id=\"1\">Hello</item></root>";
Document doc = db.parse(new ByteArrayInputStream(xml.getBytes("UTF-8")));
assertEquals("root", doc.getDocumentElement().getNodeName());
System.out.println("XML 解析成功: " + doc.getDocumentElement().getNodeName());
System.out.println("=== 测试通过 ===");
// 方法体结束
```
