---
name: Month
package: java.time
order: 225
---

## 介绍

`java.time.Month` 是 Java 8 新增的**月份枚举**，表示一月到十二月。

Month 的常量：`JANUARY` 到 `DECEMBER`。`Month.JANUARY.getValue()` 返回 1，`DECEMBER` 返回 12。

## 方法

### getValue

```java
public int getValue()
```

返回月份值（1-12）。

### plus / minus

```java
public Month plus(long months)
public Month minus(long months)
```

增减月份，循环正确。

### length

```java
public int length(boolean leapYear)
```

返回指定年份（考虑闰年）该月的天数。

### firstMonthOfQuarter

返回该月所在季度的第一个月。

## 测试

### getValue / plus

- 描述: 枚举值和循环增减
- 断言: 正确

```java
// 方法体开始
System.out.println("=== Month ===");
assertEquals(1, Month.JANUARY.getValue());
assertEquals(12, Month.DECEMBER.getValue());
assertEquals(Month.MARCH, Month.JANUARY.plus(2));
assertEquals(Month.JANUARY, Month.DECEMBER.plus(1));
System.out.println("一月=" + Month.JANUARY + ", 十二月=" + Month.DECEMBER);
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### length

- 描述: 获取月份天数
- 断言: 2024 年 2 月 29 天

```java
// 方法体开始
System.out.println("=== length ===");
assertEquals(29, Month.FEBRUARY.length(true));   // 闰年
assertEquals(28, Month.FEBRUARY.length(false));  // 平年
assertEquals(31, Month.JANUARY.length(true));
System.out.println("2024年2月: " + Month.FEBRUARY.length(true) + "天");
System.out.println("=== 测试通过 ===");
// 方法体结束
```
