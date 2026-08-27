---
name: Deflater
package: java.util.zip
order: 276
---

## 介绍

`java.util.zip.Deflater` 是**数据压缩器**类，用于压缩字节数据（ZLIB 格式）。

## 方法

构造方法：
```java
public Deflater()
public Deflater(int level)
```

### setInput / deflate / finished

设置输入/压缩/检查完成。

### deflate

```java
public int deflate(byte[] b)
```

压缩数据到输出缓冲区。

## 测试

- 描述: 压缩和解压数据
- 断言: 解压后与原文一致

```java
// 方法体开始
System.out.println("=== Deflater ===");
String input = "Hello World! Hello World! Hello World! Hello World!";
byte[] data = input.getBytes("UTF-8");
Deflater deflater = new Deflater(Deflater.BEST_COMPRESSION);
deflater.setInput(data);
deflater.finish();
byte[] compressed = new byte[100];
int len = deflater.deflate(compressed);
deflater.end();
Inflater inflater = new Inflater();
inflater.setInput(compressed, 0, len);
byte[] result = new byte[data.length];
int resultLen = inflater.inflate(result);
inflater.end();
String output = new String(result, 0, resultLen, "UTF-8");
assertEquals(input, output);
System.out.println("原始大小: " + data.length + ", 压缩后: " + len);
System.out.println("=== 测试通过 ===");
// 方法体结束
```
