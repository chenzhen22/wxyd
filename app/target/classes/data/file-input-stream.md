---
name: FileInputStream
package: java.io
order: 200
---

## 介绍

`java.io.FileInputStream` 是**文件字节输入流**，从文件系统中读取字节数据。Java 8 中推荐使用 `Files.newInputStream(Path)` 代替，但 FileInputStream 仍广泛用于遗留代码。

## 方法

构造方法：
```java
public FileInputStream(String name) throws FileNotFoundException
public FileInputStream(File file) throws FileNotFoundException
```

### read / available / close

标准 InputStream 方法。

## 测试

- 描述: 读取文件字节
- 断言: 读取结果正确

```java
// 方法体开始
System.out.println("=== FileInputStream ===");
Path tmpFile = Files.createTempFile("fis-", ".txt");
Files.write(tmpFile, "Hello".getBytes("UTF-8"));
FileInputStream fis = new FileInputStream(tmpFile.toFile());
byte[] data = new byte[5];
int n = fis.read(data);
assertEquals(5, n);
assertEquals("Hello", new String(data, "UTF-8"));
fis.close();
Files.delete(tmpFile);
System.out.println("FileInputStream 读取完成");
System.out.println("=== 测试通过 ===");
// 方法体结束
```
