---
name: CharsetEncoder
package: java.nio.charset
order: 372
---

## 介绍

`java.nio.charset.CharsetEncoder` 是**字符集编码器**，将 Unicode 字符编码为字节序列。

## 方法

### encode / flush

### isLegalReplacement / replacement / onMalformedInput

## 测试

- 描述: 编码字符串为字节
- 断言: 编码成功

```java
// 方法体开始
System.out.println("=== CharsetEncoder ===");
CharsetEncoder encoder = Charset.forName("UTF-8").newEncoder();
assertNotNull(encoder);
ByteBuffer buf = encoder.encode(CharBuffer.wrap("你好"));
assertTrue(buf.remaining() > 0);
byte[] data = new byte[buf.remaining()];
buf.get(data);
String decoded = new String(data, "UTF-8");
assertEquals("你好", decoded);
System.out.println("UTF-8 编码: '你好' -> " + data.length + " 字节");
System.out.println("=== 测试通过 ===");
// 方法体结束
```
