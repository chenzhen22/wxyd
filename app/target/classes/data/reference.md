---
name: Reference
package: java.lang.ref
order: 279
---

## 介绍

`java.lang.ref.Reference` 是**引用抽象类**，是软引用、弱引用、虚引用的基类，Java 8 中可与 Lambda 和函数式风格配合使用。

Java 中的引用类型：
- **强引用**：普通引用，不会被 GC 回收
- **软引用**（`SoftReference`）：内存不足时回收
- **弱引用**（`WeakReference`）：下次 GC 时回收
- **虚引用**（`PhantomReference`）：最弱，用于跟踪对象回收

## 方法

### get

```java
public T get()
```

获取引用的对象（软/弱引用可用，虚引用始终返回 null）。

### enqueue / isEnqueued / clear

引用队列管理方法。

## 测试

- 描述: 使用弱引用
- 断言: 弱引用可被 GC 回收

```java
// 方法体开始
System.out.println("=== WeakReference ===");
Object obj = new Object();
WeakReference<Object> wr = new WeakReference<>(obj);
assertSame(obj, wr.get());
obj = null;  // 移除强引用
System.gc();
assertNull(wr.get());
System.out.println("弱引用已被 GC 回收");
System.out.println("=== 测试通过 ===");
// 方法体结束
```
