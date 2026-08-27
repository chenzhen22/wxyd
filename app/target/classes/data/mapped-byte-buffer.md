---
name: MappedByteBuffer
package: java.nio
order: 362
---

## 介绍

`java.nio.MappedByteBuffer` 是**内存映射文件缓冲区**，将文件区域直接映射到内存中，实现超高速文件 I/O。

## 方法

### load / isLoaded / force

## 测试

- 描述: 内存映射文件
- 断言: 读写正确

```java
// 方法体开始
System.out.println("=== MappedByteBuffer ===");
Path tmpFile = Files.createTempFile("mapped-", ".dat");
try (RandomAccessFile raf = new RandomAccessFile(tmpFile.toFile(), "rw");
     FileChannel channel = raf.getChannel()) {
    MappedByteBuffer mapped = channel.map(FileChannel.MapMode.READ_WRITE, 0, 1024);
    mapped.put("Hello MappedBuffer".getBytes("UTF-8"));
    mapped.force();
    mapped.position(0);
    byte[] data = new byte[18];
    mapped.get(data);
    assertEquals("Hello MappedBuffer", new String(data, "UTF-8"));
    System.out.println("MappedByteBuffer 测试通过");
}
Files.delete(tmpFile);
System.out.println("=== 测试通过 ===");
// 方法体结束
```
