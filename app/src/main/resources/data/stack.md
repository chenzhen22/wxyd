---
name: Stack
package: java.util
order: 181
---

## 介绍

`java.util.Stack` 是**后进先出（LIFO）堆栈**，继承自 `Vector`。Java 8 获得了 Collection 接口的所有默认方法支持。

虽然 Stack 是早期 Java 的遗留类，但仍然是经典的数据结构实现。`Deque` 接口（如 `ArrayDeque`）在大多数场景中推荐替代 Stack。

## 方法

### push

```java
public E push(E item)
```

将元素压入栈顶。

### pop

```java
public E pop()
```

弹出栈顶元素。

### peek

```java
public E peek()
```

查看栈顶元素但不弹出。

### empty / search

```java
public boolean empty()
public int search(Object o)
```

判断栈是否为空 / 查找元素位置。

## 测试

### 基本操作

- 描述: Stack 的 push/pop/peek
- 断言: LIFO 顺序正确

```java
// 方法体开始
System.out.println("=== Stack ===");
Stack<String> stack = new Stack<>();
assertTrue(stack.empty());
stack.push("A");
stack.push("B");
stack.push("C");
assertEquals("C", stack.peek());
assertEquals("C", stack.pop());
assertEquals("B", stack.pop());
assertEquals("A", stack.pop());
assertTrue(stack.empty());
System.out.println("LIFO 顺序正确");
System.out.println("=== 测试通过 ===");
// 方法体结束
```
