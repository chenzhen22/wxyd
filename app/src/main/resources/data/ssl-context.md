---
name: SSLContext
package: javax.net.ssl
order: 273
---

## 介绍

`javax.net.ssl.SSLContext` 是 **SSL/TLS 上下文**类，管理 SSL 连接的协议、密钥和信任管理器。

## 方法

### getInstance

```java
public static SSLContext getInstance(String protocol) throws NoSuchAlgorithmException
```

### init

```java
public void init(KeyManager[] km, TrustManager[] tm, SecureRandom random) throws KeyManagementException
```

### getSocketFactory

```java
public SSLSocketFactory getSocketFactory()
```

## 测试

- 描述: 创建 SSLContext
- 断言: 上下文创建成功

```java
// 方法体开始
System.out.println("=== SSLContext ===");
SSLContext ctx = SSLContext.getInstance("TLS");
ctx.init(null, null, null);
SSLSocketFactory sf = ctx.getSocketFactory();
assertNotNull(sf);
System.out.println("SSLContext(TLS) 创建成功");
System.out.println("=== 测试通过 ===");
// 方法体结束
```
