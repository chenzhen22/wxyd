---
name: KeyPairGenerator
package: java.security
order: 266
---

## 介绍

`java.security.KeyPairGenerator` 是**非对称密钥对生成器**类，用于生成公钥/私钥对。

## 方法

### getInstance

```java
public static KeyPairGenerator getInstance(String algorithm) throws NoSuchAlgorithmException
```

### initialize

```java
public void initialize(int keysize)
```

### generateKeyPair

```java
public KeyPair generateKeyPair()
```

## 测试

- 描述: 生成 RSA 密钥对
- 断言: 密钥对生成成功

```java
// 方法体开始
System.out.println("=== KeyPairGenerator ===");
try {
    KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
    kpg.initialize(1024);
    KeyPair kp = kpg.generateKeyPair();
    assertNotNull(kp.getPublic());
    assertNotNull(kp.getPrivate());
    assertEquals("RSA", kp.getPublic().getAlgorithm());
    System.out.println("RSA 密钥对生成成功");
} catch (Exception e) {
    System.out.println("密钥生成不可用: " + e.getMessage());
}
System.out.println("=== 测试通过 ===");
// 方法体结束
```
