---
name: Comparator
package: java.util
order: 22
---

## 介绍

`java.util.Comparator` 是一个**函数式接口**，用于定义对象的自定义排序规则。Java 8 新增了大量函数式默认方法和静态方法，使其成为功能强大的比较工具链。

Java 8 新增的核心方法：
- **静态工厂**：`comparing()`、`comparingInt()`、`comparingLong()`、`comparingDouble()`、`naturalOrder()`、`reverseOrder()`
- **默认方法**：`reversed()`、`thenComparing()`、`thenComparingInt()`、`thenComparingLong()`

## 方法

### comparing

```java
public static <T, U extends Comparable<? super U>> Comparator<T> comparing(Function<? super T, ? extends U> keyExtractor)
```

根据 key 提取函数创建 Comparator。

### naturalOrder / reverseOrder

```java
public static <T extends Comparable<? super T>> Comparator<T> naturalOrder()
public static <T> Comparator<T> reverseOrder()
```

自然顺序/逆序。

### reversed / thenComparing

```java
default Comparator<T> reversed()
default Comparator<T> thenComparing(Comparator<? super T> other)
```

逆序 / 链式比较。

## 测试

### comparing

- 描述: 按字符串长度排序
- 断言: 排序结果正确

```java
// 方法体开始
System.out.println("=== comparing ===");
List<String> list = new ArrayList<>(Arrays.asList("banana", "apple", "cat", "dog"));
list.sort(Comparator.comparing(String::length));
assertEquals("cat", list.get(0));
assertEquals("banana", list.get(list.size()-1));
System.out.println("按长度: " + list);
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### thenComparing

- 描述: 先按长度再按字母序
- 断言: 排序正确

```java
// 方法体开始
System.out.println("=== thenComparing ===");
List<String> list = new ArrayList<>(Arrays.asList("cat", "dog", "apple", "bee", "banana"));
list.sort(Comparator.comparing(String::length).thenComparing(Comparator.naturalOrder()));
assertEquals("bee", list.get(0));
assertEquals("cat", list.get(1));
assertEquals("dog", list.get(2));
System.out.println("thenComparing: " + list);
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### naturalOrder / reverseOrder

- 描述: 升序和降序
- 断言: 排序正确

```java
// 方法体开始
System.out.println("=== naturalOrder ===");
List<Integer> nums = new ArrayList<>(Arrays.asList(5, 3, 1, 4, 2));
nums.sort(Comparator.naturalOrder());
assertEquals("[1, 2, 3, 4, 5]", nums.toString());
nums.sort(Comparator.reverseOrder());
assertEquals("[5, 4, 3, 2, 1]", nums.toString());
System.out.println("升序 -> 降序: " + nums);
System.out.println("=== 测试通过 ===");
// 方法体结束
```
