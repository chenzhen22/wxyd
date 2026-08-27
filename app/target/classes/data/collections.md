---
name: Collections
package: java.util
order: 167
---

## 介绍

`java.util.Collections` 是**集合工具类**。Java 8 新增了多个返回空 Stream 和适配器的方法。

Java 8 新增/增强的方法：
- `unmodifiableNavigableMap` / `unmodifiableNavigableSet` — 不可变视图
- `synchronizedNavigableMap` / `synchronizedNavigableSet` — 同步视图
- `newSetFromMap(Map)` — 从 Map 创建 Set 视图

## 方法

### newSetFromMap

```java
public static <E> Set<E> newSetFromMap(Map<E, Boolean> map)
```

从指定 Map 创建 Set 视图。当 Map 为空时创建空 Set。

### emptySortedSet / emptyNavigableSet

Java 8 新增的空有序集合工厂方法。

## 测试

### newSetFromMap

- 描述: 从 ConcurrentHashMap 创建并发 Set
- 断言: Set 操作反映到 Map

```java
// 方法体开始
System.out.println("=== newSetFromMap ===");
Set<String> set = Collections.newSetFromMap(new ConcurrentHashMap<>());
set.add("A");
set.add("B");
assertTrue(set.contains("A"));
assertEquals(2, set.size());
set.remove("A");
assertFalse(set.contains("A"));
System.out.println("并发 Set: " + set);
System.out.println("=== 测试通过 ===");
// 方法体结束
```
