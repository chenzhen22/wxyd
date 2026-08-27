---
name: EnumMap
package: java.util
order: 154
---

## 介绍

`java.util.EnumMap` 是**专为枚举键优化的高性能 Map**，内部使用数组实现，比 HashMap 更快。Java 8 中获得了 Map 所有默认方法的支持。

EnumMap 的核心特点：
- **高性能**：内部用数组，无需哈希计算
- **有序遍历**：按枚举常量的声明顺序遍历
- **不允许 null 键**：但允许 null 值
- **与 EnumSet 配对的 Map 版本**

## 方法

构造方法：
```java
public EnumMap(Class<K> keyType)
public EnumMap(EnumMap<K, ? extends V> m)
public EnumMap(Map<K, ? extends V> m)
```

Map 标准方法 + Java 8 默认方法：
- `put(K, V)` / `get(Object)` — 存取
- `forEach(BiConsumer)` — 遍历（按枚举顺序）
- `computeIfAbsent` / `merge` — Java 8 方法

## 测试

### 基本操作

- 描述: EnumMap 存取操作
- 断言: 操作正确且按枚举声明顺序

```java
// 方法体开始
System.out.println("=== EnumMap ===");
enum Day { MON, TUE, WED, THU, FRI }
EnumMap<Day, Integer> map = new EnumMap<>(Day.class);
map.put(Day.MON, 1);
map.put(Day.FRI, 5);
map.put(Day.WED, 3);
assertEquals(Integer.valueOf(1), map.get(Day.MON));
// 按枚举顺序遍历
List<Day> keys = new ArrayList<>();
map.forEach((k, v) -> keys.add(k));
assertEquals(Day.MON, keys.get(0));
assertEquals(Day.WED, keys.get(1));
assertEquals(Day.FRI, keys.get(2));
System.out.println("EnumMap: " + map);
System.out.println("=== 测试通过 ===");
// 方法体结束
```
