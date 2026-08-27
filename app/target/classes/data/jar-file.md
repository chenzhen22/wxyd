---
name: JarFile
package: java.util.jar
order: 274
---

## 介绍

`java.util.jar.JarFile` 是 **JAR 文件读取类**，用于读取 Java ARchive 文件。

## 方法

构造方法：
```java
public JarFile(File file) throws IOException
```

### entries / stream

```java
public Enumeration<JarEntry> entries()
public Stream<JarEntry> stream()
```

获取 JAR 条目枚举/流。

### getManifest

获取 MANIFEST.MF。

### getJarEntry / getEntry

获取指定条目。

## 测试

- 描述: 读取 JAR 文件条目
- 断言: JAR 条目不为空

```java
// 方法体开始
System.out.println("=== JarFile ===");
String javaHome = System.getProperty("java.home");
Path rtPath = Paths.get(javaHome, "lib", "rt.jar");
if (!Files.exists(rtPath)) {
    rtPath = Paths.get(javaHome, "lib", "jrt-fs.jar");
}
if (Files.exists(rtPath)) {
    try (JarFile jar = new JarFile(rtPath.toFile())) {
        long count = jar.stream().count();
        assertTrue(count > 0);
        System.out.println("JAR 条目数: " + count);
    }
} else {
    // Java 9+ 使用模块化运行时
    System.out.println("JAR 运行时不可用（模块化 JDK）");
}
System.out.println("=== 测试通过 ===");
// 方法体结束
```
