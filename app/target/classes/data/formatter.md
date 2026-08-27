---
name: Formatter
package: java.util
order: 141
---

## 介绍

`java.util.Formatter` 是 Java 中的**格式化字符串**工具类，支持类似 C 语言 `printf` 的格式化风格。Java 8 为其新增了一些便捷的方法增强。

Formatter 的核心特点：
- **格式化输出**：支持各种数据类型（整数、浮点、日期、字符串等）
- **多种输出目标**：StringBuilder、OutputStream、File、PrintStream
- **类型安全**：与 printf 语法兼容
- **与 String.format 配合**：String.format 内部使用 Formatter

## 方法

构造方法：
```java
public Formatter()
public Formatter(Appendable a)
public Formatter(File file, String csn)
public Formatter(PrintStream ps)
```

核心方法：
- `format(String format, Object... args)` — 格式化字符串
- `out()` — 返回输出目标（Appendable）
- `ioException()` — 返回上次 I/O 异常
- `toString()` — 返回格式化结果
- `flush()` / `close()` — 刷新和关闭

## 测试

### 格式化数字

- 描述: 格式化数字到字符串
- 断言: 结果正确

```java
// 方法体开始
System.out.println("=== 格式化数字 ===");
StringBuilder sb = new StringBuilder();
try (Formatter fmt = new Formatter(sb)) {
    fmt.format("int: %d, hex: %x, float: %.2f", 255, 255, 3.14159);
}
assertEquals("int: 255, hex: ff, float: 3.14", sb.toString());
System.out.println("格式化: " + sb);
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### 格式化日期

- 描述: 使用 Formatter 格式化日期
- 断言: 输出包含正确的日期部分

```java
// 方法体开始
System.out.println("=== 格式化日期 ===");
StringBuilder sb = new StringBuilder();
try (Formatter fmt = new Formatter(sb)) {
    fmt.format("现在是 %tY 年 %<tm 月 %<te 日", System.currentTimeMillis());
}
String result = sb.toString();
assertTrue(result.contains("年"));
assertTrue(result.contains("月"));
assertTrue(result.contains("日"));
System.out.println("日期格式化: " + result);
System.out.println("=== 测试通过 ===");
// 方法体结束
```
