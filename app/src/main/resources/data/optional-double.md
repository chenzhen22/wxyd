---
name: OptionalDouble
package: java.util
order: 48
---

## 介绍

`java.util.OptionalDouble` 是 Java 8 引入的 **double 类型的 Optional 容器**，与 OptionalInt 类似但针对 double 值。

## 方法

### empty

```java
public static OptionalDouble empty()
```

返回空的 OptionalDouble。

### of

```java
public static OptionalDouble of(double value)
```

包含指定 double 值的 OptionalDouble。

### isPresent

```java
public boolean isPresent()
```

判断值是否存在。

### getAsDouble

```java
public double getAsDouble()
```

获取值，空时抛出异常。

### orElse

```java
public double orElse(double other)
```

值存在返回值，否则返回默认值。

### toString

```java
public String toString()
```

字符串表示。

## 测试

### empty

- 描述: 创建空 OptionalDouble
- 断言: isPresent 为 false

```java
// 方法体开始
System.out.println("=== empty ===");
OptionalDouble opt = OptionalDouble.empty();
assertFalse(opt.isPresent());
assertEquals("OptionalDouble.empty", opt.toString());
System.out.println("空 OptionalDouble: " + opt);
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### of

- 描述: 包含值的 OptionalDouble
- 断言: isPresent 为 true

```java
// 方法体开始
System.out.println("=== of ===");
OptionalDouble opt = OptionalDouble.of(3.14);
assertTrue(opt.isPresent());
assertEquals(3.14, opt.getAsDouble(), 0.001);
System.out.println("OptionalDouble: " + opt);
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### orElse

- 描述: 值或默认值
- 断言: 有值返回原值，无值返回默认值

```java
// 方法体开始
System.out.println("=== orElse ===");
assertEquals(3.14, OptionalDouble.of(3.14).orElse(0.0), 0.001);
assertEquals(99.9, OptionalDouble.empty().orElse(99.9), 0.001);
System.out.println("of(3.14).orElse(0): " + OptionalDouble.of(3.14).orElse(0.0));
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### isPresent

- 描述: 判断值是否存在
- 断言: 有值时返回 true

```java
// 方法体开始
System.out.println("=== isPresent ===");
assertTrue(OptionalDouble.of(1.0).isPresent());
assertFalse(OptionalDouble.empty().isPresent());
System.out.println("of(1.0).isPresent: " + OptionalDouble.of(1.0).isPresent());
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### getAsDouble

- 描述: 获取 double 值
- 断言: 返回值正确

```java
// 方法体开始
System.out.println("=== getAsDouble ===");
double val = OptionalDouble.of(3.14).getAsDouble();
assertEquals(3.14, val, 0.001);
System.out.println("getAsDouble: " + val);
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### toString

- 描述: 字符串表示
- 断言: 有值时返回正确字符串

```java
// 方法体开始
System.out.println("=== toString ===");
assertEquals("OptionalDouble[3.14]", OptionalDouble.of(3.14).toString());
assertEquals("OptionalDouble.empty", OptionalDouble.empty().toString());
System.out.println("toString: " + OptionalDouble.of(3.14));
System.out.println("=== 测试通过 ===");
// 方法体结束
```

