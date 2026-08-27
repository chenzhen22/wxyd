---
name: FileVisitor
package: java.nio.file
order: 357
---

## 介绍

`java.nio.file.FileVisitor` 是**文件遍历访问者**接口，用于在 `Files.walkFileTree` 中自定义遍历行为。Java 8 中 `SimpleFileVisitor` 提供了默认实现。

## 方法

### preVisitDirectory / postVisitDirectory / visitFile / visitFileFailed

## 测试

- 描述: 使用 SimpleFileVisitor 遍历目录
- 断言: 遍历完成

```java
// 方法体开始
System.out.println("=== FileVisitor ===");
Path tmpDir = Files.createTempDirectory("visitor-");
Path subFile = Files.createTempFile(tmpDir, "test-", ".txt");
List<Path> visited = new ArrayList<>();
Files.walkFileTree(tmpDir, new SimpleFileVisitor<Path>() {
    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
        visited.add(file.getFileName());
        return FileVisitResult.CONTINUE;
    }
});
assertEquals(1, visited.size());
// 清理
try (Stream<Path> walk = Files.walk(tmpDir)) {
    walk.sorted(Comparator.reverseOrder()).forEach(p -> {
        try { Files.delete(p); } catch (Exception e) {}
    });
}
System.out.println("FileVisitor 遍历完成，文件数: " + visited.size());
System.out.println("=== 测试通过 ===");
// 方法体结束
```
