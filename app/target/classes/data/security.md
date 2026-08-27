---
name: Security
package: java.security
order: 311
---

## 介绍

`java.security.Security` 是**安全管理器**类，管理已安装的安全提供者和安全属性。

## 方法

### getProviders

```java
public static Provider[] getProviders()
```

### getProvider / addProvider / removeProvider

### getProperty / setProperty

## 测试

- 描述: 获取安全属性
- 断言: 属性不为空

```java
// 方法体开始
System.out.println("=== Security ===");
Provider[] providers = Security.getProviders();
assertTrue(providers.length > 0);
System.out.println("已安装 Provider 数: " + providers.length);
for (Provider p : providers) {
    System.out.println("  - " + p.getName());
}
System.out.println("=== 测试通过 ===");
// 方法体结束
```
