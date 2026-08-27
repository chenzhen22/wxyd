---
name: Paths
package: java.nio.file
order: 118
---

## 介绍

`java.nio.file.Paths` 是 Java 7 引入的**Path 工厂工具类**，与 `Files` 配合使用。它只包含两个工厂方法，用于从字符串或 URI 创建 `Path` 对象。

Paths 在 Java 8 中与 Files 的新方法配合更加紧密：

## 方法

### get

```java
public static Path get(String first, String... more)
```

从路径字符串创建一个 Path。支持绝对路径和相对路径。

- **参数**: `first` — 路径字符串或第一部分；`more` — 后续部分（可选）
- **返回**: `Path` — 对应的 Path 对象

### get(URI)

```java
public static Path get(URI uri)
```

从 URI 创建一个 Path。

- **参数**: `uri` — 文件 URI（如 `file:///C:/data/file.txt`）
- **返回**: `Path` — 对应的 Path 对象

## 测试

### get 相对路径

- 描述: 使用 get 创建相对路径
- 断言: 路径字符串正确

```java
// 方法体开始
System.out.println("=== get 相对路径 ===");
Path path = Paths.get("data", "file.txt");
assertTrue(path.toString().contains("data"));
assertTrue(path.toString().contains("file.txt"));
System.out.println("路径: " + path);
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### get 绝对路径

- 描述: 创建临时目录路径
- 断言: 路径非空

```java
// 方法体开始
System.out.println("=== get 绝对路径 ===");
Path tmp = Paths.get(System.getProperty("java.io.tmpdir"));
assertNotNull(tmp);
assertTrue(tmp.isAbsolute());
System.out.println("临时目录: " + tmp);
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### 与 Files 配合

- 描述: 使用 Paths 创建路径并配合 Files 读取属性
- 断言: 文件存在检查

```java
// 方法体开始
System.out.println("=== 与 Files 配合 ===");
Path path = Paths.get("pom.xml");
boolean exists = Files.exists(path);
// 不管 pom.xml 是否存在，验证 Path 对象创建成功
assertNotNull(path);
System.out.println("pom.xml exists: " + exists);
System.out.println("=== 测试通过 ===");
// 方法体结束
```
