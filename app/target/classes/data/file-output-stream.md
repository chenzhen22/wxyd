---
name: FileOutputStream
package: java.io
order: 201
---

## 介绍

`java.io.FileOutputStream` 是**文件字节输出流**，将字节数据写入文件。

## 方法

构造方法：
```java
public FileOutputStream(String name) throws FileNotFoundException
public FileOutputStream(File file, boolean append) throws FileNotFoundException
```

### write / close

标准 OutputStream 方法。

## 测试

- 描述: 写入文件字节
- 断言: 写入结果正确

```java
// 方法体开始
System.out.println("=== FileOutputStream ===");
Path tmpFile = Files.createTempFile("fos-", ".txt");
FileOutputStream fos = new FileOutputStream(tmpFile.toFile());
fos.write("Hello".getBytes("UTF-8"));
fos.close();
String content = new String(Files.readAllBytes(tmpFile), "UTF-8");
assertEquals("Hello", content);
Files.delete(tmpFile);
System.out.println("FileOutputStream 写入完成");
System.out.println("=== 测试通过 ===");
// 方法体结束
```
