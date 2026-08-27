---
name: SequenceInputStream
package: java.io
order: 207
---

## 介绍

`java.io.SequenceInputStream` 是**序列输入流**，将多个 InputStream 合并为一个，依次读取每个输入流。

## 方法

构造方法：
```java
public SequenceInputStream(Enumeration<? extends InputStream> e)
public SequenceInputStream(InputStream s1, InputStream s2)
```

### read / close

标准 InputStream 方法。

## 测试

- 描述: 合并两个输入流
- 断言: 按顺序读取

```java
// 方法体开始
System.out.println("=== SequenceInputStream ===");
ByteArrayInputStream bais1 = new ByteArrayInputStream("Hello ".getBytes("UTF-8"));
ByteArrayInputStream bais2 = new ByteArrayInputStream("World".getBytes("UTF-8"));
SequenceInputStream sis = new SequenceInputStream(bais1, bais2);
byte[] all = new byte[11];
int total = 0, n;
while ((n = sis.read(all, total, all.length - total)) != -1) {
    total += n;
}
assertEquals(11, total);
assertEquals("Hello World", new String(all, "UTF-8"));
sis.close();
System.out.println("合并: " + new String(all));
System.out.println("=== 测试通过 ===");
// 方法体结束
```
