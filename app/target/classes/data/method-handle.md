---
name: MethodHandle
package: java.lang.invoke
order: 283
---

## 介绍

`java.lang.invoke.MethodHandle` 是**方法句柄**类，Java 7 引入，在 Java 8 中与 Lambda 实现密切相关。它是反射调用的轻量级替代方案。

## 方法

### lookup / findVirtual / findStatic

```java
public static MethodHandles.Lookup lookup()
public MethodHandle findVirtual(Class<?> refc, String name, MethodType type)
```

### invoke / invokeExact

```java
public Object invoke(Object... args) throws Throwable
```

## 测试

- 描述: 使用方法句柄调用方法
- 断言: 调用结果正确

```java
// 方法体开始
System.out.println("=== MethodHandle ===");
MethodHandles.Lookup lookup = MethodHandles.lookup();
MethodType mt = MethodType.methodType(int.class);
MethodHandle length = lookup.findVirtual(String.class, "length", mt);
int len = (int) length.invoke("Hello");
assertEquals(5, len);
MethodHandle concat = lookup.findVirtual(String.class, "concat", MethodType.methodType(String.class, String.class));
String result = (String) concat.invoke("Hello ", "World");
assertEquals("Hello World", result);
System.out.println("MethodHandle 调用: length='Hello'=" + len);
System.out.println("MethodHandle 调用: concat=" + result);
System.out.println("=== 测试通过 ===");
// 方法体结束
```
