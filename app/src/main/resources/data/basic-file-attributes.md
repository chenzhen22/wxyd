---
name: BasicFileAttributes
package: java.nio.file.attribute
order: 359
---

## 介绍

`java.nio.file.attribute.BasicFileAttributes` 是**基本文件属性**接口，提供文件的基本元数据信息。

## 方法

### size / creationTime / lastModifiedTime / lastAccessTime

### isRegularFile / isDirectory / isSymbolicLink / isOther

### fileKey

## 测试

- 描述: 获取文件属性
- 断言: 属性获取成功

```java
// 方法体开始
System.out.println("=== BasicFileAttributes ===");
Path pom = Paths.get("pom.xml");
if (Files.exists(pom)) {
    BasicFileAttributes attr = Files.readAttributes(pom, BasicFileAttributes.class);
    assertTrue(attr.size() > 0);
    assertTrue(attr.isRegularFile());
    assertFalse(attr.isDirectory());
    assertNotNull(attr.creationTime());
    assertNotNull(attr.lastModifiedTime());
    System.out.println("文件大小: " + attr.size() + " bytes");
    System.out.println("修改时间: " + attr.lastModifiedTime());
} else {
    System.out.println("pom.xml 不可用");
}
System.out.println("=== 测试通过 ===");
// 方法体结束
```
