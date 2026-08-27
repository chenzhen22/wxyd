---
name: HttpsURLConnection
package: javax.net.ssl
order: 270
---

## 介绍

`javax.net.ssl.HttpsURLConnection` 是 **HTTPS 连接类**，支持 SSL/TLS 加密的 HTTP 连接，是 `HttpURLConnection` 的子类。

## 方法

### getDefaultHostnameVerifier / setDefaultHostnameVerifier

管理默认主机名验证器。

### getHostnameVerifier / setHostnameVerifier

管理主机名验证器。

### getSSLSocketFactory / setSSLSocketFactory

管理 SSL 套接字工厂。

## 测试

- 描述: 获取 HttpsURLConnection 默认设置
- 断言: 默认值不为 null

```java
// 方法体开始
System.out.println("=== HttpsURLConnection ===");
HostnameVerifier hv = HttpsURLConnection.getDefaultHostnameVerifier();
assertNotNull(hv);
SSLSocketFactory sf = HttpsURLConnection.getDefaultSSLSocketFactory();
assertNotNull(sf);
System.out.println("HttpsURLConnection 默认设置获取成功");
System.out.println("=== 测试通过 ===");
// 方法体结束
```
