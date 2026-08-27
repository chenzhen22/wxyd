---
name: Format
package: java.text
order: 184
---

## 介绍

`java.text.Format` 是**格式化基础抽象类**。Java 8 中它与 Lambda 配合，用于格式化日期/数字/消息等。

Format 的主要子类：
- `DateFormat` / `SimpleDateFormat` — 日期格式化
- `NumberFormat` — 数字格式化
- `MessageFormat` — 消息格式化
- `DecimalFormat` — 十进制数字格式化

## 方法

### format

```java
public final String format(Object obj)
```

格式化对象为字符串。

### parseObject

```java
public Object parseObject(String source) throws ParseException
```

将字符串解析为对象。

## 测试

### SimpleDateFormat

- 描述: 使用 SimpleDateFormat 格式化日期
- 断言: 格式化结果正确

```java
// 方法体开始
System.out.println("=== SimpleDateFormat ===");
SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
Date date = sdf.parse("2024-01-15");
String formatted = sdf.format(date);
assertEquals("2024-01-15", formatted);
System.out.println("日期: " + formatted);
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### DecimalFormat

- 描述: 格式化数字
- 断言: 格式正确

```java
// 方法体开始
System.out.println("=== DecimalFormat ===");
DecimalFormat df = new DecimalFormat("#,##0.00");
String formatted = df.format(1234567.89);
assertEquals("1,234,567.89", formatted);
System.out.println("数字: " + formatted);
System.out.println("=== 测试通过 ===");
// 方法体结束
```
