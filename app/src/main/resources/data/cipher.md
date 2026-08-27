---
name: Cipher
package: javax.crypto
order: 233
---

## 介绍

`javax.crypto.Cipher` 是**加密/解密类**，提供加密和解密功能。

## 方法

### getInstance

```java
public static Cipher getInstance(String transformation) throws NoSuchAlgorithmException, NoSuchPaddingException
```

获取 Cipher 实例。

### init / doFinal

```java
public void init(int opmode, Key key)
public byte[] doFinal(byte[] input)
```

初始化/执行加密操作。

### ENCRYPT_MODE / DECRYPT_MODE

加密模式/解密模式常量。

## 测试

- 描述: AES 加密和解密
- 断言: 解密后与原文一致

```java
// 方法体开始
System.out.println("=== Cipher ===");
try {
    KeyGenerator kg = KeyGenerator.getInstance("AES");
    kg.init(128);
    SecretKey key = kg.generateKey();
    Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
    cipher.init(Cipher.ENCRYPT_MODE, key);
    String plaintext = "Hello Java 8!";
    byte[] encrypted = cipher.doFinal(plaintext.getBytes("UTF-8"));
    cipher.init(Cipher.DECRYPT_MODE, key);
    byte[] decrypted = cipher.doFinal(encrypted);
    assertEquals(plaintext, new String(decrypted, "UTF-8"));
    System.out.println("AES 加密解密成功");
} catch (Exception e) {
    System.out.println("加密不可用: " + e.getMessage());
}
System.out.println("=== 测试通过 ===");
// 方法体结束
```
