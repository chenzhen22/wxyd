---
name: Objects
package: java.util
order: 18
---

## 介绍

`java.util.Objects` 是 Java 7 引入的工具类，包含操作对象的静态辅助方法。Java 8 新增了几个重要方法，用于空值安全处理和比较。

常见用途：
- **空值检查**：`isNull`、`nonNull`、`requireNonNull`
- **安全比较**：`equals`、`deepEquals`、`compare`
- **哈希计算**：`hash`、`hashCode`
- **安全转换**：`toString`

## 方法

### equals

```java
public static boolean equals(Object a, Object b)
```

判断两个对象是否相等（空安全）。两者都为 null 返回 true；仅一个为 null 返回 false。

- **参数**: `a` — 对象 a；`b` — 对象 b
- **返回**: `boolean`

### deepEquals

```java
public static boolean deepEquals(Object a, Object b)
```

深度判断两个对象是否相等，支持数组的深层比较。

- **参数**: `a` — 对象 a；`b` — 对象 b
- **返回**: `boolean`

### hashCode

```java
public static int hashCode(Object o)
```

返回对象的哈希码，如果对象为 null 则返回 0。

- **参数**: `o` — 对象
- **返回**: `int`

### hash

```java
public static int hash(Object... values)
```

为一系列值生成哈希码。用于实现 `hashCode()` 方法。

- **参数**: `values` — 可变参数
- **返回**: `int`

### toString(Object)

```java
public static String toString(Object o)
```

返回对象的字符串表示。如果对象为 null，返回 "null"。

- **参数**: `o` — 对象
- **返回**: `String`

### toString(Object, String)

```java
public static String toString(Object o, String nullDefault)
```

返回对象的字符串表示。如果对象为 null，返回默认值。

- **参数**: `o` — 对象；`nullDefault` — 默认值
- **返回**: `String`

### compare

```java
public static <T> int compare(T a, T b, Comparator<? super T> c)
```

使用比较器比较两个对象。如果比较器为 null 则返回 0。

- **参数**: `a` — 对象 a；`b` — 对象 b；`c` — 比较器
- **返回**: `int`

### isNull

```java
public static boolean isNull(Object obj)
```

判断对象是否为 null。

- **参数**: `obj` — 对象
- **返回**: `boolean`

### nonNull

```java
public static boolean nonNull(Object obj)
```

判断对象是否不为 null。

- **参数**: `obj` — 对象
- **返回**: `boolean`

### requireNonNull(T)

```java
public static <T> T requireNonNull(T obj)
```

检查对象是否为 null，如果不是则返回该对象；否则抛出 `NullPointerException`。

- **参数**: `obj` — 对象
- **返回**: `T` — 对象本身
- **异常**: `NullPointerException` — 如果对象为 null

### requireNonNull(T, String)

```java
public static <T> T requireNonNull(T obj, String message)
```

检查对象是否为 null，如果不是则返回该对象；否则抛出带消息的 `NullPointerException`。

- **参数**: `obj` — 对象；`message` — 异常消息
- **返回**: `T` — 对象本身
- **异常**: `NullPointerException` — 如果对象为 null

## 测试

### equals

- 描述: 空安全 equals
- 断言: 两者均为 null 返回 true，其中一个为 null 返回 false

```java
// 方法体开始
System.out.println("=== equals ===");
assertTrue(Objects.equals(null, null));
assertFalse(Objects.equals("a", null));
assertFalse(Objects.equals(null, "a"));
assertTrue(Objects.equals("hello", "hello"));
assertFalse(Objects.equals("hello", "world"));
System.out.println("null==null: " + Objects.equals(null, null));
System.out.println("hello==hello: " + Objects.equals("hello", "hello"));
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### deepEquals

- 描述: 深度比较（支持数组）
- 断言: 相同数组内容返回 true

```java
// 方法体开始
System.out.println("=== deepEquals ===");
int[] arr1 = {1, 2, 3};
int[] arr2 = {1, 2, 3};
int[] arr3 = {4, 5, 6};
assertTrue(Objects.deepEquals(arr1, arr2));
assertFalse(Objects.deepEquals(arr1, arr3));
assertTrue(Objects.deepEquals(null, null));
assertFalse(Objects.deepEquals(arr1, null));
System.out.println("数组深度比较: " + Objects.deepEquals(arr1, arr2));
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### hashCode

