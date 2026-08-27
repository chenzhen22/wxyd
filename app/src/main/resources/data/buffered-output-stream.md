---
name: BufferedOutputStream
package: java.io
order: 189
---

## 介绍

`java.io.BufferedOutputStream` 是**缓冲字节输出流**，减少实际写入底层输出流的次数。

## 方法

构造方法：
```java
public BufferedOutputStream(OutputStream out)
public BufferedOutputStream(OutputStream out, int size)
```

### write / flush

写入数据 / 刷新缓冲区。

## 测试

- 描述: 写入缓冲字节流
- 断言: 写入结果正确

```java
// 方法体开始
System.out.println("=== BufferedOutputStream ===");
ByteArrayOutputStream baos = new ByteArrayOutputStream();
BufferedOutputStream bos = new BufferedOutputStream(baos);
bos.write("Hello".getBytes("UTF-8"));
bos.flush();
assertEquals("Hello", baos.toString("UTF-8"));
bos.close();
System.out.println("写入: " + baos.toString("UTF-8"));
System.out.println("=== 测试通过 ===");
// 方法体结束
```
