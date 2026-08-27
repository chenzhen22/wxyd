---
name: DigestOutputStream
package: java.security
order: 305
---

## 介绍

`java.security.DigestOutputStream` 是**摘要输出流**，在写入数据时自动计算消息摘要。

## 方法

构造方法：
```java
public DigestOutputStream(OutputStream stream, MessageDigest digest)
```

### write / getMessageDigest / setMessageDigest / on

## 测试

- 描述: 写入时自动计算 SHA-256 摘要
- 断言: 摘要计算正确

```java
// 方法体开始
System.out.println("=== DigestOutputStream ===");
MessageDigest md = MessageDigest.getInstance("SHA-256");
ByteArrayOutputStream baos = new ByteArrayOutputStream();
DigestOutputStream dos = new DigestOutputStream(baos, md);
dos.write("Hello Digest".getBytes("UTF-8"));
dos.close();
MessageDigest digest = dos.getMessageDigest();
byte[] hash = digest.digest();
MessageDigest md2 = MessageDigest.getInstance("SHA-256");
byte[] expected = md2.digest("Hello Digest".getBytes("UTF-8"));
assertArrayEquals(expected, hash);
System.out.println("DigestOutputStream SHA-256 计算完成");
System.out.println("=== 测试通过 ===");
// 方法体结束
```
