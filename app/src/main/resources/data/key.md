---
name: Key
package: java.security
order: 318
---

## 介绍

`java.security.Key` 是**所有密钥的顶层接口**，定义了所有密钥的共同方法。

## 方法

### getAlgorithm / getEncoded / getFormat

## 测试

- 描述: 获取密钥信息
- 断言: 信息正确

```java
// 方法体开始
System.out.println("=== Key ===");
KeyGenerator kg = KeyGenerator.getInstance("HmacSHA256");
Key key = kg.generateKey();
assertEquals("HmacSHA256", key.getAlgorithm());
assertEquals("RAW", key.getFormat());
assertNotNull(key.getEncoded());
System.out.println("密钥算法: " + key.getAlgorithm() + ", 格式: " + key.getFormat());
System.out.println("=== 测试通过 ===");
// 方法体结束
```
