---
name: Proxy
package: java.lang.reflect
order: 245
---

## 介绍

`java.lang.reflect.Proxy` 是**动态代理类**，在运行时创建实现指定接口列表的代理类。

## 方法

### newProxyInstance

```java
public static Object newProxyInstance(ClassLoader loader, Class<?>[] interfaces, InvocationHandler h)
```

创建动态代理实例。

### getInvocationHandler / getProxyClass

获取代理的 InvocationHandler / 获取代理类。

## 测试

- 描述: 创建动态代理
- 断言: 代理方法被拦截

```java
// 方法体开始
System.out.println("=== Proxy ===");
Map<String, Object> proxy = (Map<String, Object>) Proxy.newProxyInstance(
        Map.class.getClassLoader(),
        new Class[]{Map.class},
        (obj, method, args1) -> {
            if (method.getName().equals("get")) {
                return "proxied:" + args1[0];
            }
            return null;
        });
assertEquals("proxied:hello", proxy.get("hello"));
System.out.println("动态代理: " + proxy.get("test"));
System.out.println("=== 测试通过 ===");
// 方法体结束
```
