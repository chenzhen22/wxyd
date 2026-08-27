---
name: SimpleFileVisitor
package: java.nio.file
order: 358
---

## 介绍

`java.nio.file.SimpleFileVisitor` 是 `FileVisitor` 接口的**简单实现**，提供了默认行为（继续遍历），方便子类重写。

## 测试

- 描述: 使用 SimpleFileVisitor
- 断言: 遍历工作

```java
// 方法体开始
System.out.println("=== SimpleFileVisitor ===");
Path tmpDir = Files.createTempDirectory("simple-");
Files.createTempFile(tmpDir, "f-", ".txt");
int[] count = {0};
Files.walkFileTree(tmpDir, new SimpleFileVisitor<Path>() {
    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
        count[0]++;
        return FileVisitResult.CONTINUE;
    }
});
assertEquals(1, count[0]);
// 清理
try (Stream<Path> walk = Files.walk(tmpDir)) {
    walk.sorted(Comparator.reverseOrder()).forEach(p -> {
        try { Files.delete(p); } catch (Exception e) {}
    });
}
System.out.println("SimpleFileVisitor 遍历: " + count[0] + " 个文件");
System.out.println("=== 测试通过 ===");
// 方法体结束
```
