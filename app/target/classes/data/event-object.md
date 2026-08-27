---
name: EventObject
package: java.util
order: 227
---

## 介绍

`java.util.EventObject` 是**事件对象基类**，是所有事件状态对象的父类。虽然在 Java 8 中它不是新类，但它配合 Lambda 和函数式接口一起使用时更加强大。

EventObject 的子类包括：`ActionEvent`、`MouseEvent`、`WindowEvent` 等 AWT/Swing 事件。

## 方法

构造方法：
```java
public EventObject(Object source)
```

### getSource

```java
public Object getSource()
```

返回事件源对象。

### toString

事件对象的字符串表示。

## 测试

- 描述: 创建事件对象
- 断言: 事件源正确

```java
// 方法体开始
System.out.println("=== EventObject ===");
String source = "event-source";
EventObject event = new EventObject(source);
assertSame(source, event.getSource());
System.out.println("事件源: " + event.getSource());
System.out.println("=== 测试通过 ===");
// 方法体结束
```
