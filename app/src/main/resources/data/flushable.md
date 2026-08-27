---
name: Flushable
package: java.io
order: 323
---

## 介绍

`java.io.Flushable` 是**可刷新接口**，表示可以刷新输出的数据目标。

## 方法

### flush

```java
public void flush() throws IOException
```

## 测试

- 描述: 自定义 Flushable
- 断言: flush 被调用

```java
// 方法体开始
System.out.println("=== Flushable ===");
List<String> log = new ArrayList<>();
Flushable f = () -> log.add("flushed");
f.flush();
assertEquals(1, log.size());
System.out.println("Flushable 测试通过");
System.out.println("=== 测试通过 ===");
// 方法体结束
```
