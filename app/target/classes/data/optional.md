---
name: Optional
package: java.util
order: 4
---

## 介绍

`java.util.Optional<T>` 是 Java 8 引入的**容器对象**，用于表示一个值存在或不存在。它通过类型系统让"可能为空"的语义变得显式化，减少 NullPointerException。

Optional 的核心特点：
- **显式空值处理**：强制调用方考虑值为空的情况
- **函数式风格**：配合 Lambda 实现声明式编程
- **不可序列化**：不适合作为对象字段
- **最佳实践**：主要用于返回值类型

## 方法

### of / ofNullable / empty

```java
public static <T> Optional<T> of(T value)
public static <T> Optional<T> ofNullable(T value)
public static <T> Optional<T> empty()
```

创建 Optional。

### isPresent

```java
public boolean isPresent()
```

值存在返回 true。

### ifPresent

```java
public void ifPresent(Consumer<? super T> consumer)
```

值存在时执行操作。

### orElse / orElseGet / orElseThrow

```java
public T orElse(T other)
public T orElseGet(Supplier<? extends T> other)
public <X extends Throwable> T orElseThrow(Supplier<? extends X> exceptionSupplier)
```

值不存在时返回默认值或抛出异常。

### map / filter

```java
public <U> Optional<U> map(Function<? super T, ? extends U> mapper)
public Optional<T> filter(Predicate<? super T> predicate)
```

值存在时映射或过滤。

## 测试

### of / ofNullable / empty

- 描述: 三种创建方式
- 断言: 创建结果正确

```java
// 方法体开始
System.out.println("=== 创建 ===");
Optional<String> opt1 = Optional.of("hello");
Optional<String> opt2 = Optional.ofNullable(null);
Optional<String> opt3 = Optional.empty();
assertTrue(opt1.isPresent());
assertFalse(opt2.isPresent());
assertFalse(opt3.isPresent());
System.out.println("of: " + opt1 + ", ofNullable: " + opt2 + ", empty: " + opt3);
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### orElse / orElseGet

- 描述: 值不存在时返回默认值
- 断言: 默认值正确

```java
// 方法体开始
System.out.println("=== orElse ===");
Optional<String> empty = Optional.empty();
assertEquals("default", empty.orElse("default"));
assertEquals("default", empty.orElseGet(() -> "default"));
Optional<String> present = Optional.of("hello");
assertEquals("hello", present.orElse("default"));
System.out.println("orElse 测试通过");
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### ifPresent

- 描述: 值存在时执行操作
- 断言: 操作正常执行

```java
// 方法体开始
System.out.println("=== ifPresent ===");
List<String> result = new ArrayList<>();
Optional.of("hello").ifPresent(result::add);
assertEquals(1, result.size());
assertEquals("hello", result.get(0));
Optional.empty().ifPresent(s -> result.add("wrong"));
assertEquals(1, result.size());
System.out.println("ifPresent 测试通过");
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### map / filter

- 描述: 使用 map 和 filter 链式操作
- 断言: 操作正确

```java
// 方法体开始
System.out.println("=== map/filter ===");
Optional<String> result = Optional.of("Hello World")
        .filter(s -> s.length() > 5)
        .map(String::toUpperCase);
assertEquals("HELLO WORLD", result.get());
Optional<String> filtered = Optional.of("Hi")
        .filter(s -> s.length() > 5);
assertFalse(filtered.isPresent());
System.out.println("map/filter: " + result);
System.out.println("=== 测试通过 ===");
// 方法体结束
```
