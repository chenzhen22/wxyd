---
name: SAXParser
package: javax.xml.parsers
order: 375
---

## 介绍

`javax.xml.parsers.SAXParser` 是 **SAX 解析器**，基于事件的 XML 解析方式。

## 方法

### parse

```java
public void parse(InputStream is, DefaultHandler dh) throws SAXException, IOException
```

### getParser / isNamespaceAware / getProperty / setProperty

## 测试

- 描述: SAX 解析 XML
- 断言: 解析成功

```java
// 方法体开始
System.out.println("=== SAXParser ===");
SAXParserFactory factory = SAXParserFactory.newInstance();
SAXParser parser = factory.newSAXParser();
assertNotNull(parser);
String xml = "<root><item id=\"1\">Hello</item></root>";
List<String> elements = new ArrayList<>();
parser.parse(new ByteArrayInputStream(xml.getBytes("UTF-8")), 
    new DefaultHandler() {
        public void startElement(String uri, String ln, String qn, Attributes a) {
            elements.add(qn);
        }
    });
assertTrue(elements.contains("root"));
assertTrue(elements.contains("item"));
System.out.println("SAX 解析元素: " + elements);
System.out.println("=== 测试通过 ===");
// 方法体结束
```
