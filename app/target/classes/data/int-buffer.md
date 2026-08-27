---
name: IntBuffer
package: java.nio
order: 368
---

## 介绍

`java.nio.IntBuffer` 是**整数缓冲区**，NIO 缓冲区体系的 int 特化版本。

## 方法

### allocate / wrap / put / get / flip / clear

## 测试

- 描述: IntBuffer 操作
- 断言: 正常读写

```java
// 方法体开始
System.out.println("=== IntBuffer ===");
IntBuffer buf = IntBuffer.allocate(5);
buf.put(10);
buf.put(20);
buf.put(30);
buf.flip();
assertEquals(10, buf.get());
assertEquals(20, buf.get());
assertEquals(30, buf.get());
buf.rewind();
int[] data = new int[buf.remaining()];
buf.get(data);
assertArrayEquals(new int[]{10, 20, 30}, data);
System.out.println("IntBuffer 测试通过");
System.out.println("=== 测试通过 ===");
// 方法体结束
```
