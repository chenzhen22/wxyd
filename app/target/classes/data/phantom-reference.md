---
name: PhantomReference
package: java.lang.ref
order: 287
---

## 介绍

`java.lang.ref.PhantomReference` 是**虚引用**类，是最弱的引用类型。无法通过 `get()` 获取被引用的对象，主要用于对象被回收后的跟踪通知。

## 方法

构造方法：
```java
public PhantomReference(T referent, ReferenceQueue<? super T> q)
```

### get

始终返回 null。

### clear / enqueue / isEnqueued

引用管理方法。

## 测试

- 描述: 创建虚引用
- 断言: get 始终返回 null

```java
// 方法体开始
System.out.println("=== PhantomReference ===");
ReferenceQueue<Object> queue = new ReferenceQueue<>();
Object obj = new Object();
PhantomReference<Object> pr = new PhantomReference<>(obj, queue);
assertNull(pr.get());  // 虚引用 get() 始终为 null
obj = null;
System.gc();
Reference<?> ref = queue.poll();
assertNotNull(ref);
System.out.println("虚引用已被入队");
System.out.println("=== 测试通过 ===");
// 方法体结束
```
