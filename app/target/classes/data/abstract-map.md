---
name: AbstractMap
package: java.util
order: 168
---

## 介绍

`java.util.AbstractMap` 是 Map 接口的骨架实现。Java 8 新增了内部类 `SimpleEntry` 和 `SimpleImmutableEntry`，用于创建键值对条目。

`AbstractMap.SimpleEntry<K,V>` — 可变的键值对条目
`AbstractMap.SimpleImmutableEntry<K,V>` — 不可变的键值对条目

## 方法

### SimpleEntry 构造

```java
public SimpleEntry(K key, V value)
public SimpleEntry(Map.Entry<? extends K, ? extends V> entry)
```

### getKey / getValue / setValue

```java
public K getKey()
public V getValue()
public V setValue(V value)
```

## 测试

### SimpleEntry

- 描述: 创建和使用 Map.Entry
- 断言: 键值对操作正确

```java
// 方法体开始
System.out.println("=== SimpleEntry ===");
Map.Entry<String, Integer> entry = new AbstractMap.SimpleEntry<>("age", 25);
assertEquals("age", entry.getKey());
assertEquals(Integer.valueOf(25), entry.getValue());
entry.setValue(26);
assertEquals(Integer.valueOf(26), entry.getValue());
System.out.println("entry: " + entry);
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### SimpleImmutableEntry

- 描述: 不可变 Map.Entry
- 断言: 不可变性

```java
// 方法体开始
System.out.println("=== SimpleImmutableEntry ===");
Map.Entry<String, Integer> entry = new AbstractMap.SimpleImmutableEntry<>("fixed", 42);
assertEquals("fixed", entry.getKey());
assertEquals(Integer.valueOf(42), entry.getValue());
boolean thrown = false;
try {
    entry.setValue(100);
} catch (UnsupportedOperationException e) {
    thrown = true;
}
assertTrue(thrown);
System.out.println("不可变 entry: " + entry);
System.out.println("=== 测试通过 ===");
// 方法体结束
```
