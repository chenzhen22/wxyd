---
name: Class
package: java.lang
order: 238
---

## 介绍

`java.lang.Class<T>` 是**反射的核心入口**，表示正在运行的 Java 应用程序中的类和接口。Java 8 中配合 Lambda 和方法引用一起使用更加灵活。

## 方法

### forName

```java
public static Class<?> forName(String className) throws ClassNotFoundException
```

根据全限定名加载类。

### newInstance

```java
public T newInstance() throws InstantiationException, IllegalAccessException
```

创建新实例（已弃用，推荐 Constructor）。

### getMethod / getDeclaredMethod

```java
public Method getMethod(String name, Class<?>... parameterTypes)
public Method getDeclaredMethod(String name, Class<?>... parameterTypes)
```

获取方法。

### getMethods / getDeclaredMethods

获取所有方法。

## 测试

- 描述: 获取类的元信息
- 断言: 反射操作正确

```java
// 方法体开始
System.out.println("=== Class ===");
Class<String> stringClass = String.class;
assertEquals("java.lang.String", stringClass.getName());
Method method = stringClass.getMethod("length");
assertEquals("length", method.getName());
assertEquals(int.class, method.getReturnType());
System.out.println("类: " + stringClass.getName());
System.out.println("方法: " + method.getName() + " -> " + method.getReturnType());
System.out.println("=== 测试通过 ===");
// 方法体结束
```
