---
name: XPath
package: javax.xml.xpath
order: 281
---

## 介绍

`javax.xml.xpath.XPath` 是 **XPath 表达式求值接口**，用于从 XML 文档中提取数据。

## 方法

### evaluate

```java
public String evaluate(String expression, Object item) throws XPathExpressionException
```

### compile

编译 XPath 表达式。

## 测试

- 描述: 使用 XPath 提取 XML 数据
- 断言: 提取结果正确

```java
// 方法体开始
System.out.println("=== XPath ===");
String xml = "<books><book><title>Java 8</title><price>59.9</price></book></books>";
DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
Document doc = dbf.newDocumentBuilder().parse(new ByteArrayInputStream(xml.getBytes("UTF-8")));
XPath xpath = XPathFactory.newInstance().newXPath();
String title = xpath.evaluate("/books/book/title", doc);
String price = xpath.evaluate("/books/book/price", doc);
assertEquals("Java 8", title);
assertEquals("59.9", price);
System.out.println("书名: " + title + ", 价格: " + price);
System.out.println("=== 测试通过 ===");
// 方法体结束
```
