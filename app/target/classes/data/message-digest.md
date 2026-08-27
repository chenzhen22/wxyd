---
name: MessageDigest
package: java.security
order: 216
---

## 介绍

`java.security.MessageDigest` 是**消息摘要算法**类，用于计算哈希值，如 MD5、SHA-1、SHA-256。

## 方法

### getInstance

```java
public static MessageDigest getInstance(String algorithm) throws NoSuchAlgorithmException
```

获取指定算法的 MessageDigest 实例。

### digest

```java
public byte[] digest(byte[] input)
public byte[] digest()
```

计算消息摘要。

### update

```java
public void update(byte[] input)
```

更新摘要数据。

## 测试

- 描述: 计算 SHA-256 哈希
- 断言: 哈希值固定

```java
// 方法体开始
System.out.println("=== MessageDigest ===");
MessageDigest md = MessageDigest.getInstance("SHA-256");
byte[] hash = md.digest("Hello".getBytes("UTF-8"));
assertEquals(32, hash.length);  // SHA-256 输出 32 字节
StringBuilder hex = new StringBuilder();
for (byte b : hash) hex.append(String.format("%02x", b & 0xff));
assertEquals(64, hex.length());
System.out.println("SHA-256: " + hex);
System.out.println("=== 测试通过 ===");
// 方法体结束
```
