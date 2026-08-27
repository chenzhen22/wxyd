---
name: PipedOutputStream
package: java.io
order: 206
---

## 介绍

`java.io.PipedOutputStream` 是**管道输出流**，与 `PipedInputStream` 配合在线程间传递数据。

## 方法

构造方法：
```java
public PipedOutputStream()
public PipedOutputStream(PipedInputStream snk) throws IOException
```

### write / close

写入数据 / 关闭流。

## 测试

- 描述: 通过管道输出流发送数据
- 断言: 数据正确发送

```java
// 方法体开始
System.out.println("=== PipedOutputStream ===");
PipedOutputStream pos = new PipedOutputStream();
PipedInputStream pis = new PipedInputStream(pos);
Thread writer = new Thread(() -> {
    try {
        pos.write("data".getBytes("UTF-8"));
        pos.close();
    } catch (IOException e) {}
});
writer.start();
byte[] buf = new byte[4];
pis.read(buf);
assertEquals("data", new String(buf, "UTF-8"));
pis.close();
System.out.println("PipedOutputStream 发送: data");
System.out.println("=== 测试通过 ===");
// 方法体结束
```
