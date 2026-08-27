---
name: PermissionCollection
package: java.security
order: 313
---

## 介绍

`java.security.PermissionCollection` 是**权限集合**类，用于存储一组权限对象。

## 方法

### add / elements / implies

添加/遍历/检查权限。

## 测试

- 描述: 创建权限集合
- 断言: 权限检查正确

```java
// 方法体开始
System.out.println("=== PermissionCollection ===");
PermissionCollection pc = new PermissionCollection() {
    public void add(Permission p) { super.add(p); }
};
Permission read = new BasicPermission("read") {};
pc.add(read);
assertTrue(pc.implies(new BasicPermission("read") {}));
System.out.println("权限集合测试完成");
System.out.println("=== 测试通过 ===");
// 方法体结束
```
