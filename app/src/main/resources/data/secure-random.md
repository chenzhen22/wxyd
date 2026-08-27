---
name: SecureRandom
package: java.security
order: 217
---

## 介绍

`java.security.SecureRandom` 是**加密安全的随机数生成器**，用于生成高质量的伪随机数，适合加密、令牌生成等安全场景。

## 方法

构造方法：
```java
public SecureRandom()
```

### nextBytes / nextInt / nextLong / generateSeed

生成随机字节/整数/长整数/种子。

## 测试

- 描述: 生成安全随机数
- 断言: 生成结果在范围内

```java
// 方法体开始
System.out.println("=== SecureRandom ===");
SecureRandom sr = new SecureRandom();
byte[] bytes = new byte[16];
sr.nextBytes(bytes);
assertEquals(16, bytes.length);
int randomInt = sr.nextInt(100);
assertTrue(randomInt >= 0 && randomInt < 100);
long seed = sr.generateSeed(8);
assertEquals(8, seed.length);
System.out.println("SecureRandom 随机数: " + randomInt);
System.out.println("=== 测试通过 ===");
// 方法体结束
```
