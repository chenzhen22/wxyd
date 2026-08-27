---
name: Permission
package: java.security
order: 315
---

## 介绍

`java.security.Permission` 是**权限抽象类**，是所有权限类的基类。

## 方法

构造方法：
```java
public Permission(String name)
```

### getName / getActions / implies

## 测试

- 描述: 创建权限
- 断言: 权限信息正确

```java
// 方法体开始
System.out.println("=== Permission ===");
Permission p = new BasicPermission("file.read") {};
assertEquals("file.read", p.getName());
assertEquals("", p.getActions());
System.out.println("权限: " + p.getName());
System.out.println("=== 测试通过 ===");
// 方法体结束
```
