---
name: Pipe
package: java.nio.channels
order: 364
---

## 介绍

`java.nio.channels.Pipe` 是 **NIO 管道**类，用于在同一个 JVM 中的两个线程之间传递数据。

## 方法

### open / source / sink

```java
public static Pipe open() throws IOException
public SourceChannel source()
public SinkChannel sink()
```

## 测试

- 描述: 使用 Pipe 在线程间通信
- 断言: 数据正确传递

```java
// 方法体开始
System.out.println("=== Pipe ===");
Pipe pipe = Pipe.open();
Pipe.SinkChannel sink = pipe.sink();
Pipe.SourceChannel source = pipe.source();
Thread writer = new Thread(() -> {
    try {
        sink.write(ByteBuffer.wrap("Hello Pipe".getBytes("UTF-8")));
    } catch (IOException e) {}
});
writer.start();
ByteBuffer buf = ByteBuffer.allocate(20);
int len = source.read(buf);
assertEquals(10, len);
buf.flip();
byte[] data = new byte[len];
buf.get(data);
assertEquals("Hello Pipe", new String(data, "UTF-8"));
writer.join();
source.close();
sink.close();
System.out.println("Pipe 通信测试通过");
System.out.println("=== 测试通过 ===");
// 方法体结束
```
