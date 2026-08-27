---
name: AutoCloseable
package: java.lang
order: 236
---

## 介绍

`java.lang.AutoCloseable` 是 Java 7 引入的**自动关闭接口**，是 try-with-resources 的基础。Java 8 中它与 Stream 和 Lambda 配合紧密，`Stream` 也实现了 `AutoCloseable`。

## 方法

### close

```java
public void close() throws Exception
```

释放资源。

## 测试

- 描述: 使用 try-with-resources 自动关闭
- 断言: 资源正确关闭

```java
// 方法体开始
System.out.println("=== AutoCloseable ===");
List<String> log = new ArrayList<>();
try (AutoCloseable resource = new AutoCloseable() {
    public void close() {
        log.add("closed");
    }
}) {
    log.add("using");
}
assertEquals("[using, closed]", log.toString());
System.out.println("资源已自动关闭");
System.out.println("=== 测试通过 ===");
// 方法体结束
```
