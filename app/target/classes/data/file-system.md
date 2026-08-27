---
name: FileSystem
package: java.nio.file
order: 355
---

## 介绍

`java.nio.file.FileSystem` 是**文件系统抽象类**，提供对文件系统的访问接口。通常通过 `FileSystems.getDefault()` 获取。

## 方法

### getRootDirectories / getSeparator / isReadOnly / isOpen

### getPath / getFileStores / supportedFileAttributeViews

## 测试

- 描述: 获取默认文件系统信息
- 断言: 信息获取成功

```java
// 方法体开始
System.out.println("=== FileSystem ===");
FileSystem fs = FileSystems.getDefault();
assertNotNull(fs);
String sep = fs.getSeparator();
assertTrue(sep.equals("/") || sep.equals("\\"));
System.out.println("文件系统: " + fs);
System.out.println("分隔符: " + sep);
System.out.println("只读: " + fs.isReadOnly());
fs.getRootDirectories().forEach(p -> System.out.println("  根: " + p));
System.out.println("=== 测试通过 ===");
// 方法体结束
```
