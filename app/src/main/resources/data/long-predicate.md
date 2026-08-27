---
name: LongPredicate
package: java.util.function
order: 84
---

## 介绍

`LongPredicate` 是 Java 8 引入的一个**函数式接口**，代表一个接受 `long` 参数并返回 `boolean` 的条件判断。它是 `Predicate<Long>` 的原始 long 特化版本。

LongPredicate 的四个方法：
- `test(long value)` — 核心判断方法
- `and(LongPredicate)` — 且条件组合
- `or(LongPredicate)` — 或条件组合
- `negate()` — 取反

## 方法

### test

```java
boolean test(long value)
```

对给定 long 值执行条件判断。

- **返回**: `boolean` — 判断结果

### and / or / negate

与 IntPredicate 相同的链式组合方法。

## 测试

### test

- 描述: 判断 long 值是否在指定范围内
- 断言: 100L 在 0-1000 范围内

```java
// 方法体开始
System.out.println("=== test ===");
LongPredicate inRange = v -> v >= 0L && v <= 1000L;
assertTrue(inRange.test(100L));
assertTrue(inRange.test(0L));
assertFalse(inRange.test(-1L));
assertFalse(inRange.test(1001L));
System.out.println("100 在范围内: " + inRange.test(100L));
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### and / or

- 描述: 组合条件：正数且为偶数
- 断言: 10L 是正偶数

```java
// 方法体开始
System.out.println("=== and ===");
LongPredicate isPositive = v -> v > 0;
LongPredicate isEven = v -> v % 2 == 0;
LongPredicate positiveAndEven = isPositive.and(isEven);
assertTrue(positiveAndEven.test(10L));
assertFalse(positiveAndEven.test(-2L));
System.out.println("10 是正偶数: " + positiveAndEven.test(10L));
System.out.println("=== 测试通过 ===");
// 方法体结束
```
