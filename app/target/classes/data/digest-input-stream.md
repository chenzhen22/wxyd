---
name: DigestInputStream
package: java.security
order: 304
---

## 介绍

`java.security.DigestInputStream` 是**摘要输入流**，在读取数据时自动计算消息摘要（如 MD5、SHA-256）。

## 方法

构造方法：
```java
public DigestInputStream(InputStream stream, MessageDigest digest)
```

### read / getMessageDigest / setMessageDigest

## 测试

- 描述: 读取时自动计算 SHA-256 摘要
- 断言: 摘要计算正确

```java
// 方法体开始
System.out.println("=== DigestInputStream ===");
byte[] data = "Hello Digest".getBytes("UTF-8");
MessageDigest md = MessageDigest.getInstance("SHA-256");
DigestInputStream dis = new DigestInputStream(new ByteArrayInputStream(data), md);
byte[] buf = new byte[data.length];
dis.read(buf);
MessageDigest digest = dis.getMessageDigest();
byte[] hash = digest.digest();
assertEquals(32, hash.length);
// 验证与直接计算一致
MessageDigest md2 = MessageDigest.getInstance("SHA-256");
byte[] expected = md2.digest("Hello Digest".getBytes("UTF-8"));
assertArrayEquals(expected, hash);
dis.close();
System.out.println("DigestInputStream SHA-256 计算完成");
System.out.println("=== 测试通过 ===");
// 方法体结束
```
