---
name: WeakReference
package: java.lang.ref
order: 285
---

## 介绍

`java.lang.ref.WeakReference` 是**弱引用**类，指向的对象只能在下次 GC 之前存活，常与 `WeakHashMap` 配合使用。

## 方法

构造方法：
```java
public WeakReference(T referent)
public WeakReference(T referent, ReferenceQueue<? super T> q)
```

### get

获取引用对象（如果尚未被 GC 回收）。

## 测试

- 描述: 弱引用自动回收
- 断言: GC 后弱引用失效

```java
// 方法体开始
System.out.println("=== WeakReference ===");
ReferenceQueue<Object> queue = new ReferenceQueue<>();
Object data = new Object();
WeakReference<Object> wr = new WeakReference<>(data, queue);
assertEquals(data, wr.get());
data = null;
System.gc();
Reference<?> ref = queue.poll();
assertNotNull(ref);
assertNull(wr.get());
System.out.println("弱引用已被回收");
System.out.println("=== 测试通过 ===");
// 方法体结束
```
