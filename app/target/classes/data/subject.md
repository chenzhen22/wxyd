---
name: Subject
package: javax.security.auth
order: 290
---

## 介绍

`javax.security.auth.Subject` 是**主体**类，代表一个实体（用户或服务）的安全信息，可以拥有多个 Principal 和凭据。

## 方法

构造方法：
```java
public Subject()
```

### getPrincipals

```java
public Set<Principal> getPrincipals()
```

### getPrivateCredentials / getPublicCredentials

获取私有/公开凭据。

### doAs

```java
public static <T> T doAs(Subject subject, PrivilegedAction<T> action)
```

以指定 Subject 的身份执行操作。

## 测试

- 描述: 创建 Subject 并添加身份
- 断言: 身份信息正确

```java
// 方法体开始
System.out.println("=== Subject ===");
Subject subject = new Subject();
Principal principal = () -> "testuser";
subject.getPrincipals().add(principal);
assertEquals(1, subject.getPrincipals().size());
assertEquals("testuser", subject.getPrincipals().iterator().next().getName());
System.out.println("Subject 身份: testuser");
System.out.println("=== 测试通过 ===");
// 方法体结束
```
