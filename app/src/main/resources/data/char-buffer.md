---
name: CharBuffer
package: java.nio
order: 367
---

## 介绍

`java.nio.CharBuffer` 是**字符缓冲区**，是 NIO 缓冲区体系的一部分，用于高效处理字符数据。

## 方法

### allocate / wrap

```java
public static CharBuffer allocate(int capacity)
public static CharBuffer wrap(CharSequence csq)
```

### put / get / flip / clear / rewind / remaining

### toString / length / subSequence

## 测试

- 描述: CharBuffer 基本操作
- 断言: 读写正确

```java
// 方法体开始
System.out.println("=== CharBuffer ===");
CharBuffer buf = CharBuffer.allocate(20);
buf.put("Hello");
buf.put(" ");
buf.put("CharBuffer");
buf.flip();
assertEquals(12, buf.remaining());
String result = buf.toString();
assertEquals("Hello CharBuffer", result);
buf.rewind();
assertEquals('H', buf.get());
assertEquals('e', buf.get());
System.out.println("CharBuffer: " + result);
System.out.println("=== 测试通过 ===");
// 方法体结束
```
