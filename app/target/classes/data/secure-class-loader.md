---
name: SecureClassLoader
package: java.security
order: 316
---

## 介绍

`java.security.SecureClassLoader` 是**安全类加载器**，扩展了 `ClassLoader`，添加了代码源和权限支持。

## 方法

构造方法：
```java
protected SecureClassLoader()
protected SecureClassLoader(ClassLoader parent)
```

## 测试

- 描述: 创建 SecureClassLoader
- 断言: 创建成功

```java
// 方法体开始
System.out.println("=== SecureClassLoader ===");
SecureClassLoader scl = new SecureClassLoader() {};
assertNotNull(scl);
assertNotNull(scl.getParent());
System.out.println("SecureClassLoader 创建成功");
System.out.println("=== 测试通过 ===");
// 方法体结束
```
