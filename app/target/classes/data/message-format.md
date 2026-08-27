---
name: MessageFormat
package: java.text
order: 249
---

## 介绍

`java.text.MessageFormat` 是**消息格式化类**，用于构建带占位符的字符串消息，支持国际化。

## 方法

构造方法：
```java
public MessageFormat(String pattern)
public MessageFormat(String pattern, Locale locale)
```

### format

```java
public String format(Object... arguments)
```

用参数替换模式中的占位符 `{0}`、`{1}` 等。

### setLocale / applyPattern

设置语言环境/应用模式。

## 测试

- 描述: 格式化带占位符的消息
- 断言: 替换结果正确

```java
// 方法体开始
System.out.println("=== MessageFormat ===");
String msg = MessageFormat.format("您好 {0}，您的订单 {1} 已确认，金额 {2,number,#,##0.00}", 
        "张三", "ORD-2024-001", 1299.5);
assertTrue(msg.contains("张三"));
assertTrue(msg.contains("ORD-2024-001"));
assertTrue(msg.contains("1,299.50"));
System.out.println("消息: " + msg);
System.out.println("=== 测试通过 ===");
// 方法体结束
```
