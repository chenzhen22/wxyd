---
name: SAXParserFactory
package: javax.xml.parsers
order: 376
---

## 介绍

`javax.xml.parsers.SAXParserFactory` 是 **SAX 解析器工厂**类。

## 方法

### newInstance / newSAXParser

### setNamespaceAware / setValidating / setFeature

## 测试

- 描述: 创建 SAXParserFactory
- 断言: 创建成功

```java
// 方法体开始
System.out.println("=== SAXParserFactory ===");
SAXParserFactory spf = SAXParserFactory.newInstance();
spf.setNamespaceAware(true);
assertTrue(spf.isNamespaceAware());
SAXParser parser = spf.newSAXParser();
assertNotNull(parser);
System.out.println("SAXParserFactory 创建成功");
System.out.println("=== 测试通过 ===");
// 方法体结束
```
