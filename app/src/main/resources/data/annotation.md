---
name: Annotation
package: java.lang.annotation
order: 282
---

## 介绍

`java.lang.annotation.Annotation` 是所有注解的**根接口**。Java 8 新增了重复注解（`@Repeatable`）和类型注解（`@Target(TYPE_PARAMETER/ TYPE_USE)`）的支持。

## 方法

### annotationType

```java
public Class<? extends Annotation> annotationType()
```

返回此注解的类型。

### equals / hashCode / toString

注解比较和字符串表示。

## 测试

- 描述: 获取注解信息
- 断言: 注解类型正确

```java
// 方法体开始
System.out.println("=== Annotation ===");
Override annotation = Override.class.getAnnotation(Override.class);
assertNull(annotation);  // Override 在 Override 类本身上没有定义
Deprecated deprecated = Deprecated.class.getAnnotation(Deprecated.class);
assertNotNull(deprecated);
assertEquals("java.lang.Deprecated", deprecated.annotationType().getName());
System.out.println("Deprecated 注解类型: " + deprecated.annotationType().getName());
System.out.println("=== 测试通过 ===");
// 方法体结束
```
