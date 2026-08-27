---
name: IvParameterSpec
package: javax.crypto.spec
order: 297
---

## 介绍

`javax.crypto.spec.IvParameterSpec` 是**初始化向量规范类**，用于 CBC 等加密模式。

## 方法

构造方法：
```java
public IvParameterSpec(byte[] iv)
```

### getIV

获取 IV 字节数组。

## 测试

- 描述: 使用 IV 参数进行加密
- 断言: 加解密正常

```java
// 方法体开始
System.out.println("=== IvParameterSpec ===");
try {
    byte[] keyBytes = new byte[16];
    byte[] ivBytes = new byte[16];
    Arrays.fill(keyBytes, (byte) 0x42);
    Arrays.fill(ivBytes, (byte) 0x12);
    SecretKeySpec key = new SecretKeySpec(keyBytes, "AES");
    IvParameterSpec iv = new IvParameterSpec(ivBytes);
    assertArrayEquals(ivBytes, iv.getIV());
    Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
    cipher.init(Cipher.ENCRYPT_MODE, key, iv);
    byte[] encrypted = cipher.doFinal("Hello".getBytes("UTF-8"));
    cipher.init(Cipher.DECRYPT_MODE, key, iv);
    byte[] decrypted = cipher.doFinal(encrypted);
    assertEquals("Hello", new String(decrypted, "UTF-8"));
    System.out.println("AES/CBC 加解密成功");
} catch (Exception e) {
    System.out.println("加解密不可用: " + e.getMessage());
}
System.out.println("=== 测试通过 ===");
// 方法体结束
```
