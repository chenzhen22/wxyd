---
name: GZIPInputStream
package: java.util.zip
order: 254
---

## 介绍

`java.util.zip.GZIPInputStream` 是 **GZIP 解压输入流**，用于读取并解压 GZIP 格式的数据。

## 方法

构造方法：
```java
public GZIPInputStream(InputStream in) throws IOException
```

### read

读取解压后的数据。

## 测试

- 描述: 解压 GZIP 数据
- 断言: 解压后数据正确

```java
// 方法体开始
System.out.println("=== GZIPInputStream ===");
ByteArrayOutputStream baos = new ByteArrayOutputStream();
GZIPOutputStream gzos = new GZIPOutputStream(baos);
gzos.write("Test Data".getBytes("UTF-8"));
gzos.close();
GZIPInputStream gzis = new GZIPInputStream(new ByteArrayInputStream(baos.toByteArray()));
byte[] data = new byte[9];
int n = gzis.read(data);
assertEquals(9, n);
assertEquals("Test Data", new String(data, "UTF-8"));
gzis.close();
System.out.println("GZIP 解压完成");
System.out.println("=== 测试通过 ===");
// 方法体结束
```
