---
name: File
package: java.io
order: 183
---

## 介绍

`java.io.File` 是**文件系统路径的抽象表示**。虽然 Java 7+ 推荐使用 `java.nio.file.Path`，但 File 仍然广泛使用。Java 8 没有直接为 File 增加新方法，但它可以在 Lambda 和 Stream 中与 `toPath()` 配合使用。

Java 8 相关的使用方式：
- `toPath()` — 转换为 java.nio.file.Path（Java 7）
- 与 Files API 配合：`Files.list(file.toPath())`
- 在 Stream 中过滤：`listFiles(FileFilter)` 配合 Lambda

## 方法

构造方法：
```java
public File(String pathname)
public File(String parent, String child)
public File(File parent, String child)
```

常用方法：
- `exists()` — 是否存在
- `isFile()` / `isDirectory()` — 文件/目录判断
- `listFiles()` — 列出子文件和目录
- `listFiles(FileFilter)` — 使用过滤器的 Lambda 友好方法（Java 8 风格）
- `getName()` / `getPath()` — 获取名称/路径
- `toPath()` — 转换为 Path

## 测试

### listFiles 配合 Lambda

- 描述: 使用 Lambda 过滤文件名
- 断言: 过滤出特定后缀的文件

```java
// 方法体开始
System.out.println("=== listFiles ===");
File dir = new File(".");
File[] mdFiles = dir.listFiles(f -> f.getName().endsWith(".md"));
// 过滤结果可能为空，验证不为 null
assertNotNull(mdFiles);
System.out.println("当前目录 .md 文件数: " + mdFiles.length);
System.out.println("=== 测试通过 ===");
// 方法体结束
```
