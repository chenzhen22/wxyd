---
name: LinkedList
package: java.util
order: 176
---

## 介绍

`java.util.LinkedList` 是**双向链表实现的 List 和 Deque**。Java 8 获得了 Collection 和 Iterable 接口的所有默认方法支持。

LinkedList 的核心特点：
- **双端操作**：实现 Deque 接口，支持头尾操作
- **插入/删除快**：O(1) 的头尾插入删除
- **随机访问慢**：O(n) 的 get
- **可作为 Queue/Deque/Stack 使用**

## 方法

构造方法：
```java
public LinkedList()
public LinkedList(Collection<? extends E> c)
```

特有方法：
- `addFirst(E)` / `addLast(E)` — 头尾添加
- `getFirst()` / `getLast()` — 头尾获取
- `removeFirst()` / `removeLast()` — 头尾移除
- `push(E)` / `pop()` — 栈操作

Java 8 方法：`forEach`、`removeIf`、`replaceAll`、`sort`

## 测试

### 双端队列

- 描述: LinkedList 作为双端队列使用
- 断言: 头尾操作正确

```java
// 方法体开始
System.out.println("=== 双端队列 ===");
LinkedList<String> deque = new LinkedList<>();
deque.addLast("B");
deque.addLast("C");
deque.addFirst("A");
assertEquals("A", deque.getFirst());
assertEquals("C", deque.getLast());
assertEquals("[A, B, C]", deque.toString());
deque.removeFirst();
assertEquals("B", deque.getFirst());
System.out.println("deque: " + deque);
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### 栈操作

- 描述: LinkedList 作为栈使用
- 断言: LIFO 顺序

```java
// 方法体开始
System.out.println("=== 栈操作 ===");
LinkedList<String> stack = new LinkedList<>();
stack.push("A");
stack.push("B");
stack.push("C");
assertEquals("C", stack.pop());
assertEquals("B", stack.pop());
assertEquals("A", stack.pop());
System.out.println("栈顺序正确");
System.out.println("=== 测试通过 ===");
// 方法体结束
```
