---
name: Remote
package: java.rmi
order: 379
---

## 介绍

`java.rmi.Remote` 是 **RMI 远程接口**的标记接口，所有远程对象都必须实现此接口。

## 方法

无方法（标记接口）。

## 测试

- 描述: Remote 接口
- 断言: 标记接口有效

```java
// 方法体开始
System.out.println("=== Remote ===");
assertTrue(Remote.class.isInterface());
System.out.println("Remote 是标记接口: " + Remote.class.isInterface());
System.out.println("=== 测试通过 ===");
// 方法体结束
```
