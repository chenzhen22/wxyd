---
name: CRC32
package: java.util.zip
order: 255
---

## 介绍

`java.util.zip.CRC32` 是 **CRC32 校验和类**，用于计算数据的 32 位循环冗余校验和。

## 方法

构造方法：
```java
public CRC32()
```

### update / getValue / reset

```java
public void update(byte[] b)
public long getValue()
public void reset()
```

## 测试

- 描述: 计算 CRC32 校验和
- 断言: 相同数据产生相同校验值

```java
// 方法体开始
System.out.println("=== CRC32 ===");
CRC32 crc1 = new CRC32();
crc1.update("Hello".getBytes("UTF-8"));
long val1 = crc1.getValue();
CRC32 crc2 = new CRC32();
crc2.update("Hello".getBytes("UTF-8"));
long val2 = crc2.getValue();
CRC32 crc3 = new CRC32();
crc3.update("World".getBytes("UTF-8"));
long val3 = crc3.getValue();
assertEquals(val1, val2);  // 相同数据 → 相同校验值
assertNotEquals(val1, val3);  // 不同数据 → 不同校验值
System.out.println("CRC32('Hello') = " + Long.toHexString(val1));
System.out.println("CRC32('World') = " + Long.toHexString(val3));
System.out.println("=== 测试通过 ===");
// 方法体结束
```
