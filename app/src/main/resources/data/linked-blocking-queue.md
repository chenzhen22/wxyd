---
name: LinkedBlockingQueue
package: java.util.concurrent
order: 50
---

## 介绍

`java.util.concurrent.LinkedBlockingQueue` 是 Java 5 引入的**线程安全阻塞队列**，基于链表实现，支持 FIFO 顺序。它是经典的生产者-消费者模式的核心组件。

常用场景：
- **生产者-消费者**：`put()` 阻塞放入，`take()` 阻塞取出
- **线程池任务队列**：`ThreadPoolExecutor` 的默认工作队列

## 方法

### LinkedBlockingQueue()

```java
public LinkedBlockingQueue()
```

创建容量为 Integer.MAX_VALUE 的阻塞队列。

### LinkedBlockingQueue(int)

```java
public LinkedBlockingQueue(int capacity)
```

创建指定容量的阻塞队列。

- **参数**: `capacity` — 容量

### put

```java
public void put(E e) throws InterruptedException
```

添加元素，队列满时阻塞等待。

- **参数**: `e` — 元素

### take

```java
public E take() throws InterruptedException
```

取出元素，队列空时阻塞等待。

- **返回**: `E`

### offer

```java
public boolean offer(E e)
```

添加元素，成功返回 true，队列满返回 false。

- **参数**: `e` — 元素
- **返回**: `boolean`

### offer(timeout, unit)

```java
public boolean offer(E e, long timeout, TimeUnit unit) throws InterruptedException
```

在超时时间内尝试添加元素。

- **参数**: `e` — 元素；`timeout` — 超时；`unit` — 单位
- **返回**: `boolean`

### poll

```java
public E poll(long timeout, TimeUnit unit) throws InterruptedException
```

在超时时间内取出元素。

- **参数**: `timeout` — 超时；`unit` — 单位
- **返回**: `E`

### size

```java
public int size()
```

返回元素数量。

- **返回**: `int`

### remainingCapacity

```java
public int remainingCapacity()
```

返回剩余容量。

- **返回**: `int`

### isEmpty

```java
public boolean isEmpty()
```

判断是否为空。

- **返回**: `boolean`

### clear

```java
public void clear()
```

清空队列。

### contains

```java
public boolean contains(Object o)
```

判断是否包含指定元素。

- **参数**: `o` — 元素
- **返回**: `boolean`

## 测试

### LinkedBlockingQueue

- 描述: 创建 LinkedBlockingQueue
- 断言: 空队列 size 为 0

```java
// 方法体开始
System.out.println("=== linkedBlockingQueue ===");
LinkedBlockingQueue<String> queue = new LinkedBlockingQueue<>(10);
assertTrue(queue.isEmpty());
assertEquals(0, queue.size());
assertEquals(10, queue.remainingCapacity());
System.out.println("队列容量: 10, 初始大小: " + queue.size());
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### put

- 描述: 放入并取出元素
- 断言: 元素正确取出

```java
// 方法体开始
System.out.println("=== putAndTake ===");
LinkedBlockingQueue<String> queue = new LinkedBlockingQueue<>();
queue.put("hello");
assertEquals(1, queue.size());
String val = queue.take();
assertEquals("hello", val);
assertTrue(queue.isEmpty());
System.out.println("放入: hello, 取出: " + val);
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### offer

- 描述: 非阻塞添加元素
- 断言: 添加成功返回 true，满时返回 false

```java
// 方法体开始
System.out.println("=== offer ===");
LinkedBlockingQueue<Integer> queue = new LinkedBlockingQueue<>(2);
assertTrue(queue.offer(1));
assertTrue(queue.offer(2));
assertFalse(queue.offer(3));
assertEquals(2, queue.size());
System.out.println("添加 2 个元素后 size: " + queue.size());
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### poll

- 描述: 带超时取出元素
- 断言: 有元素时正常取出

```java
// 方法体开始
System.out.println("=== poll ===");
LinkedBlockingQueue<String> queue = new LinkedBlockingQueue<>();
queue.put("test");
String val = queue.poll(100, TimeUnit.MILLISECONDS);
assertEquals("test", val);
System.out.println("poll 取出: " + val);
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### contains

- 描述: 判断是否包含元素
- 断言: 存在返回 true

```java
// 方法体开始
System.out.println("=== contains ===");
LinkedBlockingQueue<String> queue = new LinkedBlockingQueue<>();
queue.put("a");
queue.put("b");
assertTrue(queue.contains("a"));
assertTrue(queue.contains("b"));
assertFalse(queue.contains("c"));
System.out.println("包含 a: " + queue.contains("a") + ", 包含 c: " + queue.contains("c"));
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### clear

- 描述: 清空队列
- 断言: clear 后 size 为 0

```java
// 方法体开始
System.out.println("=== clear ===");
LinkedBlockingQueue<String> queue = new LinkedBlockingQueue<>();
queue.put("a");
queue.put("b");
queue.clear();
assertEquals(0, queue.size());
assertTrue(queue.isEmpty());
System.out.println("clear 后 size: " + queue.size());
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### take

- 描述: 取出元素
- 断言: 元素正确取出

```java
// 方法体开始
System.out.println("=== take ===");
LinkedBlockingQueue<String> q = new LinkedBlockingQueue<>();
q.put("test");
String val = q.take();
assertEquals("test", val);
System.out.println("take: " + val);
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### remainingCapacity

- 描述: 获取剩余容量
- 断言: 容量正确

```java
// 方法体开始
System.out.println("=== remainingCapacity ===");
LinkedBlockingQueue<String> q = new LinkedBlockingQueue<>(5);
assertEquals(5, q.remainingCapacity());
System.out.println("剩余容量: " + q.remainingCapacity());
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### isEmpty

- 描述: 判断是否为空
- 断言: 空返回 true

```java
// 方法体开始
System.out.println("=== isEmpty ===");
LinkedBlockingQueue<String> q = new LinkedBlockingQueue<>();
assertTrue(q.isEmpty());
System.out.println("isEmpty: " + q.isEmpty());
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### size

- 描述: 元素数量
- 断言: 数量正确

```java
// 方法体开始
System.out.println("=== size ===");
LinkedBlockingQueue<String> q = new LinkedBlockingQueue<>();
assertEquals(0, q.size());
System.out.println("size: " + q.size());
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### offer

- 描述: 非阻塞添加元素
- 断言: 添加成功返回 true

```java
// 方法体开始
System.out.println("=== offer ===");
LinkedBlockingQueue<Integer> q = new LinkedBlockingQueue<>(2);
assertTrue(q.offer(1));
assertTrue(q.offer(2));
assertFalse(q.offer(3));
System.out.println("offer 正确");
System.out.println("=== 测试通过 ===");
// 方法体结束
```
