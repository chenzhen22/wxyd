---
name: CheckedInputStream
package: java.util.zip
order: 299
---

## 介绍

`java.util.zip.CheckedInputStream` 是**校验和输入流**，在读取数据时计算校验和。

## 方法

构造方法：
```java
public CheckedInputStream(InputStream in, Checksum cksum)
```

### read / getChecksum

读取数据 / 获取校验和。

## 测试

- 描述: 读取时计算校验和
- 断言: 校验和正确

```java
// 方法体开始
System.out.println("=== CheckedInputStream ===");
byte[] data = "Hello Checksum".getBytes("UTF-8");
CRC32 crc = new CRC32();
CheckedInputStream cis = new CheckedInputStream(new ByteArrayInputStream(data), crc);
byte[] buf = new byte[data.length];
cis.read(buf);
Checksum checksum = cis.getChecksum();
assertTrue(checksum.getValue() > 0);
CRC32 crc2 = new CRC32();
crc2.update("Hello Checksum".getBytes("UTF-8"));
assertEquals(crc2.getValue(), checksum.getValue());
cis.close();
System.out.println("CRC32 校验和: " + checksum.getValue());
System.out.println("=== 测试通过 ===");
// 方法体结束
```
