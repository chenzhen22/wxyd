---
name: DefaultHandler
package: org.xml.sax.helpers
order: 384
---

## 介绍

`org.xml.sax.helpers.DefaultHandler` 是 **SAX 事件处理器**基类，提供所有 SAX 事件方法的空实现。

## 方法

### startDocument / endDocument / startElement / endElement

### characters / warning / error / fatalError

## 测试

- 描述: DefaultHandler 处理 XML
- 断言: 事件处理正确

```java
// 方法体开始
System.out.println("=== DefaultHandler ===");
SAXParserFactory spf = SAXParserFactory.newInstance();
SAXParser parser = spf.newSAXParser();
List<String> log = new ArrayList<>();
DefaultHandler handler = new DefaultHandler() {
    public void startElement(String uri, String ln, String qn, Attributes a) {
        log.add("START:" + qn);
    }
    public void endElement(String uri, String ln, String qn) {
        log.add("END:" + qn);
    }
};
parser.parse(new ByteArrayInputStream("<a><b/></a>".getBytes("UTF-8")), handler);
assertTrue(log.contains("START:a"));
assertTrue(log.contains("START:b"));
System.out.println("DefaultHandler 事件: " + log);
System.out.println("=== 测试通过 ===");
// 方法体结束
```
