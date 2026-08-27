---
name: Provider
package: java.security
order: 310
---

## 介绍

`java.security.Provider` 是**安全提供者**类，实现特定安全服务的引擎。

## 方法

构造方法：
```java
protected Provider(String name, double version, String info)
```

### getName / getVersion / getInfo

### put / get / remove

设置/获取/移除属性。

## 测试

- 描述: 查看已安装的 Provider
- 断言: 至少有一个 Provider

```java
// 方法体开始
System.out.println("=== Provider ===");
Provider[] providers = Security.getProviders();
assertTrue(providers.length > 0);
Provider first = providers[0];
assertNotNull(first.getName());
System.out.println("Provider: " + first.getName() + " v" + first.getVersion());
System.out.println("  信息: " + first.getInfo());
System.out.println("=== 测试通过 ===");
// 方法体结束
```
