---
name: BooleanSupplier
package: java.util.function
order: 90
---

## 介绍

`BooleanSupplier` 是 Java 8 引入的一个**函数式接口**，代表一个不接受参数但返回 `boolean` 值的供应者。

BooleanSupplier 的单个方法：
- `getAsBoolean()` — 核心方法

## 方法

### getAsBoolean

```java
boolean getAsBoolean()
```

供应一个 boolean 值。

## 测试

### getAsBoolean

- 描述: 检查一个数是否为偶数
- 断言: 4 是偶数，5 不是

```java
// 方法体开始
System.out.println("=== getAsBoolean ===");
int[] num = {4};
BooleanSupplier isEven = () -> num[0] % 2 == 0;
assertTrue(isEven.getAsBoolean());
num[0] = 5;
assertFalse(isEven.getAsBoolean());
System.out.println("4 是偶数: " + (4 % 2 == 0));
System.out.println("5 是偶数: " + (5 % 2 == 0));
System.out.println("=== 测试通过 ===");
// 方法体结束
```
