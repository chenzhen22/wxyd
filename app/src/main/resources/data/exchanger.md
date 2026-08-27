---
name: Exchanger
package: java.util.concurrent
order: 70
---

## 介绍

`Exchanger<V>` 是 Java 并发包中用于**两个线程之间交换数据**的同步点。当两个线程在交换点相遇时，它们互相交换持有的数据，然后继续执行。可以理解为"双方同时交出各自物品并得到对方的物品"。

Exchanger 的核心特点：
- **双线程交换**：精确用于两个线程之间的数据交换
- **同步点**：两个线程必须在同一时刻到达交换点才能完成交换
- **泛型支持**：两个线程可以交换不同类型的数据（通过多个 Exchanger）
- **超时支持**：提供定时版本的 `exchange` 方法

Exchanger 的常用场景：
- **生产者-消费者**：一个线程填充缓冲区，另一个线程消费缓冲区内容
- **遗传算法**：在算法迭代中交换配对数据
- **管道处理**：两个线程交换已处理和待处理的数据

## 方法

### exchange(V)

```java
public V exchange(V x) throws InterruptedException
```

在交换点等待另一个线程到达，然后交换数据。如果另一个线程还没到达则阻塞等待。

- **参数**: `x` — 要交换的数据
- **返回**: `V` — 另一个线程交换过来的数据
- **抛出**: `InterruptedException` — 等待被中断

### exchange(V, long, TimeUnit)

```java
public V exchange(V x, long timeout, TimeUnit unit) throws InterruptedException, TimeoutException
```

带超时的交换版本，在指定时间内没有另一个线程到达则抛出 `TimeoutException`。

- **参数**: `x` — 要交换的数据；`timeout` — 超时时间；`unit` — 时间单位
- **返回**: `V` — 另一个线程交换过来的数据
- **抛出**: `InterruptedException` — 等待被中断；`TimeoutException` — 超时

## 测试

### 简单数据交换

- 描述: 两个线程通过 Exchanger 交换数据
- 断言: 两个线程都能收到对方的数据

```java
// 方法体开始
System.out.println("=== 简单数据交换 ===");
Exchanger<String> exchanger = new Exchanger<>();
List<String> result1 = new ArrayList<>();
List<String> result2 = new ArrayList<>();
Thread t1 = new Thread(() -> {
    try {
        String received = exchanger.exchange("来自线程 A");
        result1.add(received);
    } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
    }
});
Thread t2 = new Thread(() -> {
    try {
        String received = exchanger.exchange("来自线程 B");
        result2.add(received);
    } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
    }
});
t1.start();
t2.start();
t1.join();
t2.join();
assertEquals("来自线程 B", result1.get(0));
assertEquals("来自线程 A", result2.get(0));
System.out.println("线程 A 收到: " + result1.get(0));
System.out.println("线程 B 收到: " + result2.get(0));
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### 交换整数

- 描述: 两个线程交换整数数据
- 断言: 交换后双方获得对方的值

```java
// 方法体开始
System.out.println("=== 交换整数 ===");
Exchanger<Integer> exchanger = new Exchanger<>();
int[] result1 = new int[1];
int[] result2 = new int[1];
Thread t1 = new Thread(() -> {
    try {
        result1[0] = exchanger.exchange(100);
    } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
    }
});
Thread t2 = new Thread(() -> {
    try {
        result2[0] = exchanger.exchange(200);
    } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
    }
});
t1.start();
t2.start();
t1.join();
t2.join();
assertEquals(200, result1[0]);
assertEquals(100, result2[0]);
System.out.println("线程 A 得到: " + result1[0] + ", 线程 B 得到: " + result2[0]);
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### 带超时的交换

- 描述: 使用带超时的 exchange 方法
- 断言: 另一个线程及时到达，交换成功

```java
// 方法体开始
System.out.println("=== 带超时的交换 ===");
Exchanger<String> exchanger = new Exchanger<>();
String[] result = new String[1];
Thread t1 = new Thread(() -> {
    try {
        result[0] = exchanger.exchange("Hello", 1, TimeUnit.SECONDS);
    } catch (Exception e) {
        result[0] = "TIMEOUT: " + e.getClass().getSimpleName();
    }
});
Thread t2 = new Thread(() -> {
    try {
        Thread.sleep(200); // 稍微延迟
        exchanger.exchange("World");
    } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
    }
});
t1.start();
t2.start();
t1.join();
t2.join();
assertEquals("World", result[0]);
System.out.println("交换结果: " + result[0]);
System.out.println("=== 测试通过 ===");
// 方法体结束
```
