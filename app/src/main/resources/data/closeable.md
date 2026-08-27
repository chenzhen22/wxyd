---
name: Closeable
package: java.io
order: 322
---

## 介绍

`java.io.Closeable` 是**可关闭接口**，表示可以关闭的数据源或目标。继承了 `AutoCloseable`，适用于 try-with-resources。

## 方法

### close

```java
public void close() throws IOException
```

## 测试

- 描述: 自定义 Closeable 资源
- 断言: 自动关闭

```java
// 方法体开始
System.out.println("=== Closeable ===");
List<String> log = new ArrayList<>();
try (Closeable res = () -> log.add("closed")) {
    log.add("using");
}
assertEquals("[using, closed]", log.toString());
System.out.println("Closeable 自动关闭成功");
System.out.println("=== 测试通过 ===");
// 方法体结束
```
