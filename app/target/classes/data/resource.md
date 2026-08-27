---
name: Resource
package: javax.annotation
order: 383
---

## 介绍

`javax.annotation.Resource` 是**资源注入**注解，用于声明式注入资源（如 EJB、JMS 等）。

## 方法

### name / type / lookup / authenticationType / shareable / mappedName / description

## 测试

- 描述: Resource 注解
- 断言: 注解存在

```java
// 方法体开始
System.out.println("=== Resource ===");
assertNotNull(Resource.class);
assertTrue(Resource.class.isAnnotation());
System.out.println("Resource 注解: " + Resource.class);
System.out.println("=== 测试通过 ===");
// 方法体结束
```
