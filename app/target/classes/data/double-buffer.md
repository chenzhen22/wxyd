---
name: DoubleBuffer
package: java.nio
order: 369
---

## 介绍

`java.nio.DoubleBuffer` 是**双精度浮点缓冲区**。

## 方法

### allocate / put / get / flip

## 测试

- 描述: DoubleBuffer 操作
- 断言: 正常读写

```java
// 方法体开始
System.out.println("=== DoubleBuffer ===");
DoubleBuffer buf = DoubleBuffer.allocate(3);
buf.put(1.5);
buf.put(2.5);
buf.put(3.5);
buf.flip();
assertEquals(1.5, buf.get(), 0.0001);
assertEquals(2.5, buf.get(), 0.0001);
assertEquals(3.5, buf.get(), 0.0001);
System.out.println("DoubleBuffer 测试通过");
System.out.println("=== 测试通过 ===");
// 方法体结束
```
