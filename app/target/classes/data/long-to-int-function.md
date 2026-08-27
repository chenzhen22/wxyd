---
name: LongToIntFunction
package: java.util.function
order: 109
---

## 介绍

`LongToIntFunction` 是 Java 8 引入的一个**函数式接口**，代表一个接受 `long` 参数并返回 `int` 值的函数。用于 long → int 的转换。

## 方法

### applyAsInt

```java
int applyAsInt(long value)
```

对给定 long 值执行转换并返回 int。

## 测试

- 描述: long 到 int 的截断转换
- 断言: 正确转换

```java
// 方法体开始
System.out.println("=== LongToIntFunction ===");
LongToIntFunction truncate = v -> (int) v;
assertEquals(100, truncate.applyAsInt(100L));
assertEquals(Integer.MAX_VALUE, truncate.applyAsInt(Integer.MAX_VALUE));
System.out.println("(int)100L = " + truncate.applyAsInt(100L));
System.out.println("=== 测试通过 ===");
// 方法体结束
```
