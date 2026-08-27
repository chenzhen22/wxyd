---
name: CheckedOutputStream
package: java.util.zip
order: 300
---

## 介绍

`java.util.zip.CheckedOutputStream` 是**校验和输出流**，在写入数据时计算校验和。

## 方法

构造方法：
```java
public CheckedOutputStream(OutputStream out, Checksum cksum)
```

### write / getChecksum

## 测试

- 描述: 写入时计算校验和
- 断言: 校验和正确

```java
// 方法体开始
System.out.println("=== CheckedOutputStream ===");
ByteArrayOutputStream baos = new ByteArrayOutputStream();
CRC32 crc = new CRC32();
CheckedOutputStream cos = new CheckedOutputStream(baos, crc);
cos.write("Test Data".getBytes("UTF-8"));
cos.close();
CRC32 crc2 = new CRC32();
crc2.update("Test Data".getBytes("UTF-8"));
assertEquals(crc2.getValue(), cos.getChecksum().getValue());
System.out.println("写入时 CRC32: " + cos.getChecksum().getValue());
System.out.println("=== 测试通过 ===");
// 方法体结束
```
