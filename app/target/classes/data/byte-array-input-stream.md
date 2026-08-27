---
name: ByteArrayInputStream
package: java.io
order: 192
---

## 介绍

`java.io.ByteArrayInputStream` 是**内存字节输入流**，将字节数组作为数据源，常用于测试和内存数据处理。

## 方法

构造方法：
```java
public ByteArrayInputStream(byte[] buf)
public ByteArrayInputStream(byte[] buf, int offset, int length)
```

### read / available / skip

标准 InputStream 方法。

## 测试

- 描述: 从内存读取字节
- 断言: 读取结果正确

```java
// 方法体开始
System.out.println("=== ByteArrayInputStream ===");
byte[] data = {10, 20, 30, 40, 50};
ByteArrayInputStream bais = new ByteArrayInputStream(data);
assertEquals(10, bais.read());
assertEquals(20, bais.read());
assertEquals(5, bais.available());
bais.skip(2);
assertEquals(50, bais.read());
assertEquals(-1, bais.read());
bais.close();
System.out.println("读取完成");
System.out.println("=== 测试通过 ===");
// 方法体结束
```
