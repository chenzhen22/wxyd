---
name: Vector
package: java.util
order: 180
---

## 介绍

`java.util.Vector` 是**线程安全的动态数组**，是 ArrayList 的旧版同步实现。Java 8 获得了 Collection 接口的所有默认方法支持。

## 方法

构造方法：
```java
public Vector()
public Vector(int initialCapacity)
public Vector(Collection<? extends E> c)
```

特有方法（古老但仍在用）：
- `addElement(E)` — 添加元素
- `elementAt(int)` — 获取元素
- `elements()` — 获取 Enumeration
- `capacity()` — 获取容量

Java 8 方法：`forEach`、`removeIf`、`stream`、`spliterator`

## 测试

### 基本操作

- 描述: Vector 的基本操作
- 断言: 同步动态数组正常

```java
// 方法体开始
System.out.println("=== Vector ===");
Vector<String> vector = new Vector<>();
vector.addElement("A");
vector.addElement("B");
assertEquals("A", vector.elementAt(0));
assertEquals(2, vector.size());
vector.forEach(s -> System.out.println("  " + s));
System.out.println("Vector 遍历完成");
System.out.println("=== 测试通过 ===");
// 方法体结束
```
