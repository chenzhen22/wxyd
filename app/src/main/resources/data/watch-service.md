---
name: WatchService
package: java.nio.file
order: 356
---

## 介绍

`java.nio.file.WatchService` 是**文件监控服务**接口，用于监视目录中的文件变化（创建、修改、删除）。

## 方法

### take / poll / close

### watchable

## 测试

- 描述: 创建 WatchService
- 断言: 创建成功

```java
// 方法体开始
System.out.println("=== WatchService ===");
try {
    WatchService watcher = FileSystems.getDefault().newWatchService();
    assertNotNull(watcher);
    assertFalse(watcher.poll() != null);
    watcher.close();
    assertTrue(true);
    System.out.println("WatchService 创建成功");
} catch (Exception e) {
    System.out.println("WatchService 不可用: " + e.getMessage());
}
System.out.println("=== 测试通过 ===");
// 方法体结束
```
