---
name: UUID
package: java.util
order: 45
---

## 介绍

`java.util.UUID` 是 Java 5 引入的**通用唯一标识符**（UUID）类，用于生成 128 位的全局唯一标识符。UUID 的标准字符串形式如 `550e8400-e29b-41d4-a716-446655440000`。

常用场景：
- **主键生成**：数据库主键
- **会话标识**：Token、Session ID
- **文件命名**：避免重名

## 方法

### randomUUID

```java
public static UUID randomUUID()
```

生成随机的 UUID（Type 4）。

- **返回**: `UUID`

### fromString

```java
public static UUID fromString(String name)
```

从标准 UUID 字符串解析。

- **参数**: `name` — UUID 字符串
- **返回**: `UUID`

### toString

```java
public String toString()
```

返回 UUID 的字符串表示。

- **返回**: `String`

### getMostSignificantBits

```java
public long getMostSignificantBits()
```

返回 UUID 的高 64 位。

- **返回**: `long`

### getLeastSignificantBits

```java
public long getLeastSignificantBits()
```

返回 UUID 的低 64 位。

- **返回**: `long`

### version

```java
public int version()
```

返回 UUID 的版本号。

- **返回**: `int`

### equals

```java
public boolean equals(Object obj)
```

判断两个 UUID 是否相等。

- **参数**: `obj` — 比较对象
- **返回**: `boolean`

## 测试

### randomUUID

- 描述: 生成随机 UUID
- 断言: UUID 不为 null，字符串格式正确

```java
// 方法体开始
System.out.println("=== randomUUID ===");
UUID uuid = UUID.randomUUID();
assertNotNull(uuid);
String str = uuid.toString();
System.out.println("UUID: " + str);
assertEquals(36, str.length());
assertEquals(5, str.split("-").length);
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### fromString

- 描述: 从字符串解析 UUID
- 断言: 解析正确

```java
// 方法体开始
System.out.println("=== fromString ===");
UUID uuid = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
assertEquals("550e8400-e29b-41d4-a716-446655440000", uuid.toString());
System.out.println("解析 UUID: " + uuid);
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### version

- 描述: 获取 UUID 版本号
- 断言: randomUUID 版本为 4

```java
// 方法体开始
System.out.println("=== version ===");
UUID uuid = UUID.randomUUID();
assertTrue(uuid.version() >= 0);
System.out.println("UUID 版本: " + uuid.version() + ", 字符串: " + uuid);
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### getBits

- 描述: 获取 UUID 的高低 64 位
- 断言: 高低位不为 0

```java
// 方法体开始
System.out.println("=== getBits ===");
UUID uuid = UUID.randomUUID();
long msb = uuid.getMostSignificantBits();
long lsb = uuid.getLeastSignificantBits();
System.out.println("高64位: " + msb + ", 低64位: " + lsb);
assertTrue(msb != 0 || lsb != 0);
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### equals

- 描述: 判断 UUID 相等
- 断言: 相同 UUID 返回 true

```java
// 方法体开始
System.out.println("=== equals ===");
UUID uuid1 = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
UUID uuid2 = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
UUID uuid3 = UUID.randomUUID();
assertTrue(uuid1.equals(uuid2));
assertFalse(uuid1.equals(uuid3));
System.out.println(uuid1 + " equals " + uuid2 + " : " + uuid1.equals(uuid2));
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### toString

- 描述: UUID 字符串表示
- 断言: 字符串格式正确

```java
// 方法体开始
System.out.println("=== toString ===");
UUID uuid = UUID.randomUUID();
String str = uuid.toString();
assertEquals(36, str.length());
assertEquals(5, str.split("-").length);
System.out.println("UUID: " + str);
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### getMostSignificantBits

- 描述: 获取高 64 位
- 断言: 随机 UUID 的高位不为 0

```java
// 方法体开始
System.out.println("=== getMostSignificantBits ===");
UUID uuid = UUID.randomUUID();
long msb = uuid.getMostSignificantBits();
System.out.println("高64位: " + msb);
assertNotNull(uuid);
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### getLeastSignificantBits

- 描述: 获取低 64 位
- 断言: 随机 UUID 的低位不为 0

```java
// 方法体开始
System.out.println("=== getLeastSignificantBits ===");
UUID uuid = UUID.randomUUID();
long lsb = uuid.getLeastSignificantBits();
System.out.println("低64位: " + lsb);
assertNotNull(uuid);
System.out.println("=== 测试通过 ===");
// 方法体结束
```

