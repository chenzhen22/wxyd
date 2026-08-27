---
name: NumberFormat
package: java.text
order: 219
---

## 介绍

`java.text.NumberFormat` 是**数字格式抽象类**，用于格式化和解析数字。

## 方法

### getInstance / getCurrencyInstance / getPercentInstance

```java
public static NumberFormat getInstance()
public static NumberFormat getCurrencyInstance()
public static NumberFormat getPercentInstance()
```

### format / parse / setMaximumFractionDigits

格式化和解析数字。

## 测试

- 描述: 格式化数字为本地化字符串
- 断言: 格式结果正确

```java
// 方法体开始
System.out.println("=== NumberFormat ===");
NumberFormat nf = NumberFormat.getInstance(Locale.US);
nf.setMaximumFractionDigits(2);
assertEquals("1,234.57", nf.format(1234.567));
NumberFormat cf = NumberFormat.getCurrencyInstance(Locale.US);
assertTrue(cf.format(99.99).contains("99.99"));
NumberFormat pf = NumberFormat.getPercentInstance(Locale.US);
assertEquals("50%", pf.format(0.5));
System.out.println("数字: 1,234.57, 货币: $99.99, 百分比: 50%");
System.out.println("=== 测试通过 ===");
// 方法体结束
```
