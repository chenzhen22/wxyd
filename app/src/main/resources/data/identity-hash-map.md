---
name: IdentityHashMap
package: java.util
order: 142
---

## 介绍

`java.util.IdentityHashMap` 是一个**使用引用相等性（==）而非 equals() 比较键**的特殊 Map。Java 8 为其新增了所有 Map 接口的默认方法（`computeIfAbsent`、`forEach`、`replaceAll`、`merge` 等）。

IdentityHashMap 的核心特点：
- **引用相等**：键比较使用 `==` 而非 `equals()`
- **用途特殊**：序列化/深拷贝、代理对象、实例计数
- **线性探测**：内部使用数组 + 线性探测法
- **线程不安全**：需要外部同步

## 方法

构造方法：
```java
public IdentityHashMap()
public IdentityHashMap(int expectedMaxSize)
public IdentityHashMap(Map<? extends K, ? extends V> m)
```

Map 标准方法 + Java 8 默认方法：
- `put(K, V)` / `get(Object)` — 插入/获取（使用 == 比较键）
- `computeIfAbsent(K, Function)` — 键缺失时计算
- `forEach(BiConsumer)` — 遍历
- `size()` / `keySet()` / `values()` — 视图

## 测试

### 引用相等性

- 描述: IdentityHashMap 使用 == 比较键
- 断言: 不同对象即使内容相同也视为不同键

```java
// 方法体开始
System.out.println("=== 引用相等 ===");
IdentityHashMap<String, Integer> map = new IdentityHashMap<>();
String a1 = "hello";
String a2 = new String("hello");
map.put(a1, 1);
map.put(a2, 2);
assertEquals(2, map.size());  // 两个不同的 String 对象
assertEquals(Integer.valueOf(1), map.get(a1));
assertEquals(Integer.valueOf(2), map.get(a2));
System.out.println("a1 -> " + map.get(a1) + ", a2 -> " + map.get(a2));
System.out.println("=== 测试通过 ===");
// 方法体结束
```
