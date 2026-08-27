---
name: Consumer
package: java.util.function
order: 27
---

## 介绍

`Consumer<T>` 是 Java 8 引入的一个**函数式接口**，代表一个接受单个参数但不返回结果的操作（即"消费"一个值）。它通常用于遍历集合、打印日志、修改对象状态等场景。

Consumer 的核心特点：
- **消费数据**：接受一个参数，执行副作用操作
- **链式执行**：通过 `andThen` 组合多个 Consumer
- **无返回值**：所有方法返回 `void`

Consumer 的两个方法：
- `accept(T t)` — 核心方法，对给定参数执行操作
- `andThen(Consumer)` — 先执行当前操作，再执行另一个操作

## 方法

### accept

```java
void accept(T t)
```

对给定参数执行操作。

- **参数**: `t` — 输入参数
- **返回**: 无

### andThen

```java
default Consumer<T> andThen(Consumer<? super T> after)
```

返回一个组合 Consumer，先执行当前操作，再执行 `after` 操作。如果当前操作抛出异常，`after` 不会执行。

- **参数**: `after` — 后执行的操作
- **返回**: `Consumer<T>` — 组合后的 Consumer

## 测试

### accept

- 描述: 使用 `accept` 方法向 StringBuilder 追加字符
- 断言: accept 执行后 StringBuilder 内容正确

```java
// 方法体开始
System.out.println("=== accept ===");
StringBuilder sb = new StringBuilder();
Consumer<StringBuilder> appendA = s -> s.append("A");
appendA.accept(sb);
assertEquals("A", sb.toString());
appendA.accept(sb);
assertEquals("AA", sb.toString());
System.out.println("两次 accept 后: " + sb.toString());
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### andThen

- 描述: 使用 `andThen` 组合两个 Consumer 依次执行
- 断言: 先追加 "A" 再追加 "B"，结果为 "AB"

```java
// 方法体开始
System.out.println("=== andThen ===");
StringBuilder sb = new StringBuilder();
Consumer<StringBuilder> appendA = s -> s.append("A");
Consumer<StringBuilder> appendB = s -> s.append("B");
Consumer<StringBuilder> combined = appendA.andThen(appendB);
combined.accept(sb);
assertEquals("AB", sb.toString());
System.out.println("andThen 组合结果: " + sb.toString());
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### andThen 链式调用

- 描述: 使用 `andThen` 链式组合多个 Consumer
- 断言: 执行三次消费操作，结果为 "ABC"

```java
// 方法体开始
System.out.println("=== andThen 链式 ===");
StringBuilder sb = new StringBuilder();
Consumer<StringBuilder> appendA = s -> s.append("A");
Consumer<StringBuilder> appendB = s -> s.append("B");
Consumer<StringBuilder> appendC = s -> s.append("C");
Consumer<StringBuilder> chain = appendA.andThen(appendB).andThen(appendC);
chain.accept(sb);
assertEquals("ABC", sb.toString());
System.out.println("链式 andThen 结果: " + sb.toString());
System.out.println("=== 测试通过 ===");
// 方法体结束
```
