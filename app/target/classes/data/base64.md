---
name: Base64
package: java.util
order: 35
---

## 介绍

`java.util.Base64` 是 Java 8 新增的**Base64 编解码工具类**，提供了标准 Base64、URL 安全和 MIME 三种编码风格的支持。它使用静态内部类 `Encoder` 和 `Decoder` 实现编解码操作。

Base64 的三种编码类型：
- **Basic**：标准 Base64，`A-Za-z0-9+/`，补位用 `=`
- **URL Safe**：文件名/URL 安全，使用 `-` 和 `_` 代替 `+` 和 `/`
- **MIME**：MIME 编码，每行最多 76 字符，使用 `\r\n` 换行

## 方法

### getEncoder / getUrlEncoder / getMimeEncoder

```java
public static Encoder getEncoder()
public static Encoder getUrlEncoder()
public static Encoder getMimeEncoder()
```

获取对应类型的编码器。

### getDecoder / getUrlDecoder / getMimeDecoder

```java
public static Decoder getDecoder()
public static Decoder getUrlDecoder()
public static Decoder getMimeDecoder()
```

获取对应类型的解码器。

### Encoder.encodeToString

```java
public String encodeToString(byte[] src)
```

将字节数组编码为 Base64 字符串。

### Decoder.decode

```java
public byte[] decode(String src)
```

解码 Base64 字符串为字节数组。

## 测试

### 基本编码解码

- 描述: 使用 Base64 编码和解码字符串
- 断言: 解码后与原始字符串一致

```java
// 方法体开始
System.out.println("=== 基本编码解码 ===");
String original = "Hello, Java 8!";
String encoded = Base64.getEncoder().encodeToString(original.getBytes("UTF-8"));
byte[] decoded = Base64.getDecoder().decode(encoded);
assertEquals(original, new String(decoded, "UTF-8"));
System.out.println("原: " + original + " -> 编码: " + encoded);
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### URL 安全编码

- 描述: URL 安全的 Base64 编码
- 断言: 编码结果不含 `+` 和 `/`

```java
// 方法体开始
System.out.println("=== URL 安全 ===");
byte[] data = {0, (byte) 255, (byte) 128, 64};
String b64 = Base64.getUrlEncoder().encodeToString(data);
assertFalse(b64.contains("+"));
assertFalse(b64.contains("/"));
byte[] decoded = Base64.getUrlDecoder().decode(b64);
assertArrayEquals(data, decoded);
System.out.println("URL 安全编码: " + b64);
System.out.println("=== 测试通过 ===");
// 方法体结束
```
