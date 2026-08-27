---
name: Field
package: java.lang.reflect
order: 242
---

## 介绍

`java.lang.reflect.Field` 代表类的**字段**（成员变量），提供动态获取/设置字段值的功能。

## 方法

### get / set

```java
public Object get(Object obj) throws IllegalAccessException
public void set(Object obj, Object value) throws IllegalAccessException, IllegalArgumentException
```

获取/设置字段值。

### getType / getName

获取字段类型和名称。

### setAccessible

```java
public void setAccessible(boolean flag)
```

设置可访问性（可访问私有字段）。

## 测试

- 描述: 反射读写字段
- 断言: 字段值正确

```java
// 方法体开始
System.out.println("=== Field ===");
Field field = Integer.class.getDeclaredField("value");
field.setAccessible(true);
Integer i = 42;
int val = field.getInt(i);
assertEquals(42, val);
field.setInt(i, 100);
assertEquals(100, i.intValue());
System.out.println("通过反射修改 Integer 值: " + i);
System.out.println("=== 测试通过 ===");
// 方法体结束
```
