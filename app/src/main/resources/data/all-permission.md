---
name: AllPermission
package: java.security
order: 312
---

## 介绍

`java.security.AllPermission` 是**所有权限类**，表示所有权限。

## 方法

构造方法：
```java
public AllPermission()
public AllPermission(String name, String actions)
```

## 测试

- 描述: 创建 AllPermission
- 断言: 创建成功

```java
// 方法体开始
System.out.println("=== AllPermission ===");
AllPermission ap = new AllPermission();
assertEquals("<all permissions>", ap.getName());
System.out.println("AllPermission: " + ap.getName());
System.out.println("=== 测试通过 ===");
// 方法体结束
```
