---
name: OptionalInt
package: java.util
order: 46
---

## 介绍

`java.util.OptionalInt` 是 Java 8 引入的 **int 类型的 Optional 容器**，用于避免 int 值的 null 检查和 `NoSuchElementException`。

## 方法

### empty

```java
public static OptionalInt empty()
```

返回空的 OptionalInt。

### of

```java
public static OptionalInt of(int value)
```

包含指定 int 值的 OptionalInt。

### isPresent

```java
public boolean isPresent()
```

判断值是否存在。

### getAsInt

```java
public int getAsInt()
```

获取值，空时抛出异常。

### orElse

```java
public int orElse(int other)
```

值存在返回值，否则返回默认值。

### orElseGet

```java
public int orElseGet(IntSupplier supplier)
```

值存在返回值，否则调用供应商函数。

### orElseThrow

```java
public int orElseThrow(Supplier<X> exceptionSupplier)
```

值存在返回值，否则抛异常。

### toString

```java
public String toString()
```

字符串表示。

## 测试

### empty

- 描述: 创建空 OptionalInt
- 断言: isPresent 为 false

```java
// 方法体开始
System.out.println("=== empty ===");
OptionalInt opt = OptionalInt.empty();
assertFalse(opt.isPresent());
assertEquals("OptionalInt.empty", opt.toString());
System.out.println("空 OptionalInt: " + opt);
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### of

- 描述: 包含值的 OptionalInt
- 断言: isPresent 为 true

```java
// 方法体开始
System.out.println("=== of ===");
OptionalInt opt = OptionalInt.of(42);
assertTrue(opt.isPresent());
assertEquals(42, opt.getAsInt());
System.out.println("OptionalInt: " + opt);
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### orElse

- 描述: 值或默认值
- 断言: 有值返回原值，无值返回默认值

```java
// 方法体开始
System.out.println("=== orElse ===");
assertEquals(42, OptionalInt.of(42).orElse(0));
assertEquals(100, OptionalInt.empty().orElse(100));
System.out.println("of(42).orElse(0): " + OptionalInt.of(42).orElse(0));
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### orElseThrow

- 描述: 值存在返回值，否则抛出异常
- 断言: 空时抛出异常

```java
// 方法体开始
System.out.println("=== orElseThrow ===");
int val = OptionalInt.of(10).orElseThrow(() -> new RuntimeException("no value"));
assertEquals(10, val);
try {
    OptionalInt.empty().orElseThrow(() -> new RuntimeException("empty"));
    fail("应抛出异常");
} catch (RuntimeException e) {
    assertEquals("empty", e.getMessage());
}
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### isPresent

- 描述: 判断值是否存在
- 断言: 有值时返回 true

```java
// 方法体开始
System.out.println("=== isPresent ===");
assertTrue(OptionalInt.of(1).isPresent());
assertFalse(OptionalInt.empty().isPresent());
System.out.println("of(1).isPresent: " + OptionalInt.of(1).isPresent());
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### getAsInt

- 描述: 获取 int 值
- 断言: 返回值正确

```java
// 方法体开始
System.out.println("=== getAsInt ===");
int val = OptionalInt.of(42).getAsInt();
assertEquals(42, val);
System.out.println("getAsInt: " + val);
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### toString

- 描述: 字符串表示
- 断言: 有值时返回正确字符串

```java
// 方法体开始
System.out.println("=== toString ===");
assertEquals("OptionalInt[42]", OptionalInt.of(42).toString());
assertEquals("OptionalInt.empty", OptionalInt.empty().toString());
System.out.println("toString: " + OptionalInt.of(42));
System.out.println("=== 测试通过 ===");
// 方法体结束
```

