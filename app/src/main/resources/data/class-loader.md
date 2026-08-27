---
name: ClassLoader
package: java.lang
order: 240
---

## 介绍

`java.lang.ClassLoader` 是**类加载器**类，负责加载 Java 类到 JVM 中。

## 方法

### loadClass

```java
public Class<?> loadClass(String name) throws ClassNotFoundException
```

加载指定名称的类。

### getParent

```java
public final ClassLoader getParent()
```

获取父类加载器。

### getSystemClassLoader

```java
public static ClassLoader getSystemClassLoader()
```

获取系统类加载器。

### getResource / getResourceAsStream

```java
public URL getResource(String name)
public InputStream getResourceAsStream(String name)
```

加载资源文件。

## 测试

- 描述: 获取系统类加载器
- 断言: 不为 null

```java
// 方法体开始
System.out.println("=== ClassLoader ===");
ClassLoader cl = ClassLoader.getSystemClassLoader();
assertNotNull(cl);
assertNotNull(cl.getParent());
Class<?> cls = cl.loadClass("java.lang.String");
assertEquals(String.class, cls);
System.out.println("类加载器: " + cl);
System.out.println("父加载器: " + cl.getParent());
System.out.println("=== 测试通过 ===");
// 方法体结束
```
