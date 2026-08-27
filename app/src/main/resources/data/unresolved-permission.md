---
name: UnresolvedPermission
package: java.security
order: 308
---

## 介绍

`java.security.UnresolvedPermission` 是**未解析权限**类，用于保存尚未加载的权限类信息。

## 方法

构造方法：
```java
public UnresolvedPermission(String type, String name, String actions, java.security.cert.Certificate[] certs)
```

### getUnresolvedType / getUnresolvedName / getUnresolvedActions

## 测试

- 描述: 创建未解析权限
- 断言: 信息正确

```java
// 方法体开始
System.out.println("=== UnresolvedPermission ===");
UnresolvedPermission up = new UnresolvedPermission("java.io.FilePermission", "/tmp/-", "read,write", null);
assertEquals("java.io.FilePermission", up.getUnresolvedType());
assertEquals("/tmp/-", up.getUnresolvedName());
assertEquals("read,write", up.getUnresolvedActions());
System.out.println("未解析权限: " + up.getUnresolvedType());
System.out.println("=== 测试通过 ===");
// 方法体结束
```
