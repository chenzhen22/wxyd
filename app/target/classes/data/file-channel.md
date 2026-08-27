---
name: FileChannel
package: java.nio.channels
order: 349
---

## 介绍

`java.nio.channels.FileChannel` 是**文件通道**类，用于通过 NIO 方式读写文件，支持文件锁、内存映射文件等高级功能。

## 方法

### open

```java
public static FileChannel open(Path path, OpenOption... options) throws IOException
```

### read / write / position / size / truncate

### map / force / lock / tryLock

## 测试

- 描述: 使用 FileChannel 读写文件
- 断言: 读写正确

```java
// 方法体开始
System.out.println("=== FileChannel ===");
Path tmpFile = Files.createTempFile("channel-", ".txt");
try (FileChannel channel = FileChannel.open(tmpFile, 
        StandardOpenOption.WRITE, StandardOpenOption.READ)) {
    ByteBuffer buf = ByteBuffer.wrap("Hello NIO".getBytes("UTF-8"));
    int written = channel.write(buf);
    assertEquals(9, written);
    channel.position(0);
    ByteBuffer readBuf = ByteBuffer.allocate(9);
    int read = channel.read(readBuf);
    assertEquals(9, read);
    readBuf.flip();
    byte[] data = new byte[readBuf.remaining()];
    readBuf.get(data);
    assertEquals("Hello NIO", new String(data, "UTF-8"));
    System.out.println("FileChannel 读写成功");
}
Files.delete(tmpFile);
System.out.println("=== 测试通过 ===");
// 方法体结束
```
