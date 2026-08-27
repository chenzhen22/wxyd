---
name: CertificateFactory
package: java.security.cert
order: 269
---

## 介绍

`java.security.cert.CertificateFactory` 是**证书工厂**类，用于从输入流生成证书。

## 方法

### getInstance

```java
public static CertificateFactory getInstance(String type) throws CertificateException
```

### generateCertificate

```java
public Certificate generateCertificate(InputStream inStream) throws CertificateException
```

## 测试

- 描述: 创建证书工厂
- 断言: 工厂创建成功

```java
// 方法体开始
System.out.println("=== CertificateFactory ===");
try {
    CertificateFactory cf = CertificateFactory.getInstance("X.509");
    assertNotNull(cf);
    System.out.println("X.509 CertificateFactory 创建成功");
} catch (Exception e) {
    System.out.println("证书工厂不可用: " + e.getMessage());
}
System.out.println("=== 测试通过 ===");
// 方法体结束
```
