---
name: OptionalLong
package: java.util
order: 47
---

## 介绍

`java.util.OptionalLong` 是 Java 8 引入的 **long 类型的 Optional 容器**，与 OptionalInt 类似但针对 long 值。

## 方法

### empty

```java
public static OptionalLong empty()
```

返回空的 OptionalLong。

### of

```java
public static OptionalLong of(long value)
```

包含指定 long 值的 OptionalLong。

### isPresent

```java
public boolean isPresent()
```

判断值是否存在。

### getAsLong

```java
public long getAsLong()
```

获取值，空时抛出异常。

### orElse

```java
public long orElse(long other)
```

值存在返回值，否则返回默认值。

### toString

```java
public String toString()
```

字符串表示。

## 测试

### empty

- 描述: 创建空 OptionalLong
- 断言: isPresent 为 false

```java
// 方法体开始
System.out.println("=== empty ===");
OptionalLong opt = OptionalLong.empty();
assertFalse(opt.isPresent());
assertEquals("OptionalLong.empty", opt.toString());
System.out.println("空 OptionalLong: " + opt);
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### of

- 描述: 包含值的 OptionalLong
- 断言: isPresent 为 true

```java
// 方法体开始
System.out.println("=== of ===");
OptionalLong opt = OptionalLong.of(100L);
assertTrue(opt.isPresent());
assertEquals(100L, opt.getAsLong());
System.out.println("OptionalLong: " + opt);
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### orElse

- 描述: 值或默认值
- 断言: 有值返回原值，无值返回默认值

```java
// 方法体开始
System.out.println("=== orElse ===");
assertEquals(100L, OptionalLong.of(100L).orElse(0L));
assertEquals(999L, OptionalLong.empty().orElse(999L));
System.out.println("of(100).orElse(0): " + OptionalLong.of(100L).orElse(0L));
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### isPresent

- 描述: 判断值是否存在
- 断言: 有值时返回 true

```java
// 方法体开始
System.out.println("=== isPresent ===");
assertTrue(OptionalLong.of(1L).isPresent());
assertFalse(OptionalLong.empty().isPresent());
System.out.println("of(1).isPresent: " + OptionalLong.of(1L).isPresent());
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### getAsLong

- 描述: 获取 long 值
- 断言: 返回值正确

```java
// 方法体开始
System.out.println("=== getAsLong ===");
long val = OptionalLong.of(100L).getAsLong();
assertEquals(100L, val);
System.out.println("getAsLong: " + val);
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### toString

- 描述: 字符串表示
- 断言: 有值时返回正确字符串

```java
// 方法体开始
System.out.println("=== toString ===");
assertEquals("OptionalLong[100]", OptionalLong.of(100L).toString());
assertEquals("OptionalLong.empty", OptionalLong.empty().toString());
System.out.println("toString: " + OptionalLong.of(100L));
System.out.println("=== 测试通过 ===");
// 方法体结束
```

