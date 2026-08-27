---
name: KeyStore
package: java.security
order: 321
---

## 介绍

`java.security.KeyStore` 是**密钥库**类，用于存储密钥和证书条目。

## 方法

### getInstance / load / store

### setKeyEntry / setCertificateEntry / getKey

### aliases / containsAlias / size

## 测试

- 描述: 使用密钥库
- 断言: 创建成功

```java
// 方法体开始
System.out.println("=== KeyStore ===");
KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
ks.load(null, null);
assertEquals("PKCS12", KeyStore.getDefaultType());
System.out.println("KeyStore 创建成功，类型: " + KeyStore.getDefaultType());
System.out.println("=== 测试通过 ===");
// 方法体结束
```
