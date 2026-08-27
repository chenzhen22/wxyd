---
name: ChoiceFormat
package: java.text
order: 258
---

## 介绍

`java.text.ChoiceFormat` 是**选择格式化类**，根据数值范围选择格式化输出。

## 方法

构造方法：
```java
public ChoiceFormat(double[] limits, String[] formats)
```

### format

根据数值匹配范围并返回对应格式。

## 测试

- 描述: 根据数值选择不同输出
- 断言: 匹配正确

```java
// 方法体开始
System.out.println("=== ChoiceFormat ===");
double[] limits = {0, 1, 2};
String[] formats = {"没有项目", "1 个项目", "{0} 个项目"};
ChoiceFormat cf = new ChoiceFormat(limits, formats);
MessageFormat mf = new MessageFormat("{0}");
mf.setFormat(0, cf);
assertEquals("没有项目", mf.format(new Object[]{0}));
assertEquals("1 个项目", mf.format(new Object[]{1}));
assertEquals("5 个项目", mf.format(new Object[]{5}));
System.out.println("0 -> " + cf.format(0));
System.out.println("1 -> " + cf.format(1));
System.out.println("5 -> " + cf.format(5));
System.out.println("=== 测试通过 ===");
// 方法体结束
```
