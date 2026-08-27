---
name: FilterInputStream
package: java.io
order: 288
---

## 介绍

`java.io.FilterInputStream` 是**装饰器模式的输入流基类**，所有缓冲流（`BufferedInputStream`）、数据流（`DataInputStream`）的父类。

## 方法

构造方法：
```java
protected FilterInputStream(InputStream in)
```

### read / available / close / skip

将这些调用委托给底层输入流。

## 测试

- 描述: 使用 FilterInputStream
- 断言: 委托调用正确

```java
// 方法体开始
System.out.println("=== FilterInputStream ===");
byte[] data = "Hello Filter".getBytes("UTF-8");
FilterInputStream fis = new FilterInputStream(new ByteArrayInputStream(data)) {};
assertEquals('H', fis.read());
assertEquals(11, fis.available());
fis.close();
System.out.println("FilterInputStream 测试完成");
System.out.println("=== 测试通过 ===");
// 方法体结束
```
