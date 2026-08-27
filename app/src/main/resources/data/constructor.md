---
name: Constructor
package: java.lang.reflect
order: 241
---

## 介绍

`java.lang.reflect.Constructor<T>` 代表类的**构造方法**。Java 8 中可以通过 `Executable` 父类获取参数信息。

## 方法

### newInstance

```java
public T newInstance(Object... initargs) throws InstantiationException, IllegalAccessException, InvocationTargetException
```

创建新实例。

### getParameterTypes / getParameterCount

获取参数信息。

## 测试

- 描述: 使用反射创建对象
- 断言: 创建成功

```java
// 方法体开始
System.out.println("=== Constructor ===");
Constructor<StringBuilder> constructor = StringBuilder.class.getConstructor(int.class);
StringBuilder sb = constructor.newInstance(16);
assertNotNull(sb);
assertEquals(16, sb.capacity());
System.out.println("通过 Constructor 创建 StringBuilder 成功");
System.out.println("=== 测试通过 ===");
// 方法体结束
```
