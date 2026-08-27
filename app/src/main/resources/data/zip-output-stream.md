---
name: ZipOutputStream
package: java.util.zip
order: 252
---

## 介绍

`java.util.zip.ZipOutputStream` 是 **ZIP 输出流**，用于创建 ZIP 格式的压缩文件。

## 方法

构造方法：
```java
public ZipOutputStream(OutputStream out)
```

### putNextEntry / closeEntry

```java
public void putNextEntry(ZipEntry e) throws IOException
public void closeEntry() throws IOException
```

### write

写入压缩数据。

### setLevel / setMethod

设置压缩级别和方法。

## 测试

- 描述: 创建 ZIP 压缩数据
- 断言: 压缩成功

```java
// 方法体开始
System.out.println("=== ZipOutputStream ===");
ByteArrayOutputStream baos = new ByteArrayOutputStream();
ZipOutputStream zos = new ZipOutputStream(baos);
ZipEntry entry = new ZipEntry("test.txt");
zos.putNextEntry(entry);
zos.write("Hello Zip".getBytes("UTF-8"));
zos.closeEntry();
zos.close();
assertTrue(baos.size() > 0);
System.out.println("ZIP 数据大小: " + baos.size() + " 字节");
System.out.println("=== 测试通过 ===");
// 方法体结束
```
