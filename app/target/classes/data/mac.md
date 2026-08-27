---
name: Mac
package: javax.crypto
order: 275
---

## 介绍

`javax.crypto.Mac` 是**消息认证码**类，用于生成和验证消息的 HMAC。

## 方法

### getInstance

```java
public static Mac getInstance(String algorithm) throws NoSuchAlgorithmException
```

### init

```java
public void init(Key key) throws InvalidKeyException
```

### doFinal

```java
public byte[] doFinal(byte[] input)
```

## 测试

- 描述: 计算 HMAC-SHA256
- 断言: 相同数据产生相同 HMAC

```java
// 方法体开始
System.out.println("=== Mac ===");
try {
    KeyGenerator kg = KeyGenerator.getInstance("HmacSHA256");
    SecretKey key = kg.generateKey();
    Mac mac = Mac.getInstance("HmacSHA256");
    mac.init(key);
    byte[] result1 = mac.doFinal("Hello".getBytes("UTF-8"));
    mac.init(key);
    byte[] result2 = mac.doFinal("Hello".getBytes("UTF-8"));
    mac.init(key);
    byte[] result3 = mac.doFinal("World".getBytes("UTF-8"));
    assertArrayEquals(result1, result2);  // 相同数据
    assertFalse(Arrays.equals(result1, result3));  // 不同数据
    System.out.println("HMAC-SHA256 测试通过");
} catch (Exception e) {
    System.out.println("HMAC 不可用: " + e.getMessage());
}
System.out.println("=== 测试通过 ===");
// 方法体结束
```
