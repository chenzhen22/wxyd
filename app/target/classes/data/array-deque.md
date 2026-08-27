---
name: ArrayDeque
package: java.util
order: 256
---

## 介绍

`java.util.ArrayDeque` 是**可扩容数组实现的双端队列**，通常作为栈和队列使用。Java 8 获得了 Collection 所有默认方法的支持。

ArrayDeque 的核心特点：
- **双端操作**：高效的头部和尾部插入/删除
- **替代 Stack**：作为栈使用时比 Stack 快
- **替代 LinkedList**：作为队列使用时通常比 LinkedList 快
- **无容量限制**：自动扩容

## 方法

构造方法：
```java
public ArrayDeque()
public ArrayDeque(int numElements)
```

### addFirst / addLast / getFirst / getLast / pollFirst / pollLast

双端队列方法。

### push / pop / peek

栈方法（Deque 接口）。

## 测试

- 描述: ArrayDeque 作为队列
- 断言: FIFO 顺序正确

```java
// 方法体开始
System.out.println("=== ArrayDeque 队列 ===");
ArrayDeque<String> queue = new ArrayDeque<>();
queue.addLast("A");
queue.addLast("B");
queue.addLast("C");
assertEquals("A", queue.pollFirst());
assertEquals("B", queue.pollFirst());
assertEquals("C", queue.pollFirst());
assertNull(queue.pollFirst());
System.out.println("FIFO 队列正确");
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### 作为栈

- 描述: ArrayDeque 作为栈使用
- 断言: LIFO 顺序正确

```java
// 方法体开始
System.out.println("=== ArrayDeque 栈 ===");
ArrayDeque<Integer> stack = new ArrayDeque<>();
stack.push(1);
stack.push(2);
stack.push(3);
assertEquals(Integer.valueOf(3), stack.pop());
assertEquals(Integer.valueOf(2), stack.pop());
assertEquals(Integer.valueOf(1), stack.pop());
System.out.println("LIFO 栈正确");
System.out.println("=== 测试通过 ===");
// 方法体结束
```
