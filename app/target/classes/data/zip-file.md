---
name: ZipFile
package: java.util.zip
order: 124
---

## 介绍

`java.util.zip.ZipFile` 用于读取 ZIP 文件。Java 8 为其新增了 `stream()` 方法，可以方便地遍历 ZIP 文件中的所有条目。

Java 8 新增的方法：
- `stream()` — 返回 ZIP 文件条目流的顺序 Stream

## 方法

### stream

```java
public Stream<? extends ZipEntry> stream()
```

返回 ZIP 文件中的所有条目流。

- **返回**: `Stream<? extends ZipEntry>` — ZIP 条目流

### entries

```java
public Enumeration<? extends ZipEntry> entries()
```

返回 ZIP 文件条目的枚举（Java 8 前的方式）。

## 测试

### stream

- 描述: 使用 stream() 遍历 ZIP 文件条目
- 断言: 流不为空，条目名称正确

```java
// 方法体开始
System.out.println("=== ZipFile.stream ===");
// 创建临时 ZIP 文件用于测试
Path zipPath = Paths.get(System.getProperty("java.io.tmpdir"), "test-javaapi.zip");
try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipPath.toFile()))) {
    zos.putNextEntry(new ZipEntry("hello.txt"));
    zos.write("Hello World".getBytes());
    zos.closeEntry();
    zos.putNextEntry(new ZipEntry("sub/data.txt"));
    zos.write("data".getBytes());
    zos.closeEntry();
}
// 使用 ZipFile.stream() 遍历
try (ZipFile zf = new ZipFile(zipPath.toFile())) {
    List<String> names = zf.stream()
            .map(ZipEntry::getName)
            .sorted()
            .collect(Collectors.toList());
    assertEquals(2, names.size());
    assertEquals("hello.txt", names.get(0));
    assertEquals("sub/data.txt", names.get(1));
    System.out.println("ZIP 条目: " + names);
}
Files.delete(zipPath);
System.out.println("=== 测试通过 ===");
// 方法体结束
```
