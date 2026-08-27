---
name: KeyPair
package: java.security
order: 320
---

## 介绍

`java.security.KeyPair` 是**密钥对**类，持有公钥和私钥。

## 方法

构造方法：
```java
public KeyPair(PublicKey publicKey, PrivateKey privateKey)
```

### getPublic / getPrivate

## 测试

- 描述: 创建和获取密钥对
- 断言: 密钥对正确

```java
// 方法体开始
System.out.println("=== KeyPair ===");
KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
kpg.initialize(1024);
KeyPair kp = kpg.generateKeyPair();
assertNotNull(kp.getPublic());
assertNotNull(kp.getPrivate());
assertEquals("RSA", kp.getPublic().getAlgorithm());
System.out.println("RSA KeyPair 生成成功");
System.out.println("=== 测试通过 ===");
// 方法体结束
```
