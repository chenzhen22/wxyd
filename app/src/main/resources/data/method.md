---
name: Method
package: java.lang.reflect
order: 243
---

## 介绍

`java.lang.reflect.Method` 代表类的**方法**，提供动态调用方法的功能。

## 方法

### invoke

```java
public Object invoke(Object obj, Object... args) throws IllegalAccessException, InvocationTargetException
```

调用底层方法。

### getReturnType / getParameterTypes / getName

获取方法信息。

## 测试

- 描述: 反射调用方法
- 断言: 调用结果正确

```java
// 方法体开始
System.out.println("=== Method ===");
Method method = String.class.getMethod("toUpperCase");
String result = (String) method.invoke("hello");
assertEquals("HELLO", result);
Method parseInt = Integer.class.getMethod("parseInt", String.class);
int num = (int) parseInt.invoke(null, "42");
assertEquals(42, num);
System.out.println("反射调用 toUpperCase: " + result);
System.out.println("反射调用 parseInt: " + num);
System.out.println("=== 测试通过 ===");
// 方法体结束
```
