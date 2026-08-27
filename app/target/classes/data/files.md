---
name: Files
package: java.nio.file
order: 19
---

## 介绍

`java.nio.file.Files` 是 Java 7 引入的文件操作工具类，包含对文件和目录操作的静态方法。`java.nio.file.Paths` 用于创建 `Path` 对象。

Java 8 为 `Files` 新增了 `lines`、`list`、`walk`、`find` 等 Stream API 方法。

常见用途：
- **读写文件**：`readAllBytes`、`readAllLines`、`write`
- **文件信息**：`exists`、`isDirectory`、`size`、`getLastModifiedTime`
- **目录操作**：`createDirectories`、`list`
- **文件复制/移动**：`copy`、`move`、`delete`
- **Stream 操作**：`lines`、`list`、`walk`

## 方法

### Paths.get

```java
public static Path get(String first, String... more)
```

将路径字符串转换为 `Path` 对象。

- **参数**: `first` — 路径字符串；`more` — 附加路径
- **返回**: `Path`

### exists

```java
public static boolean exists(Path path, LinkOption... options)
```

判断文件或目录是否存在。

- **参数**: `path` — 路径
- **返回**: `boolean`

### isDirectory

```java
public static boolean isDirectory(Path path, LinkOption... options)
```

判断路径是否为目录。

- **参数**: `path` — 路径
- **返回**: `boolean`

### size

```java
public static long size(Path path) throws IOException
```

获取文件大小（字节数）。

- **参数**: `path` — 文件路径
- **返回**: `long` — 文件大小
- **异常**: `IOException`

### readAllBytes

```java
public static byte[] readAllBytes(Path path) throws IOException
```

读取文件所有字节。

- **参数**: `path` — 文件路径
- **返回**: `byte[]`
- **异常**: `IOException`

### readAllLines

```java
public static List<String> readAllLines(Path path) throws IOException
```

读取文件所有行。

- **参数**: `path` — 文件路径
- **返回**: `List<String>`
- **异常**: `IOException`

### write

```java
public static Path write(Path path, byte[] bytes, OpenOption... options) throws IOException
```

将字节写入文件。

- **参数**: `path` — 文件路径；`bytes` — 字节数组
- **返回**: `Path`
- **异常**: `IOException`

### lines

```java
public static Stream<String> lines(Path path) throws IOException
```

以 Stream 方式按行读取文件（Java 8 新增）。

- **参数**: `path` — 文件路径
- **返回**: `Stream<String>`
- **异常**: `IOException`

### list

```java
public static Stream<Path> list(Path dir) throws IOException
```

返回目录中的条目流（Java 8 新增）。

- **参数**: `dir` — 目录路径
- **返回**: `Stream<Path>`
- **异常**: `IOException`

### walk

```java
public static Stream<Path> walk(Path start, int maxDepth, FileVisitOption... options) throws IOException
```

递归遍历目录树返回 Stream（Java 8 新增）。

- **参数**: `start` — 起始目录；`maxDepth` — 最大深度
- **返回**: `Stream<Path>`
- **异常**: `IOException`

### find

```java
public static Stream<Path> find(Path start, int maxDepth, BiPredicate<Path, BasicFileAttributes> matcher, FileVisitOption... options) throws IOException
```

查找匹配指定条件的文件（Java 8 新增）。

- **参数**: `start` — 起始目录；`maxDepth` — 最大深度；`matcher` — 匹配条件
- **返回**: `Stream<Path>`
- **异常**: `IOException`

### createDirectories

```java
public static Path createDirectories(Path dir, FileAttribute<?>... attrs) throws IOException
```

创建目录（包括所有不存在的父目录）。

- **参数**: `dir` — 目录路径
- **返回**: `Path`
- **异常**: `IOException`

### copy

```java
public static Path copy(Path source, Path target, CopyOption... options) throws IOException
```

复制文件或目录。

- **参数**: `source` — 源路径；`target` — 目标路径
- **返回**: `Path`
- **异常**: `IOException`

### move

```java
public static Path move(Path source, Path target, CopyOption... options) throws IOException
```

移动或重命名文件。

- **参数**: `source` — 源路径；`target` — 目标路径
- **返回**: `Path`
- **异常**: `IOException`

### delete

```java
public static void delete(Path path) throws IOException
```

删除文件或空目录。

- **参数**: `path` — 路径
- **异常**: `IOException`

### createTempFile

```java
public static Path createTempFile(String prefix, String suffix, FileAttribute<?>... attrs) throws IOException
```

在默认临时目录创建临时文件。

- **参数**: `prefix` — 前缀；`suffix` — 后缀
- **返回**: `Path`
- **异常**: `IOException`

## 测试

### Paths.get

- 描述: 使用 Paths.get 创建路径
- 断言: 路径字符串正确

