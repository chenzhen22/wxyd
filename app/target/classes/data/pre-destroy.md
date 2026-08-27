---
name: PreDestroy
package: javax.annotation
order: 382
---

## 介绍

`javax.annotation.PreDestroy` 是**销毁前回调**注解，标记在对象销毁前需要执行的方法。

## 方法

无方法（注解）。

## 测试

- 描述: PreDestroy 注解
- 断言: 注解存在

```java
// 方法体开始
System.out.println("=== PreDestroy ===");
assertNotNull(PreDestroy.class);
assertTrue(PreDestroy.class.isAnnotation());
System.out.println("PreDestroy 注解: " + PreDestroy.class);
System.out.println("=== 测试通过 ===");
// 方法体结束
```
