---
name: StringWriter
package: java.io
order: 199
---

## 介绍

`java.io.StringWriter` 是**字符串字符输出流**，字符数据写入到 StringBuffer 中。

## 方法

构造方法：
```java
public StringWriter()
public StringWriter(int initialSize)
```

### write / toString / getBuffer

写入字符 / 获取字符串 / 获取 StringBuffer。

## 测试

- 描述: 写入字符串到字符流
- 断言: 写入结果正确

```java
// 方法体开始
System.out.println("=== StringWriter ===");
StringWriter sw = new StringWriter();
sw.write("Hello");
sw.write(" World");
assertEquals("Hello World", sw.toString());
System.out.println("StringWriter: " + sw.toString());
System.out.println("=== 测试通过 ===");
// 方法体结束
```
