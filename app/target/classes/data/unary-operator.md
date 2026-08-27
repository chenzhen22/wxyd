---
name: UnaryOperator
package: java.util.function
order: 58
---

## 介绍

`UnaryOperator<T>` 是 Java 8 引入的一个**函数式接口**，是 `Function<T, T>` 的特化版本。它代表一个输入和输出类型**相同**的函数，即对单个值进行"一元运算"。

UnaryOperator 的核心特点：
- **类型不变**：输入输出类型相同
- **恒等函数**：通过 `identity()` 返回不改变输入的运算
- **链式组合**：继承 Function 的 `andThen` 和 `compose`
- **常用场景**：`Stream.iterate()`、`List.replaceAll()`、`Map.replaceAll()`

UnaryOperator 从 Function 继承的所有方法：
- `apply(T t)` — 核心方法，对输入执行运算并返回结果
- `andThen(Function)` — 先执行当前运算，再对结果执行另一个 Function
- `compose(Function)` — 先执行另一个 Function，再对结果执行当前运算
- `identity()` — 静态方法，返回输入本身

为什么需要 UnaryOperator？因为它明确表达了"类型不变"的语义，比起 `Function<T, T>` 更加语义化，而且在泛型推断中表现更好。

## 方法

### apply

```java
T apply(T t)
```

对给定参数执行运算，返回同类型结果。

- **参数**: `t` — 输入参数
- **返回**: `T` — 运算结果

### identity

```java
static <T> UnaryOperator<T> identity()
```

返回一个恒等 UnaryOperator，即输入什么就返回什么。

- **返回**: `UnaryOperator<T>` — 恒等运算符

### andThen / compose

继承自 `Function` 接口，详见 [Function](?name=Function) 文档。

## 测试

### apply 字符串转大写

- 描述: 使用 `apply` 方法将字符串转为大写
- 断言: `"hello"` 转为 `"HELLO"`

```java
// 方法体开始
System.out.println("=== apply 大写 ===");
UnaryOperator<String> toUpper = String::toUpperCase;
assertEquals("HELLO", toUpper.apply("hello"));
assertEquals("JAVA", toUpper.apply("java"));
System.out.println("hello -> " + toUpper.apply("hello"));
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### apply 数值运算

- 描述: 使用 UnaryOperator 计算平方
- 断言: 5 的平方是 25

```java
// 方法体开始
System.out.println("=== apply 平方 ===");
UnaryOperator<Integer> square = x -> x * x;
assertEquals(Integer.valueOf(25), square.apply(5));
assertEquals(Integer.valueOf(0), square.apply(0));
assertEquals(Integer.valueOf(100), square.apply(10));
System.out.println("5 的平方: " + square.apply(5));
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### identity

- 描述: 使用 `identity` 返回输入本身
- 断言: 输入什么就返回什么

```java
// 方法体开始
System.out.println("=== identity ===");
UnaryOperator<String> id = UnaryOperator.identity();
assertEquals("hello", id.apply("hello"));
assertEquals("", id.apply(""));
System.out.println("identity(\"hello\"): " + id.apply("hello"));
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### Stream.iterate

- 描述: 使用 `UnaryOperator` 配合 `Stream.iterate` 生成序列
- 断言: 从 1 开始重复 `x -> x + 2` 生成前 5 个奇数

```java
// 方法体开始
System.out.println("=== Stream.iterate ===");
UnaryOperator<Integer> addTwo = x -> x + 2;
List<Integer> odds = Stream.iterate(1, addTwo)
        .limit(5)
        .collect(Collectors.toList());
assertEquals(Arrays.asList(1, 3, 5, 7, 9), odds);
System.out.println("前 5 个奇数: " + odds);
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### List.replaceAll

- 描述: 使用 `UnaryOperator` 批量转换列表中的每个元素
- 断言: 所有字符串转为大写

```java
// 方法体开始
System.out.println("=== List.replaceAll ===");
List<String> names = new ArrayList<>(Arrays.asList("alice", "bob", "charlie"));
names.replaceAll(String::toUpperCase);
assertEquals("ALICE", names.get(0));
assertEquals("BOB", names.get(1));
assertEquals("CHARLIE", names.get(2));
System.out.println("转换后: " + names);
System.out.println("=== 测试通过 ===");
// 方法体结束
```
