---
name: AtomicReferenceArray
package: java.util.concurrent.atomic
order: 162
---

## 介绍

`java.util.concurrent.atomic.AtomicReferenceArray<V>` 是**原子操作引用类型数组**的类。Java 8 为其新增了 `getAndUpdate`、`updateAndGet`、`getAndAccumulate`、`accumulateAndGet` 等 Lambda 友好的方法。

## 方法

### updateAndGet

```java
public final V updateAndGet(int i, UnaryOperator<V> updateFunction)
```

对指定索引的元素执行更新操作并返回新值。

## 测试

### updateAndGet

- 描述: 使用 updateAndGet 原子更新引用数组
- 断言: 字符串转换正确

```java
// 方法体开始
System.out.println("=== AtomicReferenceArray ===");
AtomicReferenceArray<String> arr = new AtomicReferenceArray<>(new String[]{"hello", "world", "java"});
String v = arr.updateAndGet(0, s -> s.toUpperCase());
assertEquals("HELLO", v);
assertEquals("HELLO", arr.get(0));
assertEquals("world", arr.get(1));
System.out.println("arr[0] 大写: " + arr.get(0));
System.out.println("=== 测试通过 ===");
// 方法体结束
```
