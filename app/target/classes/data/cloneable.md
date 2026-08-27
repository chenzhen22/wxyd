---
name: Cloneable
package: java.lang
order: 325
---

## 介绍

`java.lang.Cloneable` 是**可克隆标记接口**，指示允许克隆对象。

## 方法

无方法（标记接口）。

## 测试

- 描述: 检查 Cloneable
- 断言: 检查结果正确

```java
// 方法体开始
System.out.println("=== Cloneable ===");
assertTrue(int[].class.isArray());
ArrayList<String> list = new ArrayList<>();
assertFalse(list instanceof Cloneable);  // ArrayList 实现了 Cloneable
// int[] 数组实现了 Cloneable
int[] arr = {1, 2, 3};
assertTrue(arr instanceof Cloneable);
int[] clone = arr.clone();
assertArrayEquals(arr, clone);
System.out.println("Cloneable 测试完成");
System.out.println("=== 测试通过 ===");
// 方法体结束
```
