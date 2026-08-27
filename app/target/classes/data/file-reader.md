---
name: FileReader
package: java.io
order: 202
---

## 介绍

`java.io.FileReader` 是**文件字符输入流**，方便地读取文件中的字符数据。

## 方法

构造方法：
```java
public FileReader(String fileName) throws FileNotFoundException
public FileReader(File file) throws FileNotFoundException
```

### read

读取字符。

## 测试

- 描述: 从文件读取字符
- 断言: 读取结果正确

```java
// 方法体开始
System.out.println("=== FileReader ===");
Path tmpFile = Files.createTempFile("fr-", ".txt");
Files.write(tmpFile, "你好".getBytes("UTF-8"));
FileReader fr = new FileReader(tmpFile.toFile(), java.nio.charset.Charset.forName("UTF-8"));
char[] buf = new char[2];
int n = fr.read(buf);
assertEquals(2, n);
assertEquals("你好", new String(buf, 0, n));
fr.close();
Files.delete(tmpFile);
System.out.println("FileReader: " + new String(buf, 0, n));
System.out.println("=== 测试通过 ===");
// 方法体结束
```
