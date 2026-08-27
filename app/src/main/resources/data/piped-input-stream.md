---
name: PipedInputStream
package: java.io
order: 205
---

## 介绍

`java.io.PipedInputStream` 是**管道输入流**，与 `PipedOutputStream` 配合在线程间传递数据。数据通过管道从一个线程传递到另一个线程。

## 方法

构造方法：
```java
public PipedInputStream()
public PipedInputStream(PipedOutputStream src) throws IOException
```

### connect

```java
public void connect(PipedOutputStream src) throws IOException
```

连接到管道输出流。

### read

从管道读取字节。

## 测试

- 描述: 管道线程间通信
- 断言: 数据正确传递

```java
// 方法体开始
System.out.println("=== PipedInputStream ===");
PipedOutputStream pos = new PipedOutputStream();
PipedInputStream pis = new PipedInputStream(pos);
Thread writer = new Thread(() -> {
    try {
        pos.write("Hello".getBytes("UTF-8"));
        pos.close();
    } catch (IOException e) {}
});
writer.start();
byte[] buf = new byte[5];
int n = pis.read(buf);
assertEquals(5, n);
assertEquals("Hello", new String(buf, "UTF-8"));
pis.close();
System.out.println("管道读取: " + new String(buf));
System.out.println("=== 测试通过 ===");
// 方法体结束
```
