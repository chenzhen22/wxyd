---
name: Inflater
package: java.util.zip
order: 277
---

## 介绍

`java.util.zip.Inflater` 是**数据解压器**类，用于解压 ZLIB 格式的压缩数据。

## 方法

构造方法：
```java
public Inflater()
```

### setInput / inflate / finished / end

设置输入 / 解压 / 检查完成 / 结束。

## 测试

- 描述: 解压 ZLIB 数据
- 断言: 解压结果正确

```java
// 方法体开始
System.out.println("=== Inflater ===");
String original = "Java compression test with Inflater/Deflater!";
byte[] input = original.getBytes("UTF-8");
Deflater deflater = new Deflater();
deflater.setInput(input);
deflater.finish();
byte[] compressed = new byte[200];
int compressedLen = deflater.deflate(compressed);
deflater.end();
Inflater inflater = new Inflater();
inflater.setInput(compressed, 0, compressedLen);
byte[] result = new byte[input.length];
int resultLen = inflater.inflate(result);
inflater.end();
assertEquals(original, new String(result, 0, resultLen, "UTF-8"));
System.out.println("解压正确: " + new String(result, 0, resultLen));
System.out.println("=== 测试通过 ===");
// 方法体结束
```
