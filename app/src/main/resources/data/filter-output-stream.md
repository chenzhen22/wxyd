---
name: FilterOutputStream
package: java.io
order: 289
---

## 介绍

`java.io.FilterOutputStream` 是**装饰器模式的输出流基类**，`BufferedOutputStream`、`DataOutputStream`、`PrintStream` 的父类。

## 方法

构造方法：
```java
public FilterOutputStream(OutputStream out)
```

### write / flush / close

将调用委托给底层输出流。

## 测试

- 描述: 使用 FilterOutputStream
- 断言: 委托调用正确

```java
// 方法体开始
System.out.println("=== FilterOutputStream ===");
ByteArrayOutputStream baos = new ByteArrayOutputStream();
FilterOutputStream fos = new FilterOutputStream(baos) {};
fos.write("Hello".getBytes("UTF-8"));
fos.flush();
fos.close();
assertEquals("Hello", baos.toString("UTF-8"));
System.out.println("FilterOutputStream 测试完成");
System.out.println("=== 测试通过 ===");
// 方法体结束
```
