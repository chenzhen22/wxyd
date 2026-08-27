---
name: PostConstruct
package: javax.annotation
order: 381
---

## 介绍

`javax.annotation.PostConstruct` 是**初始化后回调**注解，标记在依赖注入完成后需要执行的方法。

## 方法

无方法（注解）。

## 测试

- 描述: PostConstruct 注解
- 断言: 注解存在

```java
// 方法体开始
System.out.println("=== PostConstruct ===");
assertNotNull(PostConstruct.class);
assertTrue(PostConstruct.class.isAnnotation());
System.out.println("PostConstruct 注解: " + PostConstruct.class);
// 注解在 Spring 中被广泛使用，如 @PostConstruct init() 方法
System.out.println("=== 测试通过 ===");
// 方法体结束
```