```java
// 方法体开始
System.out.println("=== Paths.get ===");
java.nio.file.Path path = java.nio.file.Paths.get("test.txt");
System.out.println("路径: " + path);
assertNotNull(path);
assertEquals("test.txt", path.toString());
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### size

- 描述: 获取文件大小
- 断言: 临时文件大小为 0

```java
// 方法体开始
System.out.println("=== size ===");
java.nio.file.Path tempFile = java.nio.file.Files.createTempFile("size-", ".txt");
assertEquals(0, java.nio.file.Files.size(tempFile));
java.nio.file.Files.write(tempFile, "hello".getBytes("UTF-8"));
assertEquals(5, java.nio.file.Files.size(tempFile));
java.nio.file.Files.delete(tempFile);
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### readAllBytes

- 描述: 读取文件所有字节
- 断言: 读取的字节与写入的一致

```java
// 方法体开始
System.out.println("=== readAllBytes ===");
byte[] content = "readAllBytes test".getBytes("UTF-8");
java.nio.file.Path tempFile = java.nio.file.Files.createTempFile("read-", ".txt");
java.nio.file.Files.write(tempFile, content);
byte[] readBytes = java.nio.file.Files.readAllBytes(tempFile);
assertArrayEquals(content, readBytes);
java.nio.file.Files.delete(tempFile);
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### write

- 描述: 写入文件
- 断言: 写入后文件存在且内容正确

```java
// 方法体开始
System.out.println("=== write ===");
byte[] data = "write test data".getBytes("UTF-8");
java.nio.file.Path tempFile = java.nio.file.Files.createTempFile("write-", ".txt");
java.nio.file.Path result = java.nio.file.Files.write(tempFile, data);
assertEquals(tempFile, result);
assertArrayEquals(data, java.nio.file.Files.readAllBytes(tempFile));
java.nio.file.Files.delete(tempFile);
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### delete

- 描述: 删除文件
- 断言: 删除后文件不存在

```java
// 方法体开始
System.out.println("=== delete ===");
java.nio.file.Path tempFile = java.nio.file.Files.createTempFile("del-", ".txt");
assertTrue(java.nio.file.Files.exists(tempFile));
java.nio.file.Files.delete(tempFile);
assertFalse(java.nio.file.Files.exists(tempFile));
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### exists

- 描述: 检查文件是否存在
- 断言: 该文件存在时返回 true

```java
// 方法体开始
System.out.println("=== exists ===");
java.nio.file.Path path = java.nio.file.Paths.get("src/main/resources/data/files.md");
boolean exists = java.nio.file.Files.exists(path);
System.out.println("文件是否存在: " + exists);
assertTrue(exists);
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### notExists

- 描述: 检查不存在的文件
- 断言: 不存在文件返回 false

```java
// 方法体开始
System.out.println("=== notExists ===");
java.nio.file.Path path = java.nio.file.Paths.get("nonexistent-file-xyz.txt");
boolean exists = java.nio.file.Files.exists(path);
System.out.println("不存在的文件: " + exists);
assertFalse(exists);
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### isDirectory

- 描述: 检查路径是否为目录
- 断言: src 目录是目录

```java
// 方法体开始
System.out.println("=== isDirectory ===");
java.nio.file.Path path = java.nio.file.Paths.get("src");
boolean isDir = java.nio.file.Files.isDirectory(path);
System.out.println("src 是目录: " + isDir);
assertTrue(isDir);
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### createTempFile

- 描述: 创建临时文件
- 断言: 临时文件创建成功且存在

```java
// 方法体开始
System.out.println("=== createTempFile ===");
java.nio.file.Path tempFile = java.nio.file.Files.createTempFile("test-", ".txt");
assertNotNull(tempFile);
System.out.println("临时文件: " + tempFile);
// 清理
java.nio.file.Files.delete(tempFile);
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### readAllLines

- 描述: 按行读取文件
- 断言: 文件行数正确

```java
// 方法体开始
System.out.println("=== readAllLines ===");
String line1 = "第一行";
String line2 = "第二行";
String line3 = "第三行";
String content = line1 + "\n" + line2 + "\n" + line3;
java.nio.file.Path tempFile = java.nio.file.Files.createTempFile("lines-", ".txt");
java.nio.file.Files.write(tempFile, content.getBytes("UTF-8"));
java.util.List<String> lines = java.nio.file.Files.readAllLines(tempFile);
System.out.println("行数: " + lines.size());
assertEquals(3, lines.size());
assertEquals(line1, lines.get(0));
assertEquals(line2, lines.get(1));
assertEquals(line3, lines.get(2));
// 清理
java.nio.file.Files.delete(tempFile);
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### copy

- 描述: 复制文件
- 断言: 复制后目标文件存在且内容一致

