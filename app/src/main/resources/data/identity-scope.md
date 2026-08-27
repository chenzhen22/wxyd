---
name: IdentityScope
package: java.security
order: 309
---

## 介绍

`java.security.IdentityScope` 是**身份范围**类，表示身份的作用域。

## 方法

### getSystemScope

获取系统级身份范围。

## 测试

- 描述: 获取系统身份范围
- 断言: 可能为 null

```java
// 方法体开始
System.out.println("=== IdentityScope ===");
IdentityScope scope = IdentityScope.getSystemScope();
if (scope != null) {
    System.out.println("系统 Scope: " + scope);
} else {
    System.out.println("系统 Scope 为 null（JDK 默认）");
}
System.out.println("=== 测试通过 ===");
// 方法体结束
```
