---
name: ByteBuffer
package: java.nio
order: 347
---

## 介绍

`java.nio.ByteBuffer` 是 **NIO 字节缓冲区**，是 Java NIO 的核心类，用于高效地处理字节数据。它提供了直接缓冲区和非直接缓冲区两种分配方式。

## 方法

### allocate / allocateDirect

```java
public static ByteBuffer allocate(int capacity)
public static ByteBuffer allocateDirect(int capacity)
```

### put / get / flip / clear / rewind

缓冲区操作的核心方法。

### wrap

```java
public static ByteBuffer wrap(byte[] array)
```

### remaining / limit / position / capacity

## 测试

- 描述: ByteBuffer 基本操作
- 断言: 正确读写

```java
// 方法体开始
System.out.println("=== ByteBuffer ===");
ByteBuffer buf = ByteBuffer.allocate(10);
assertEquals(10, buf.capacity());
assertEquals(10, buf.remaining());
buf.put((byte) 'H');
buf.put((byte) 'e');
buf.put((byte) 'l');
buf.put((byte) 'l');
buf.put((byte) 'o');
buf.flip();
assertEquals(5, buf.remaining());
assertEquals('H', (char) buf.get());
assertEquals('e', (char) buf.get());
assertEquals('l', (char) buf.get());
assertEquals('l', (char) buf.get());
assertEquals('o', (char) buf.get());
assertEquals(0, buf.remaining());
buf.rewind();
assertEquals(5, buf.remaining());
System.out.println("ByteBuffer 测试通过");
System.out.println("=== 测试通过 ===");
// 方法体结束
```
