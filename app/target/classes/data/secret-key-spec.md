---
name: SecretKeySpec
package: javax.crypto.spec
order: 296
---

## 介绍

`javax.crypto.spec.SecretKeySpec` 是**密钥规范类**，从字节数组创建密钥，无需 KeyGenerator。

## 方法

构造方法：
```java
public SecretKeySpec(byte[] key, String algorithm)
```

### getAlgorithm / getFormat / getEncoded

## 测试

- 描述: 从字节数组创建密钥
- 断言: 创建成功

```java
// 方法体开始
System.out.println("=== SecretKeySpec ===");
byte[] keyBytes = new byte[16];
Arrays.fill(keyBytes, (byte) 0x42);
SecretKeySpec key = new SecretKeySpec(keyBytes, "AES");
assertEquals("AES", key.getAlgorithm());
assertEquals("RAW", key.getFormat());
assertArrayEquals(keyBytes, key.getEncoded());
System.out.println("SecretKeySpec 创建成功");
System.out.println("=== 测试通过 ===");
// 方法体结束
```
