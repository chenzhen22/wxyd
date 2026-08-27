---
name: RandomAccessFile
package: java.io
order: 211
---

## 介绍

`java.io.RandomAccessFile` 是**随机访问文件**类，支持在文件任意位置读写数据。它支持读/写/读写三种模式。

## 方法

构造方法：
```java
public RandomAccessFile(String name, String mode) throws FileNotFoundException
public RandomAccessFile(File file, String mode) throws FileNotFoundException
```

模式：`"r"` 只读、`"rw"` 读写、`"rws"` 同步读写、`"rwd"` 数据同步

### read / write / seek / getFilePointer / length

核心随机访问方法。

## 测试

- 描述: 随机读写文件
- 断言: 读写结果正确

```java
// 方法体开始
System.out.println("=== RandomAccessFile ===");
Path tmpFile = Files.createTempFile("raf-", ".dat");
RandomAccessFile raf = new RandomAccessFile(tmpFile.toFile(), "rw");
raf.writeInt(100);
raf.writeDouble(3.14);
raf.writeUTF("test");
raf.seek(0);
assertEquals(100, raf.readInt());
assertEquals(3.14, raf.readDouble(), 0.0001);
assertEquals("test", raf.readUTF());
raf.seek(0);
raf.writeInt(200);  // 覆盖第一个 int
raf.seek(0);
assertEquals(200, raf.readInt());
raf.close();
Files.delete(tmpFile);
System.out.println("RandomAccessFile 随机读写成功");
System.out.println("=== 测试通过 ===");
// 方法体结束
```
