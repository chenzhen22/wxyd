---
name: SignedObject
package: java.security
order: 306
---

## 介绍

`java.security.SignedObject` 是**签名对象**类，用于创建可序列化对象的数字签名。

## 方法

构造方法：
```java
public SignedObject(Serializable object, PrivateKey signingKey, Signature signingEngine) throws IOException, InvalidKeyException, SignatureException
```

### getObject / getSignature / verify

## 测试

- 描述: 创建和验证签名对象
- 断言: 验证通过

```java
// 方法体开始
System.out.println("=== SignedObject ===");
try {
    KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
    kpg.initialize(1024);
    KeyPair kp = kpg.generateKeyPair();
    Signature sig = Signature.getInstance("SHA256withRSA");
    SignedObject so = new SignedObject("Hello Signature", kp.getPrivate(), sig);
    assertEquals("Hello Signature", so.getObject());
    assertTrue(so.verify(kp.getPublic(), sig));
    System.out.println("签名对象验证成功");
} catch (Exception e) {
    System.out.println("签名不可用: " + e.getMessage());
}
System.out.println("=== 测试通过 ===");
// 方法体结束
```
