---
name: SimpleDateFormat
package: java.text
order: 221
---

## 介绍

`java.text.SimpleDateFormat` 是**日期格式化类**，用于格式化和解析日期。它是 `DateFormat` 的具体子类。

尽管 Java 8 推荐使用 `DateTimeFormatter`，但 SimpleDateFormat 在遗留代码中广泛使用。

## 方法

构造方法：
```java
public SimpleDateFormat(String pattern)
public SimpleDateFormat(String pattern, Locale locale)
```

### format / parse

```java
public String format(Date date)
public Date parse(String source) throws ParseException
```

### applyPattern / toPattern

应用/获取格式模式。

## 测试

- 描述: 格式化日期
- 断言: 格式化结果正确

```java
// 方法体开始
System.out.println("=== SimpleDateFormat ===");
SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
Date date = sdf.parse("2024-01-15 10:30:00");
String formatted = sdf.format(date);
assertEquals("2024-01-15 10:30:00", formatted);
Calendar cal = Calendar.getInstance();
cal.setTime(date);
assertEquals(2024, cal.get(Calendar.YEAR));
assertEquals(0, cal.get(Calendar.MONTH));  // 0 = January
assertEquals(15, cal.get(Calendar.DAY_OF_MONTH));
System.out.println("日期: " + formatted);
System.out.println("=== 测试通过 ===");
// 方法体结束
```
