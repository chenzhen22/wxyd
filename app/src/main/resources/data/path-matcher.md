---
name: PathMatcher
package: java.nio.file
order: 360
---

## 介绍

`java.nio.file.PathMatcher` 是**路径匹配器**接口，用于匹配文件路径（支持 glob 和 regex 语法）。

## 方法

### matches

```java
public boolean matches(Path path)
```

## 测试

- 描述: 使用 glob 匹配文件
- 断言: 匹配正确

```java
// 方法体开始
System.out.println("=== PathMatcher ===");
FileSystem fs = FileSystems.getDefault();
PathMatcher globMatcher = fs.getPathMatcher("glob:*.{java,xml}");
assertTrue(globMatcher.matches(Paths.get("pom.xml")));
assertFalse(globMatcher.matches(Paths.get("test.txt")));
PathMatcher regexMatcher = fs.getPathMatcher("regex:.*\\.md");
assertTrue(regexMatcher.matches(Paths.get("readme.md")));
assertFalse(regexMatcher.matches(Paths.get("readme.txt")));
System.out.println("glob *.xml match pom.xml: " + globMatcher.matches(Paths.get("pom.xml")));
System.out.println("regex .md match readme.md: " + regexMatcher.matches(Paths.get("readme.md")));
System.out.println("=== 测试通过 ===");
// 方法体结束
```
