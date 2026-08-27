---
name: DateFormatSymbols
package: java.text
order: 259
---

## 介绍

`java.text.DateFormatSymbols` 是**日期格式符号**类，封装了日期格式化中的本地化字符串（月份名、星期名等）。

## 方法

### getInstance

```java
public static DateFormatSymbols getInstance()
public static DateFormatSymbols getInstance(Locale locale)
```

### getMonths / getShortMonths

```java
public String[] getMonths()
public String[] getShortMonths()
```

### getWeekdays / getShortWeekdays

```java
public String[] getWeekdays()
public String[] getShortWeekdays()
```

## 测试

- 描述: 获取本地化月份和星期名称
- 断言: 名称不为空

```java
// 方法体开始
System.out.println("=== DateFormatSymbols ===");
DateFormatSymbols dfs = DateFormatSymbols.getInstance(Locale.CHINA);
String[] months = dfs.getMonths();
assertEquals(13, months.length);  // 12 个月 + 空字符串
assertEquals("一月", months[0]);
assertEquals("十二月", months[11]);
String[] weekdays = dfs.getWeekdays();
assertEquals("星期一", weekdays[2]);  // 索引 2 对应周一
System.out.println("月份[0]=" + months[0] + ", 月份[11]=" + months[11]);
System.out.println("星期[2]=" + weekdays[2]);
System.out.println("=== 测试通过 ===");
// 方法体结束
```
