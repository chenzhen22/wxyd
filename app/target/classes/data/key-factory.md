---
name: KeyFactory
package: java.security
order: 319
---

## 介绍

`java.security.KeyFactory` 是**密钥工厂**类，用于在密钥和不透明的密钥规范之间转换。

## 方法

### getInstance

```java
public static KeyFactory getInstance(String algorithm) throws NoSuchAlgorithmException
```

### generatePublic / generatePrivate / getKeySpec / translateKey

## 测试

- 描述: 创建密钥工厂
- 断言: 工厂创建成功

```java
// 方法体开始
System.out.println("=== KeyFactory ===");
KeyFactory kf = KeyFactory.getInstance("RSA");
assertNotNull(kf);
assertEquals("RSA", kf.getAlgorithm());
System.out.println("KeyFactory(RSA) 创建成功");
System.out.println("=== 测试通过 ===");
// 方法体结束
```
