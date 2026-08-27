---
name: CharsetDecoder
package: java.nio.charset
order: 373
---

## 介绍

`java.nio.charset.CharsetDecoder` 是**字符集解码器**，将字节序列解码为 Unicode 字符。

## 方法

### decode / charset / averageCharsPerByte / maxCharsPerByte

## 测试

- 描述: 解码字节为字符
- 断言: 解码正确

```java
// 方法体开始
System.out.println("=== CharsetDecoder ===");
CharsetDecoder decoder = Charset.forName("UTF-8").newDecoder();
ByteBuffer buf = ByteBuffer.wrap("Hello World".getBytes("UTF-8"));
CharBuffer result = decoder.decode(buf);
assertEquals("Hello World", result.toString());
System.out.println("UTF-8 解码: " + result);
System.out.println("=== 测试通过 ===");
// 方法体结束
```
