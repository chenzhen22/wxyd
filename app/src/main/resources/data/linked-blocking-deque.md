---
name: LinkedBlockingDeque
package: java.util.concurrent
order: 120
---

## 介绍

`java.util.concurrent.LinkedBlockingDeque` 是 Java 6 引入的**可选有界并发双端队列**，基于链表实现。与 `LinkedBlockingQueue` 类似，但支持双端操作。

LinkedBlockingDeque 的核心特点：
- **双端操作**：可以从头部和尾部插入/移除
- **可选有界**：构造时指定容量，不指定则容量为 Integer.MAX_VALUE
- **线程安全**：使用 ReentrantLock 保证并发安全
- **支持阻塞**：提供 put/take 方法的阻塞版本

## 方法

构造方法：
```java
public LinkedBlockingDeque()
public LinkedBlockingDeque(int capacity)
public LinkedBlockingDeque(Collection<? extends E> c)
```

双端操作方法（接口 Deque 定义）：
- `addFirst(E)` / `addLast(E)` — 头部/尾部添加（满时抛异常）
- `offerFirst(E)` / `offerLast(E)` — 头部/尾部添加（满时返回 false）
- `putFirst(E)` / `putLast(E)` — 头部/尾部阻塞添加
- `takeFirst()` / `takeLast()` — 头部/尾部阻塞移除
- `getFirst()` / `getLast()` — 获取但不移除
- `pollFirst()` / `pollLast()` — 获取并移除（空时返回 null）
- `peekFirst()` / `peekLast()` — 获取但不移除（空时返回 null）

## 测试

### 基本操作

- 描述: LinkedBlockingDeque 添加和获取
- 断言: 添加顺序正确

```java
// 方法体开始
System.out.println("=== 基本操作 ===");
LinkedBlockingDeque<String> deque = new LinkedBlockingDeque<>(3);
deque.addLast("A");
deque.addLast("B");
deque.addFirst("0");
assertEquals("0", deque.getFirst());
assertEquals("B", deque.getLast());
assertEquals(3, deque.size());
System.out.println("deque: " + deque);
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### poll 操作

- 描述: 从两端获取并移除元素
- 断言: 正确移除

```java
// 方法体开始
System.out.println("=== poll 操作 ===");
LinkedBlockingDeque<Integer> deque = new LinkedBlockingDeque<>(5);
deque.addLast(1);
deque.addLast(2);
deque.addLast(3);
assertEquals(Integer.valueOf(1), deque.pollFirst());
assertEquals(Integer.valueOf(3), deque.pollLast());
assertEquals(Integer.valueOf(2), deque.peekFirst());
System.out.println("剩余: " + deque);
System.out.println("=== 测试通过 ===");
// 方法体结束
```
