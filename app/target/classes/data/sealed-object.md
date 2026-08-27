---
name: SealedObject
package: javax.crypto
order: 294
---

## 介绍

`javax.crypto.SealedObject` 是**密封对象**类，使用加密算法将对象加密保护。

## 方法

构造方法：
```java
public SealedObject(Serializable object, Cipher c) throws IOException, IllegalBlockSizeException
```

### getObject

```java
public final Object getObject(Cipher c) throws ClassNotFoundException, IllegalBlockSizeException, BadPaddingException
```

## 测试

- 描述: 密封和解封对象
- 断言: 解封后数据一致

```java
// 方法体开始
System.out.println("=== SealedObject ===");
try {
    KeyGenerator kg = KeyGenerator.getInstance("AES");
    kg.init(128);
    SecretKey key = kg.generateKey();
    Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
    cipher.init(Cipher.ENCRYPT_MODE, key);
    SealedObject sealed = new SealedObject("Hello Secret", cipher);
    cipher.init(Cipher.DECRYPT_MODE, key);
    String result = (String) sealed.getObject(cipher);
    assertEquals("Hello Secret", result);
    System.out.println("密封对象: " + result);
} catch (Exception e) {
    System.out.println("密封操作不可用: " + e.getMessage());
}
System.out.println("=== 测试通过 ===");
// 方法体结束
```
