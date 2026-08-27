---
name: Phaser
package: java.util.concurrent
order: 61
---

## 介绍

`Phaser` 是 Java 7 引入的一个**灵活的同步屏障**，可以与 `CountDownLatch` 和 `CyclicBarrier` 类比，但更加强大和灵活。`Phaser` 允许一组线程在"阶段"（phase）上进行协调，每个阶段都可以有不同数量的参与者。

Phaser 的核心特点：
- **动态注册**：参与者可以在运行时动态增加或减少
- **阶段复用**：同一个 Phaser 对象可以重复用于多个阶段
- **灵活同步**：支持 `arrive()`、`arriveAndAwaitAdvance()`、`arriveAndDeregister()` 等多种同步模式
- **层级结构**：支持父子 Phaser 树形结构

Phaser 的生命周期：
1. 创建 Phaser，初始参与者数量可指定
2. 参与者调用 `arriveAndAwaitAdvance()` 等待其他参与者
3. 当所有参与者都到达时，Phaser 进入下一阶段
4. 参与者可以通过 `arriveAndDeregister()` 注销自己
5. 当所有参与者都注销后，Phaser 进入终止状态

Phaser 常用于**多阶段并行计算**、**分步任务协调**等场景。

## 方法

### 构造方法

```java
public Phaser()
public Phaser(int parties)
public Phaser(Phaser parent)
public Phaser(Phaser parent, int parties)
```

创建一个 Phaser。无参构造创建初始注册数为 0 的 Phaser。

- **参数**: `parties` — 初始参与者数量；`parent` — 父 Phaser
- **返回**: 无

### register

```java
public int register()
```

添加一个新的未到达的参与者到 Phaser 中。等价于 `bulkRegister(1)`。

- **返回**: `int` — 注册时所在的阶段号

### bulkRegister

```java
public int bulkRegister(int parties)
```

批量添加指定数量的参与者。

- **参数**: `parties` — 要添加的参与者数量
- **返回**: `int` — 注册时所在的阶段号

### arrive

```java
public int arrive()
```

到达但**不等待**其他参与者。这是一个非阻塞方法。

- **返回**: `int` — 到达时的阶段号
- **注意**: 使用此方法需要配合外部等待或检查阶段变化

### arriveAndAwaitAdvance

```java
public int arriveAndAwaitAdvance()
```

到达并**等待**其他所有参与者到达当前阶段。效果类似 `CyclicBarrier.await()`。

- **返回**: `int` — 新的阶段号（如果已终止则返回负值）

### arriveAndDeregister

```java
public int arriveAndDeregister()
```

到达并**注销**当前参与者，不再参与后续阶段。

- **返回**: `int` — 到达时的阶段号

### getPhase

```java
public final int getPhase()
```

返回当前阶段号。初始阶段号为 0，每完成一个阶段加 1。如果 Phaser 已终止则返回负值。

- **返回**: `int` — 当前阶段号

### getRegisteredParties

```java
public int getRegisteredParties()
```

返回当前注册的参与者数量。

- **返回**: `int` — 注册参与者数量

### isTerminated

```java
public boolean isTerminated()
```

检查 Phaser 是否已终止。

- **返回**: `boolean` — 是否已终止

## 测试

### 基本阶段同步

- 描述: 主线程和子线程在两个阶段进行同步
- 断言: 所有线程正确到达每个阶段

```java
// 方法体开始
System.out.println("=== 基本阶段同步 ===");
Phaser phaser = new Phaser(2);  // 主线程 + 子线程
int[] phaseLog = new int[4];
Thread t = new Thread(() -> {
    // 第一阶段
    phaseLog[0] = phaser.getPhase();
    phaser.arriveAndAwaitAdvance();
    // 第二阶段
    phaseLog[1] = phaser.getPhase();
    phaser.arriveAndAwaitAdvance();
});
t.start();
// 主线程也等待
phaseLog[2] = phaser.getPhase();
phaser.arriveAndAwaitAdvance();
phaseLog[3] = phaser.getPhase();
phaser.arriveAndAwaitAdvance();
t.join();
assertEquals(0, phaseLog[0]);  // 子线程第一阶段
assertEquals(0, phaseLog[2]);  // 主线程第一阶段
assertEquals(1, phaseLog[1]);  // 子线程第二阶段
assertEquals(1, phaseLog[3]);  // 主线程第二阶段
System.out.println("所有线程完成两阶段同步");
System.out.println("最终阶段: " + phaser.getPhase());
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### 动态注册

- 描述: 使用 `register` 动态添加参与者
- 断言: 注册后参与者数量增加

```java
// 方法体开始
System.out.println("=== 动态注册 ===");
Phaser phaser = new Phaser();
assertEquals(0, phaser.getRegisteredParties());
phaser.register();
assertEquals(1, phaser.getRegisteredParties());
phaser.bulkRegister(3);
assertEquals(4, phaser.getRegisteredParties());
System.out.println("注册后参与者数量: " + phaser.getRegisteredParties());
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### arrive 非阻塞

- 描述: 使用 `arrive` 和 `arriveAndDeregister` 实现非阻塞的到达
- 断言: arrive 后其他到达可推进阶段

```java
// 方法体开始
System.out.println("=== arrive 非阻塞 ===");
Phaser phaser = new Phaser(3);  // 3 个参与者
phaser.arrive();                // 参与者 1 到达，不等待
assertEquals(0, phaser.getPhase());
phaser.arrive();                // 参与者 2 到达，不等待
assertEquals(0, phaser.getPhase());  // 还没推进
phaser.arrive();                // 第三个到达，推进到下一阶段
assertEquals(1, phaser.getPhase());
System.out.println("三次 arrive 后阶段: " + phaser.getPhase());
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### 多阶段计算

- 描述: 使用 Phaser 协调 3 个线程完成 3 个阶段的计算
- 断言: 所有线程完成所有阶段

```java
// 方法体开始
System.out.println("=== 多阶段计算 ===");
int N = 3;
Phaser phaser = new Phaser(N);
List<Integer> results = new ArrayList<>();
List<Thread> threads = new ArrayList<>();
for (int i = 0; i < N; i++) {
    final int id = i;
    Thread t = new Thread(() -> {
        for (int phase = 0; phase < 3; phase++) {
            ThreadLocalRandom.current().nextInt();  // 模拟计算
            int arrivePhase = phaser.arriveAndAwaitAdvance();
        }
        synchronized (results) {
            results.add(id);
        }
    });
    t.start();
    threads.add(t);
}
for (Thread t : threads) t.join();
assertEquals(N, results.size());
System.out.println("所有线程完成 3 阶段计算");
System.out.println("=== 测试通过 ===");
// 方法体结束
```
