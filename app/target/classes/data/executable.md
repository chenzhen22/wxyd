---
name: Executable
package: java.lang.reflect
order: 140
---

## 介绍

`java.lang.reflect.Executable` 是 Java 8 新增的一个**抽象基类**，它是 `Method` 和 `Constructor` 的公共父类。它集中了方法和构造函数的共同反射能力，特别是在方法参数反射方面。

Executable 的核心特点：
- **统一 Method 和 Constructor**：共享的反射操作方法
- **参数信息**：获取参数数量和 Parameter 对象
- **类型参数**：获取泛型类型参数
- **注解支持**：获取参数注解、类型注解

## 方法

### getParameters

```java
public Parameter[] getParameters()
```

返回形式参数（`Parameter` 对象数组），如果没有参数则返回空数组。

### getParameterCount

```java
public int getParameterCount()
```

返回形式参数的数量。

### isVarArgs

```java
public boolean isVarArgs()
```

返回该方法/构造方法是否包含可变参数。

## 测试

### getParameterCount

- 描述: 通过 Executable 获取方法的参数数量
- 断言: String.valueOf(int) 有 1 个参数

```java
// 方法体开始
System.out.println("=== getParameterCount ===");
Method method = String.class.getMethod("valueOf", int.class);
assertTrue(method instanceof Executable);
int count = method.getParameterCount();
assertEquals(1, count);
System.out.println("String.valueOf(int) 参数数量: " + count);
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### isVarArgs

- 描述: 检查方法是否为可变参数
- 断言: String.format 是可变参数

```java
// 方法体开始
System.out.println("=== isVarArgs ===");
Method varArgsMethod = String.class.getMethod("format", String.class, Object[].class);
assertTrue(varArgsMethod.isVarArgs());
Method normalMethod = String.class.getMethod("length");
assertFalse(normalMethod.isVarArgs());
System.out.println("String.format 是可变参数: " + varArgsMethod.isVarArgs());
System.out.println("=== 测试通过 ===");
// 方法体结束
```