```java
// 方法体开始
System.out.println("=== copy ===");
byte[] content = "复制测试".getBytes("UTF-8");
java.nio.file.Path source = java.nio.file.Files.createTempFile("src-", ".txt");
java.nio.file.Files.write(source, content);
java.nio.file.Path target = java.nio.file.Paths.get(source.toString() + ".copy");
java.nio.file.Files.copy(source, target, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
assertTrue(java.nio.file.Files.exists(target));
assertArrayEquals(content, java.nio.file.Files.readAllBytes(target));
System.out.println("文件复制成功");
// 清理
java.nio.file.Files.delete(source);
java.nio.file.Files.delete(target);
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### move

- 描述: 移动文件
- 断言: 移动后源文件不存在，目标文件存在

```java
// 方法体开始
System.out.println("=== move ===");
java.nio.file.Path source = java.nio.file.Files.createTempFile("move-", ".txt");
java.nio.file.Files.write(source, "移动测试".getBytes("UTF-8"));
java.nio.file.Path target = java.nio.file.Paths.get(source.toString() + ".moved");
java.nio.file.Files.move(source, target, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
assertFalse(java.nio.file.Files.exists(source));
assertTrue(java.nio.file.Files.exists(target));
System.out.println("文件移动成功");
// 清理
java.nio.file.Files.delete(target);
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### createDirectories

- 描述: 创建多级目录
- 断言: 目录创建成功且存在

```java
// 方法体开始
System.out.println("=== createDirectories ===");
java.nio.file.Path tempDir = java.nio.file.Files.createTempDirectory("parent-");
java.nio.file.Path subDir = tempDir.resolve("sub").resolve("nested");
java.nio.file.Files.createDirectories(subDir);
assertTrue(java.nio.file.Files.exists(subDir));
assertTrue(java.nio.file.Files.isDirectory(subDir));
System.out.println("多级目录创建成功: " + subDir);
// 清理（递归删除测试目录）
java.nio.file.Files.walk(tempDir)
    .sorted(java.util.Comparator.reverseOrder())
    .forEach(p -> { try { java.nio.file.Files.delete(p); } catch (Exception e) {} });
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### lines

- 描述: 使用 Stream 按行读取文件（Java 8）
- 断言: 每行内容正确

```java
// 方法体开始
System.out.println("=== lines ===");
String content = "line1\nline2\nline3";
java.nio.file.Path tempFile = java.nio.file.Files.createTempFile("stream-", ".txt");
java.nio.file.Files.write(tempFile, content.getBytes("UTF-8"));
try (java.util.stream.Stream<String> stream = java.nio.file.Files.lines(tempFile)) {
    java.util.List<String> lines = stream.collect(java.util.stream.Collectors.toList());
    System.out.println("行数: " + lines.size());
    assertEquals(3, lines.size());
    assertEquals("line1", lines.get(0));
    assertEquals("line2", lines.get(1));
    assertEquals("line3", lines.get(2));
}
// 清理
java.nio.file.Files.delete(tempFile);
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### list

- 描述: 使用 Files.list 遍历目录（Java 8）
- 断言: 目录中的文件数大于 0

```java
// 方法体开始
System.out.println("=== list ===");
java.nio.file.Path tmpDir = java.nio.file.Files.createTempDirectory("javatest-");
java.nio.file.Files.createTempFile(tmpDir, "f1-", ".txt");
java.nio.file.Files.createTempFile(tmpDir, "f2-", ".txt");
try (java.util.stream.Stream<java.nio.file.Path> stream = java.nio.file.Files.list(tmpDir)) {
    long count = stream.count();
    assertTrue(count >= 2);
    System.out.println("目录文件数: " + count);
}
// 清理
try (java.util.stream.Stream<java.nio.file.Path> walk = java.nio.file.Files.walk(tmpDir)) {
    walk.sorted(java.util.Comparator.reverseOrder())
        .forEach(p -> { try { java.nio.file.Files.delete(p); } catch (Exception e) {} });
}
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### walk

- 描述: 使用 Files.walk 递归遍历目录树（Java 8）
- 断言: 遍历包含子目录内容

```java
// 方法体开始
System.out.println("=== walk ===");
java.nio.file.Path tmpDir = java.nio.file.Files.createTempDirectory("walktest-");
java.nio.file.Path subDir = tmpDir.resolve("sub");
java.nio.file.Files.createDirectories(subDir);
java.nio.file.Files.createTempFile(subDir, "data-", ".txt");
try (java.util.stream.Stream<java.nio.file.Path> stream = java.nio.file.Files.walk(tmpDir)) {
    java.util.List<java.nio.file.Path> files = stream.collect(java.util.stream.Collectors.toList());
    assertTrue(files.size() >= 3);
    System.out.println("遍历文件数: " + files.size());
}
// 清理
try (java.util.stream.Stream<java.nio.file.Path> walk = java.nio.file.Files.walk(tmpDir)) {
    walk.sorted(java.util.Comparator.reverseOrder())
        .forEach(p -> { try { java.nio.file.Files.delete(p); } catch (Exception e) {} });
}
System.out.println("=== 测试通过 ===");
// 方法体结束
```
