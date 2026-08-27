---
name: BasicPermission
package: java.security
order: 307
---

## 介绍

`java.security.BasicPermission` 是**基础权限类**，命名权限的基类，扩展自 `Permission`。

## 方法

构造方法：
```java
public BasicPermission(String name)
public BasicPermission(String name, String actions)
```

### getActions / implies

## 测试

- 描述: 创建基本权限
- 断言: 权限名称正确

```java
// 方法体开始
System.out.println("=== BasicPermission ===");
Permission p = new BasicPermission("read") {};
assertEquals("read", p.getName());
assertEquals("", p.getActions());
System.out.println("权限: " + p.getName());
System.out.println("=== 测试通过 ===");
// 方法体结束
```