- 描述: 获取对象的哈希码
- 断言: null 返回 0，非 null 返回正确哈希码

```java
// 方法体开始
System.out.println("=== hashCode ===");
assertEquals(0, Objects.hashCode(null));
assertEquals("hello".hashCode(), Objects.hashCode("hello"));
System.out.println("null hashCode: " + Objects.hashCode(null));
System.out.println("hello hashCode: " + Objects.hashCode("hello"));
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### hash

- 描述: 为多个值生成哈希码
- 断言: 相同值的哈希码相同

```java
// 方法体开始
System.out.println("=== hash ===");
int hash1 = Objects.hash("a", "b", "c");
int hash2 = Objects.hash("a", "b", "c");
int hash3 = Objects.hash("x", "y", "z");
assertEquals(hash1, hash2);
assertFalse(hash1 == hash3);
System.out.println("hash([a,b,c]): " + hash1);
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### toString

- 描述: 空安全 toString
- 断言: null 返回 "null"，非 null 返回 toString

```java
// 方法体开始
System.out.println("=== toString ===");
assertEquals("null", Objects.toString(null));
assertEquals("hello", Objects.toString("hello"));
System.out.println("null: " + Objects.toString(null));
System.out.println("hello: " + Objects.toString("hello"));
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### toStringWithDefault

- 描述: toString 带默认值
- 断言: null 返回默认值，非 null 返回对象本身

```java
// 方法体开始
System.out.println("=== toStringWithDefault ===");
assertEquals("default", Objects.toString(null, "default"));
assertEquals("hello", Objects.toString("hello", "default"));
System.out.println("null: " + Objects.toString(null, "default"));
System.out.println("hello: " + Objects.toString("hello", "default"));
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### compare

- 描述: 使用比较器比较对象
- 断言: 比较结果正确

```java
// 方法体开始
System.out.println("=== compare ===");
int result = Objects.compare("a", "b", String::compareTo);
assertTrue(result < 0);
assertEquals(0, Objects.compare("a", "a", String::compareTo));
assertTrue(Objects.compare("c", "a", String::compareTo) > 0);
System.out.println("a 和 b 比较结果: " + result);
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### isNull

- 描述: 判断对象是否为 null
- 断言: null 返回 true，非 null 返回 false

```java
// 方法体开始
System.out.println("=== isNull ===");
assertTrue(Objects.isNull(null));
assertFalse(Objects.isNull("hello"));
System.out.println("null: " + Objects.isNull(null) + ", hello: " + Objects.isNull("hello"));
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### nonNull

- 描述: 判断对象是否不为 null
- 断言: null 返回 false，非 null 返回 true

```java
// 方法体开始
System.out.println("=== nonNull ===");
assertFalse(Objects.nonNull(null));
assertTrue(Objects.nonNull("hello"));
System.out.println("null: " + Objects.nonNull(null) + ", hello: " + Objects.nonNull("hello"));
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### requireNonNull

- 描述: 检查非空
- 断言: 非 null 返回对象本身，null 抛出 NullPointerException

```java
// 方法体开始
System.out.println("=== requireNonNull ===");
String str = Objects.requireNonNull("hello");
assertEquals("hello", str);
try {
    Objects.requireNonNull(null);
    fail("应抛出 NullPointerException");
} catch (NullPointerException e) {
    System.out.println("null 抛出了 NullPointerException");
}
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### requireNonNullWithMessage

- 描述: 带消息的非空检查
- 断言: null 抛出带自定义消息的 NullPointerException

```java
// 方法体开始
System.out.println("=== requireNonNullWithMessage ===");
String str = Objects.requireNonNull("hello", "不应为 null");
assertEquals("hello", str);
try {
    Objects.requireNonNull(null, "自定义异常消息");
    fail("应抛出 NullPointerException");
} catch (NullPointerException e) {
    assertEquals("自定义异常消息", e.getMessage());
}
System.out.println("=== 测试通过 ===");
// 方法体结束
```
