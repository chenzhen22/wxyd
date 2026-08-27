---
name: Comparable
package: java.lang
order: 235
---

## 介绍

`java.lang.Comparable` 是**自然排序接口**。Java 8 中它是 `@FunctionalInterface`，虽然它只有一个抽象方法，但函数式风格主要用在配合 Comparator 使用。

## 方法

### compareTo

```java
public int compareTo(T o)
```

比较此对象与指定对象的顺序。返回负数（小于）、零（等于）、正数（大于）。

## 测试

- 描述: 使用 Comparable 比较对象
- 断言: 比较结果正确

```java
// 方法体开始
System.out.println("=== Comparable ===");
assertEquals(-1, Integer.valueOf(3).compareTo(5));
assertEquals(0, Integer.valueOf(5).compareTo(5));
assertEquals(1, Integer.valueOf(7).compareTo(5));
String a = "apple", b = "banana";
assertTrue(a.compareTo(b) < 0);
System.out.println("3 < 5: true, 5 == 5: true, 7 > 5: true");
System.out.println("=== 测试通过 ===");
// 方法体结束
```
