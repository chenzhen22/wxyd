---
name: Modifier
package: java.lang.reflect
order: 244
---

## 介绍

`java.lang.reflect.Modifier` 是**修饰符解析工具类**，用于解码类、方法、字段的访问修饰符。

## 方法

### isPublic / isPrivate / isProtected / isStatic / isFinal

```java
public static boolean isPublic(int mod)
public static boolean isStatic(int mod)
public static boolean isFinal(int mod)
```

### toString

```java
public static String toString(int mod)
```

返回修饰符的字符串表示。

## 测试

- 描述: 解析方法修饰符
- 断言: 解析结果正确

```java
// 方法体开始
System.out.println("=== Modifier ===");
int mod = String.class.getModifiers();
assertTrue(Modifier.isPublic(mod));
assertFalse(Modifier.isStatic(mod));
assertFalse(Modifier.isFinal(mod));
String modStr = Modifier.toString(mod);
System.out.println("String 类修饰符: " + modStr);
Method method = Integer.class.getMethod("parseInt", String.class);
int methodMod = method.getModifiers();
assertTrue(Modifier.isStatic(methodMod));
assertTrue(Modifier.isPublic(methodMod));
System.out.println("parseInt 修饰符: " + Modifier.toString(methodMod));
System.out.println("=== 测试通过 ===");
// 方法体结束
```
