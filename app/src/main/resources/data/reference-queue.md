---
name: ReferenceQueue
package: java.lang.ref
order: 341
---

## 介绍

`java.lang.ref.ReferenceQueue` 是**引用队列**类，当引用对象所指向的对象被 GC 回收后，引用对象会被加入队列中。

## 方法

构造方法：
```java
public ReferenceQueue()
```

### poll / remove

```java
public Reference<? extends T> poll()
public Reference<? extends T> remove(long timeout) throws InterruptedException
```

## 测试

- 描述: 使用 ReferenceQueue 跟踪 GC
- 断言: 回收后入队

```java
// 方法体开始
System.out.println("=== ReferenceQueue ===");
ReferenceQueue<Object> queue = new ReferenceQueue<>();
Object obj = new Object();
WeakReference<Object> wr = new WeakReference<>(obj, queue);
assertNull(queue.poll());
obj = null;
System.gc();
Reference<?> ref = queue.poll();
assertNotNull(ref);
assertSame(wr, ref);
System.out.println("引用已入队");
System.out.println("=== 测试通过 ===");
// 方法体结束
```
