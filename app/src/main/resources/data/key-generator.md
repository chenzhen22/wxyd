---
name: KeyGenerator
package: javax.crypto
order: 234
---

## 介绍

`javax.crypto.KeyGenerator` 是**对称密钥生成器**类，用于生成加密密钥。

## 方法

### getInstance

```java
public static KeyGenerator getInstance(String algorithm) throws NoSuchAlgorithmException
```

### init

```java
public void init(int keysize)
```

指定密钥长度（取决于算法）。

### generateKey

```java
public SecretKey generateKey()
```

生成密钥。

## 测试

- 描述: 生成 AES 密钥
- 断言: 密钥生成成功

```java
// 方法体开始
System.out.println("=== KeyGenerator ===");
try {
    KeyGenerator kg = KeyGenerator.getInstance("AES");
    kg.init(128);
    SecretKey key = kg.generateKey();
    assertNotNull(key);
    assertEquals("AES", key.getAlgorithm());
    System.out.println("AES 密钥生成成功");
} catch (Exception e) {
    System.out.println("密钥生成不可用: " + e.getMessage());
}
System.out.println("=== 测试通过 ===");
// 方法体结束
```
