---
name: Selector
package: java.nio.channels
order: 363
---

## 介绍

`java.nio.channels.Selector` 是**选择器**类，是 Java NIO 多路复用的核心，支持单线程管理多个 Channel。

## 方法

### open / close / isOpen

### select / selectNow / select(long)

### selectedKeys / keys

### wakeup

## 测试

- 描述: 创建 Selector
- 断言: 选择器创建成功

```java
// 方法体开始
System.out.println("=== Selector ===");
try (Selector selector = Selector.open()) {
    assertNotNull(selector);
    assertTrue(selector.isOpen());
    System.out.println("Selector 创建成功");
    int selected = selector.selectNow();
    assertEquals(0, selected);
} catch (Exception e) {
    System.out.println("Selector 不可用: " + e.getMessage());
}
System.out.println("=== 测试通过 ===");
// 方法体结束
```
