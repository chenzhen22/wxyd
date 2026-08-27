---
name: CodeSource
package: java.security
order: 314
---

## 介绍

`java.security.CodeSource` 是**代码源**类，封装了代码的 URL 位置和签名证书。

## 方法

构造方法：
```java
public CodeSource(URL url, java.security.cert.Certificate[] certs)
```

### getLocation / getCertificates / implies

## 测试

- 描述: 创建 CodeSource
- 断言: 位置正确

```java
// 方法体开始
System.out.println("=== CodeSource ===");
CodeSource cs = new CodeSource(new URL("file:/"), (java.security.cert.Certificate[]) null);
assertEquals(new URL("file:/"), cs.getLocation());
System.out.println("CodeSource: " + cs.getLocation());
System.out.println("=== 测试通过 ===");
// 方法体结束
```
