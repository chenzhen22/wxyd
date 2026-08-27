---
name: Void
package: java.lang
order: 334
---

## 介绍

`java.lang.Void` 是 **Void 类**，是一个不可实例化的占位符类，用于表示 `void` 返回类型。

## 方法

无。

## 测试

- 描述: Void 类型
- 断言: TYPE 正确

```java
// 方法体开始
System.out.println("=== Void ===");
assertEquals(void.class, Void.TYPE);
assertNotNull(Void.class);
System.out.println("Void.TYPE == void.class: " + (Void.TYPE == void.class));
System.out.println("=== 测试通过 ===");
// 方法体结束
```
