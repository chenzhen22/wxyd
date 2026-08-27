---
name: FormatStyle
package: java.time.format
order: 232
---

## 介绍

`java.time.format.FormatStyle` 是 Java 8 新增的**格式化样式枚举**，用于定义日期/时间的显示样式。

FormatStyle 的常量：
- `FULL` — 完整格式，如 `2024年1月15日 星期一`
- `LONG` — 长格式，如 `2024年1月15日`
- `MEDIUM` — 中等格式，如 `2024-1-15`
- `SHORT` — 短格式，如 `24-1-15`

## 方法

### values / valueOf

标准枚举方法。

## 测试

- 描述: 使用不同样式格式化日期
- 断言: 样式影响输出格式

```java
// 方法体开始
System.out.println("=== FormatStyle ===");
LocalDate date = LocalDate.of(2024, 1, 15);
DateTimeFormatter full = DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL).withLocale(Locale.CHINA);
DateTimeFormatter medium = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(Locale.CHINA);
DateTimeFormatter short_ = DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT).withLocale(Locale.CHINA);
String f = date.format(full);
String m = date.format(medium);
String s = date.format(short_);
assertNotNull(f);
assertNotNull(m);
assertNotNull(s);
System.out.println("FULL: " + f + ", MEDIUM: " + m + ", SHORT: " + s);
System.out.println("=== 测试通过 ===");
// 方法体结束
```
