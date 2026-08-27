---
name: FileLock
package: java.nio.channels
order: 361
---

## 介绍

`java.nio.channels.FileLock` 是**文件锁**类，用于锁定文件区域以防止并发访问。

## 方法

### position / size / isValid / release

### isShared / overlaps / acquiredBy

## 测试

- 描述: 获取文件锁
- 断言: 锁获取成功

```java
// 方法体开始
System.out.println("=== FileLock ===");
Path tmpFile = Files.createTempFile("lock-", ".txt");
try (FileChannel channel = FileChannel.open(tmpFile, StandardOpenOption.WRITE, StandardOpenOption.READ)) {
    FileLock lock = channel.lock(0, Long.MAX_VALUE, true);
    assertTrue(lock.isValid());
    assertTrue(lock.isShared());
    assertEquals(0, lock.position());
    lock.release();
    assertFalse(lock.isValid());
    System.out.println("文件锁测试通过");
}
Files.delete(tmpFile);
System.out.println("=== 测试通过 ===");
// 方法体结束
```
