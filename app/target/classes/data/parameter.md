---
name: Parameter
package: java.lang.reflect
order: 143
---

## 介绍

`java.lang.reflect.Parameter` 是 Java 8 新增的**方法参数反射类**。它代表方法或构造函数的单个参数，提供了获取参数名、类型、修饰符等信息的能力。

Parameter 的核心特点：
- **参数名称**：`getName()` 获取参数名（默认是 `arg0`，需要 `-parameters` 编译选项）
- **参数类型**：`getType()` 获取参数类型
- **参数化类型**：`getParameterizedType()` 获取泛型参数类型
- **注解支持**：`getAnnotations()` 获取参数注解
- **与 Executable 配合**：通过 `Executable.getParameters()` 获取

## 方法

### getName

```java
public String getName()
```

返回参数名称。默认返回 `argN`，需要 `-parameters` 编译选项才能获取源码参数名。

### getType

```java
public Class<?> getType()
```

返回参数类型。

### getParameterizedType

```java
public Type getParameterizedType()
```

返回参数的泛型类型。

### getModifiers

```java
public int getModifiers()
```

返回参数修饰符。

### isVarArgs

方法所属的可执行对象是否为可变参数。

## 测试

### 获取参数信息

- 描述: 通过 Parameter 获取方法参数信息
- 断言: 参数类型正确

```java
// 方法体开始
System.out.println("=== 获取参数 ===");
Method method = String.class.getMethod("valueOf", int.class);
Parameter[] params = method.getParameters();
assertEquals(1, params.length);
Parameter param = params[0];
assertEquals(int.class, param.getType());
System.out.println("参数类型: " + param.getType());
System.out.println("参数名: " + param.getName());
System.out.println("=== 测试通过 ===");
// 方法体结束
```
