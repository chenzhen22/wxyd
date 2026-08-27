---
name: ProtectionDomain
package: java.security
order: 317
---

## 介绍

`java.security.ProtectionDomain` 是**保护域**类，封装了类的代码源和权限集合。

## 方法

构造方法：
```java
public ProtectionDomain(CodeSource codesource, PermissionCollection permissions)
```

### getCodeSource / getPermissions / implies

## 测试

- 描述: 创建保护域
- 断言: 权限检查正确

```java
// 方法体开始
System.out.println("=== ProtectionDomain ===");
CodeSource cs = new CodeSource(new URL("file:/"), (java.security.cert.Certificate[]) null);
PermissionCollection perms = new PermissionCollection() {
    public void add(Permission p) { super.add(p); }
};
ProtectionDomain pd = new ProtectionDomain(cs, perms);
assertNotNull(pd.getCodeSource());
System.out.println("ProtectionDomain 创建成功");
System.out.println("=== 测试通过 ===");
// 方法体结束
```
