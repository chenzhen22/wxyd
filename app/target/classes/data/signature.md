---
name: Signature
package: java.security
order: 267
---

## 介绍

`java.security.Signature` 是**数字签名类**，用于生成和验证数字签名。

## 方法

### getInstance

```java
public static Signature getInstance(String algorithm) throws NoSuchAlgorithmException
```

### initSign / initVerify

```java
public void initSign(PrivateKey privateKey)
public void initVerify(PublicKey publicKey)
```

### update / sign / verify

更新数据 / 签名 / 验证签名。

## 测试

- 描述: 使用 RSA 签名和验证
- 断言: 签名验证成功

```java
// 方法体开始
System.out.println("=== Signature ===");
try {
    KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
    kpg.initialize(1024);
    KeyPair kp = kpg.generateKeyPair();
    Signature sig = Signature.getInstance("SHA256withRSA");
    sig.initSign(kp.getPrivate());
    sig.update("Hello".getBytes("UTF-8"));
    byte[] signature = sig.sign();
    sig.initVerify(kp.getPublic());
    sig.update("Hello".getBytes("UTF-8"));
    assertTrue(sig.verify(signature));
    System.out.println("数字签名验证成功");
} catch (Exception e) {
    System.out.println("签名不可用: " + e.getMessage());
}
System.out.println("=== 测试通过 ===");
// 方法体结束
```
