---
name: Iterable
package: java.lang
order: 146
---

## 介绍

`java.lang.Iterable` 接口在 Java 8 中新增了 `forEach` 默认方法，使得所有可迭代对象都能直接使用 Lambda 遍历。

Java 8 新增的方法：
- `forEach(Consumer)` — 对每个元素执行操作

## 方法

### forEach

```java
default void forEach(Consumer<? super T> action)
```

对 Iterable 中的每个元素执行给定操作，直到所有元素都被处理或抛出异常。

- **参数**: `action` — 要对每个元素执行的操作
- **返回**: 无

## 测试

### forEach 遍历列表

- 描述: 使用 Iterable.forEach 遍历列表
- 断言: 正确遍历所有元素

```java
// 方法体开始
System.out.println("=== forEach 列表 ===");
List<String> list = Arrays.asList("A", "B", "C");
List<String> result = new ArrayList<>();
Iterable<String> iterable = list;
iterable.forEach(result::add);
assertEquals(3, result.size());
assertEquals("[A, B, C]", result.toString());
System.out.println("遍历: " + result);
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### forEach 自定义 Iterable

- 描述: 自定义 Iterable 实现
- 断言: 遍历结果正确

```java
// 方法体开始
System.out.println("=== 自定义 Iterable ===");
Iterable<Integer> range = () -> new Iterator<Integer>() {
    int i = 1;
    public boolean hasNext() { return i <= 5; }
    public Integer next() { return i++; }
};
int[] sum = {0};
range.forEach(n -> sum[0] += n);
assertEquals(15, sum[0]);
System.out.println("1-5 求和: " + sum[0]);
System.out.println("=== 测试通过 ===");
// 方法体结束
```
