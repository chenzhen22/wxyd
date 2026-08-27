---
name: PrintStream
package: java.io
order: 187
---

## 介绍

`java.io.PrintStream` 是**格式化输出字节流**。`System.out` 和 `System.err` 就是 PrintStream 实例。Java 8 中它与 Lambda 配合使用更加便捷。

## 方法

### print / println

```java
public void print(String s)
public void println(String x)
```

输出字符串 / 输出并换行。

### printf / format

```java
public PrintStream printf(String format, Object... args)
public PrintStream format(String format, Object... args)
```

格式化输出。

## 测试

### printf

- 描述: 使用 printf 格式化输出
- 断言: 格式化结果正确

```java
// 方法体开始
System.out.println("=== printf ===");
ByteArrayOutputStream baos = new ByteArrayOutputStream();
PrintStream ps = new PrintStream(baos);
ps.printf("Hello %s, you are %d years old.", "Alice", 25);
String result = baos.toString("UTF-8");
assertEquals("Hello Alice, you are 25 years old.", result);
System.out.println("printf 格式化: " + result);
System.out.println("=== 测试通过 ===");
// 方法体结束
```
