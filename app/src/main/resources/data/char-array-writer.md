---
name: CharArrayWriter
package: java.io
order: 210
---

## 介绍

`java.io.CharArrayWriter` 是**字符数组输出流**，字符写入到内部的字符数组缓冲区。

## 方法

构造方法：
```java
public CharArrayWriter()
public CharArrayWriter(int initialSize)
```

### write / toString / toCharArray / reset

写入 / 获取字符串 / 获取字符数组 / 重置缓冲区。

## 测试

- 描述: 写入字符到缓冲区
- 断言: 结果正确

```java
// 方法体开始
System.out.println("=== CharArrayWriter ===");
CharArrayWriter caw = new CharArrayWriter();
caw.write("Hello");
caw.write(" World");
assertEquals("Hello World", caw.toString());
assertEquals(11, caw.toCharArray().length);
caw.reset();
assertEquals(0, caw.size());
System.out.println("CharArrayWriter 测试通过");
System.out.println("=== 测试通过 ===");
// 方法体结束
```
