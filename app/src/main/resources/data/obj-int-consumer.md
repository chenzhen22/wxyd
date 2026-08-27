---
name: ObjIntConsumer
package: java.util.function
order: 93
---

## 介绍

`ObjIntConsumer<T>` 是 Java 8 引入的一个**函数式接口**，代表一个接受一个对象和一个 `int` 参数但不返回结果的操作。

对应的特化接口：
- `ObjLongConsumer<T>` — 对象 + long
- `ObjDoubleConsumer<T>` — 对象 + double

## 方法

### accept

```java
void accept(T t, int value)
```

对给定对象和 int 值执行操作。

## 测试

### accept

- 描述: 使用 ObjIntConsumer 向列表添加指定次数的元素
- 断言: 添加 "hello" 3 次

```java
// 方法体开始
System.out.println("=== accept ===");
List<String> list = new ArrayList<>();
ObjIntConsumer<String> repeater = (s, times) -> {
    for (int i = 0; i < times; i++) list.add(s);
};
repeater.accept("hello", 3);
assertEquals(3, list.size());
assertEquals("hello", list.get(0));
assertEquals("hello", list.get(2));
System.out.println("列表: " + list);
System.out.println("=== 测试通过 ===");
// 方法体结束
```
