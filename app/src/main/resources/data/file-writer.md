---
name: FileWriter
package: java.io
order: 203
---

## 介绍

`java.io.FileWriter` 是**文件字符输出流**，方便地写入字符数据到文件。

## 方法

构造方法：
```java
public FileWriter(String fileName) throws IOException
public FileWriter(String fileName, boolean append) throws IOException
```

### write / close

写入字符 / 关闭流。

## 测试

- 描述: 写入字符到文件
- 断言: 写入结果正确

```java
// 方法体开始
System.out.println("=== FileWriter ===");
Path tmpFile = Files.createTempFile("fw-", ".txt");
FileWriter fw = new FileWriter(tmpFile.toFile(), java.nio.charset.Charset.forName("UTF-8"));
fw.write("Hello FileWriter");
fw.close();
String content = new String(Files.readAllBytes(tmpFile), "UTF-8");
assertEquals("Hello FileWriter", content);
Files.delete(tmpFile);
System.out.println("FileWriter 写入完成");
System.out.println("=== 测试通过 ===");
// 方法体结束
```
