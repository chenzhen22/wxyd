---
name: DecimalFormat
package: java.text
order: 228
---

## 介绍

`java.text.DecimalFormat` 是 `NumberFormat` 的具体子类，用于格式化十进制数字。

## 方法

构造方法：
```java
public DecimalFormat()
public DecimalFormat(String pattern)
```

### applyPattern / toPattern

设置/获取格式模式。

### setMinimumFractionDigits / setMaximumFractionDigits

设置小数位数。

## 测试

- 描述: 格式化数字
- 断言: 格式正确

```java
// 方法体开始
System.out.println("=== DecimalFormat ===");
DecimalFormat df = new DecimalFormat("#,##0.00");
assertEquals("12,345.68", df.format(12345.6789));
DecimalFormat pct = new DecimalFormat("#0.00%");
assertEquals("85.50%", pct.format(0.855));
System.out.println("数字: " + df.format(12345.6789));
System.out.println("百分比: " + pct.format(0.855));
System.out.println("=== 测试通过 ===");
// 方法体结束
```
