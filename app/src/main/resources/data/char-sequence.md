---
name: CharSequence
package: java.lang
order: 164
---

## 介绍

`java.lang.CharSequence` 接口在 Java 8 中新增了 `chars()` 和 `codePoints()` 默认方法，使所有字符序列（String、StringBuilder、StringBuffer 等）都能直接获取字符流。

## 方法

### chars

```java
public default IntStream chars()
```

返回字符序列中 char 值的 IntStream（0 到 65535）。

### codePoints

```java
public default IntStream codePoints()
```

返回 Unicode 码点的 IntStream。与 `chars` 不同，它正确处理增补字符（如 emoji）。

## 测试

### chars 统计

- 描述: 使用 chars() 统计字符数
- 断言: 字符数正确

```java
// 方法体开始
System.out.println("=== chars ===");
CharSequence cs = "Hello Java 8";
long count = cs.chars().count();
assertEquals(11, count);
System.out.println("字符数: " + count);
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### 过滤大写字母

- 描述: 使用 chars() 过滤大写字母
- 断言: 大写字母数量正确

```java
// 方法体开始
System.out.println("=== 过滤大写 ===");
CharSequence cs = "Hello World Java";
long upperCount = cs.chars()
        .filter(Character::isUpperCase)
        .count();
assertEquals(3, upperCount);  // H, W, J
System.out.println("大写字母数: " + upperCount);
System.out.println("=== 测试通过 ===");
// 方法体结束
```
