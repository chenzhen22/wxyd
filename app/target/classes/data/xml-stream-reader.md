---
name: XMLStreamReader
package: javax.xml.stream
order: 378
---

## 介绍

`javax.xml.stream.XMLStreamReader` 是 **StAX 流式 XML 读取器**，提供基于指针的 XML 解析。

## 方法

### next / hasNext / getEventType

### getLocalName / getText / getAttributeValue

### START_ELEMENT / END_ELEMENT / CHARACTERS / START_DOCUMENT / END_DOCUMENT

## 测试

- 描述: StAX 流式读取 XML
- 断言: 解析正确

```java
// 方法体开始
System.out.println("=== XMLStreamReader ===");
XMLInputFactory factory = XMLInputFactory.newInstance();
String xml = "<root><item id=\"1\">Hello</item></root>";
XMLStreamReader reader = factory.createXMLStreamReader(new ByteArrayInputStream(xml.getBytes("UTF-8")));
List<String> events = new ArrayList<>();
while (reader.hasNext()) {
    int event = reader.next();
    switch (event) {
        case XMLStreamReader.START_ELEMENT:
            events.add("<" + reader.getLocalName() + ">");
            break;
        case XMLStreamReader.END_ELEMENT:
            events.add("</" + reader.getLocalName() + ">");
            break;
        case XMLStreamReader.CHARACTERS:
            events.add(reader.getText());
            break;
    }
}
reader.close();
assertEquals("[<root>, <item>, Hello, </item>, </root>]", events.toString());
System.out.println("XML 事件: " + events);
System.out.println("=== 测试通过 ===");
// 方法体结束
```
