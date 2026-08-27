---
name: ObjectStreamField
package: java.io
order: 298
---

## 介绍

`java.io.ObjectStreamField` 是**序列化字段描述**类，用于描述 Java 对象序列化中的字段信息。

## 方法

构造方法：
```java
public ObjectStreamField(String name, Class<?> type)
```

### getName / getType / getTypeString

获取字段名和类型。

### isPrimitive / isUnshared

## 测试

- 描述: 描述序列化字段
- 断言: 字段信息正确

```java
// 方法体开始
System.out.println("=== ObjectStreamField ===");
ObjectStreamField field = new ObjectStreamField("name", String.class);
assertEquals("name", field.getName());
assertEquals(String.class, field.getType());
assertFalse(field.isPrimitive());
ObjectStreamField intField = new ObjectStreamField("age", Integer.TYPE);
assertTrue(intField.isPrimitive());
System.out.println("字段: " + field.getName() + " 类型: " + field.getType());
System.out.println("=== 测试通过 ===");
// 方法体结束
```
