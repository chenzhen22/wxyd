---
name: Authenticator
package: java.net
order: 291
---

## 介绍

`java.net.Authenticator` 是**网络认证器**抽象类，用于在 HTTP 认证时提供用户名/密码。

## 方法

### setDefault

```java
public static void setDefault(Authenticator a)
```

设置默认认证器。

### getRequestingHost / getRequestingPort / getRequestingPrompt

获取请求认证的主机/端口/提示信息。

### getPasswordAuthentication

```java
protected PasswordAuthentication getPasswordAuthentication()
```

## 测试

- 描述: 设置自定义认证器
- 断言: 认证器设置成功

```java
// 方法体开始
System.out.println("=== Authenticator ===");
Authenticator.setDefault(new Authenticator() {
    protected PasswordAuthentication getPasswordAuthentication() {
        return new PasswordAuthentication("user", "pass".toCharArray());
    }
});
Authenticator auth = Authenticator.getDefault();
assertNotNull(auth);
System.out.println("Authenticator 设置成功");
System.out.println("=== 测试通过 ===");
// 方法体结束
```
