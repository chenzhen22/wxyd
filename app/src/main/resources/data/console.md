---
name: Console
package: java.io
order: 169
---

## 介绍

`java.io.Console` 是用于与**系统控制台**交互的类，提供读取密码、格式化输出等功能。Java 8 中它获得了一些 Lambda 友好的 API。

## 方法

### format / printf

```java
public Console format(String fmt, Object... args)
public Console printf(String format, Object... args)
```

格式化输出到控制台。

### readLine

```java
public String readLine()
public String readLine(String fmt, Object... args)
```

读取一行输入。

### readPassword

```java
public char[] readPassword()
public char[] readPassword(String fmt, Object... args)
```

读取密码（不回显）。

### reader / writer

```java
public Reader reader()
public PrintWriter writer()
```

获取控制台的 Reader/Writer。

### flush

```java
public void flush()
```

刷新控制台。

## 测试

### console

- 描述: 获取系统 Console 对象
- 断言: Console 可用时不为 null（IDE 中通常为 null）

```java
// 方法体开始
System.out.println("=== Console ===");
Console console = System.console();
if (console != null) {
    console.printf("控制台可用%n");
    assertNotNull(console.writer());
    assertNotNull(console.reader());
    System.out.println("Console 可用");
} else {
    System.out.println("Console 不可用（非交互式环境）");
}
System.out.println("=== 测试通过 ===");
// 方法体结束
```
