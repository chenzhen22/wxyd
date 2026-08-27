---
name: OutputStreamWriter
package: java.io
order: 191
---

## 介绍

`java.io.OutputStreamWriter` 是**字符流到字节流的桥接器**，将字符编码为字节。

## 方法

构造方法：
```java
public OutputStreamWriter(OutputStream out)
public OutputStreamWriter(OutputStream out, String charsetName)
```

### write

写入字符。

## 测试

- 描述: 字符流转字节流
- 断言: 写入结果正确

```java
// 方法体开始
System.out.println("=== OutputStreamWriter ===");
ByteArrayOutputStream baos = new ByteArrayOutputStream();
OutputStreamWriter osw = new OutputStreamWriter(baos, "UTF-8");
osw.write("Hello");
osw.flush();
assertEquals("Hello", baos.toString("UTF-8"));
osw.close();
System.out.println("写入: " + baos.toString("UTF-8"));
System.out.println("=== 测试通过 ===");
// 方法体结束
```
