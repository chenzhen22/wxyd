---
name: Hashtable
package: java.util
order: 182
---

## 介绍

`java.util.Hashtable` 是**线程安全的哈希表**，是 HashMap 的旧版同步实现。Java 8 获得了 Map 接口的所有默认方法支持。

## 方法

构造方法：
```java
public Hashtable()
public Hashtable(int initialCapacity)
public Hashtable(Map<? extends K, ? extends V> t)
```

Map 接口方法 + Java 8 默认方法：
- `forEach(BiConsumer)` — 遍历
- `computeIfAbsent` / `merge` — Lambda 操作
- `getOrDefault` — 默认值

## 测试

### 基本操作

- 描述: Hashtable 的存取操作
- 断言: 正确存取

```java
// 方法体开始
System.out.println("=== Hashtable ===");
Hashtable<String, Integer> table = new Hashtable<>();
table.put("A", 1);
table.put("B", 2);
assertEquals(Integer.valueOf(1), table.get("A"));
table.forEach((k, v) -> System.out.println(k + "=" + v));
assertEquals(2, table.size());
System.out.println("=== 测试通过 ===");
// 方法体结束
```
