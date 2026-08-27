---
name: SoftReference
package: java.lang.ref
order: 286
---

## 介绍

`java.lang.ref.SoftReference` 是**软引用**类，指向的对象在内存不足时才被 GC 回收，常用于内存敏感的缓存实现。

## 方法

构造方法：
```java
public SoftReference(T referent)
public SoftReference(T referent, ReferenceQueue<? super T> q)
```

### get

获取引用对象。

## 测试

- 描述: 使用软引用创建缓存
- 断言: 软引用对象可访问

```java
// 方法体开始
System.out.println("=== SoftReference ===");
Object cachedData = new Object();
SoftReference<Object> sr = new SoftReference<>(cachedData);
assertNotNull(sr.get());
assertSame(cachedData, sr.get());
// 软引用在内存充足时不会被回收
cachedData = null;
System.gc();
// 软引用通常不会在这次 GC 中被回收（除非内存紧张）
System.out.println("软引用测试完成");
System.out.println("=== 测试通过 ===");
// 方法体结束
```
