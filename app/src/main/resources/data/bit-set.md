---
name: BitSet
package: java.util
order: 122
---

## 介绍

`java.util.BitSet` 是 Java 中的**位向量**类。Java 8 为 BitSet 新增了 Stream 支持，可以通过 `stream()` 方法将所有设置为 1 的位作为 IntStream 返回。

Java 8 新增的方法：
- `stream()` — 返回所有 set 位的索引组成的 IntStream

## 方法

### stream

```java
public IntStream stream()
```

返回一个 IntStream，包含所有设置为 1 的位的索引。

- **返回**: `IntStream` — set 位索引的流

## 测试

### stream

- 描述: 使用 `stream` 获取所有 set 位的索引
- 断言: 索引正确

```java
// 方法体开始
System.out.println("=== stream ===");
BitSet bits = new BitSet();
bits.set(0);
bits.set(2);
bits.set(5);
bits.set(10);
int[] setBits = bits.stream().toArray();
assertArrayEquals(new int[]{0, 2, 5, 10}, setBits);
System.out.println("set 位索引: " + java.util.Arrays.toString(setBits));
System.out.println("=== 测试通过 ===");
// 方法体结束
```
