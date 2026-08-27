---
name: EnumSet
package: java.util
order: 155
---

## 介绍

`java.util.EnumSet` 是**专为枚举类型优化的高性能 Set**，内部使用位向量实现。Java 8 中获得了 Collection 所有默认方法的支持。

EnumSet 的核心特点：
- **极高性能**：内部使用位运算，比 HashSet 快
- **紧凑存储**：每个枚举常量占 1 个 bit
- **有序遍历**：按枚举常量的声明顺序
- **不允许 null 元素**

## 方法

### noneOf / allOf / of / range

```java
public static <E extends Enum<E>> EnumSet<E> noneOf(Class<E> elementType)
public static <E extends Enum<E>> EnumSet<E> allOf(Class<E> elementType)
public static <E extends Enum<E>> EnumSet<E> of(E e)
public static <E extends Enum<E>> EnumSet<E> range(E from, E to)
```

创建 EnumSet。

### complementOf / copyOf

```java
public static <E extends Enum<E>> EnumSet<E> complementOf(EnumSet<E> s)
public static <E extends Enum<E>> EnumSet<E> copyOf(Collection<E> c)
```

补集/复制。

## 测试

### allOf / noneOf

- 描述: 创建包含全部/不包含枚举常量的集合
- 断言: 大小正确

```java
// 方法体开始
System.out.println("=== allOf/noneOf ===");
enum Color { RED, GREEN, BLUE }
EnumSet<Color> all = EnumSet.allOf(Color.class);
EnumSet<Color> none = EnumSet.noneOf(Color.class);
assertEquals(3, all.size());
assertEquals(0, none.size());
System.out.println("allOf: " + all + ", noneOf: " + none);
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### of / range

- 描述: 创建包含指定枚举常量的集合
- 断言: 包含关系正确

```java
// 方法体开始
System.out.println("=== of/range ===");
enum Size { XS, S, M, L, XL, XXL }
EnumSet<Size> two = EnumSet.of(Size.S, Size.L);
EnumSet<Size> range = EnumSet.range(Size.S, Size.L);
assertEquals(2, two.size());
assertEquals(3, range.size());  // S, M, L
System.out.println("of(S, L): " + two + ", range(S, L): " + range);
System.out.println("=== 测试通过 ===");
// 方法体结束
```
