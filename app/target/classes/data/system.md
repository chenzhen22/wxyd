---
name: System
package: java.lang
order: 153
---

## 介绍

`java.lang.System` 类在 Java 8 中新增了 **`lines()`** 方法，用于从系统控制台获取输入行流。

Java 8 新增的方法：
- `lines()` — 返回控制台输入行 Stream（如果可用）

## 方法

### lineSeparator

```java
public static String lineSeparator()
```

返回系统相关的行分隔符。

- **返回**: `String` — 行分隔符

### console

```java
public static Console console()
```

返回与当前 JVM 关联的 Console 对象（如果有）。

## 测试

### lineSeparator

- 描述: 获取系统行分隔符
- 断言: 不为空

```java
// 方法体开始
System.out.println("=== lineSeparator ===");
String separator = System.lineSeparator();
assertNotNull(separator);
assertTrue(separator.length() > 0);
System.out.println("行分隔符长度: " + separator.length());
System.out.println("=== 测试通过 ===");
// 方法体结束
```
