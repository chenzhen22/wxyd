---
name: GZIPOutputStream
package: java.util.zip
order: 253
---

## 介绍

`java.util.zip.GZIPOutputStream` 是 **GZIP 压缩输出流**，用于创建 GZIP 格式的压缩数据。

## 方法

构造方法：
```java
public GZIPOutputStream(OutputStream out) throws IOException
```

### write / finish / close

写入压缩数据 / 完成压缩 / 关闭。

## 测试

- 描述: GZIP 压缩和解压
- 断言: 解压后与原文一致

```java
// 方法体开始
System.out.println("=== GZIPOutputStream ===");
ByteArrayOutputStream baos = new ByteArrayOutputStream();
GZIPOutputStream gzos = new GZIPOutputStream(baos);
gzos.write("Hello GZIP!".getBytes("UTF-8"));
gzos.close();
assertTrue(baos.size() > 0);
ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
GZIPInputStream gzis = new GZIPInputStream(bais);
String result = new java.util.Scanner(gzis, "UTF-8").useDelimiter("\\A").next();
assertEquals("Hello GZIP!", result);
gzis.close();
System.out.println("GZIP 压缩大小: " + baos.size() + " 字节");
System.out.println("解压: " + result);
System.out.println("=== 测试通过 ===");
// 方法体结束
```
