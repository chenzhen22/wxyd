---
name: NullPointerException
package: java.lang
order: 354
---

## 介绍

`java.lang.NullPointerException` 是**空指针异常**，当应用程序试图在需要对象的地方使用 `null` 时抛出。Java 8 中，Optional 可以帮助减少 NPE。

## 方法

构造方法：
```java
public NullPointerException()
public NullPointerException(String s)
```

## 测试

- 描述: 空指针异常
- 断言: 正确捕获

```java
// 方法体开始
System.out.println("=== NullPointerException ===");
try {
    String s = null;
    s.length();
} catch (NullPointerException e) {
    assertNotNull(e);
    System.out.println("捕获 NullPointerException");
}
// Optional 可以避免 NPE
Optional<String> opt = Optional.ofNullable(null);
assertFalse(opt.isPresent());
assertEquals("default", opt.orElse("default"));
System.out.println("Optional 避免 NPE: " + opt.orElse("default"));
System.out.println("=== 测试通过 ===");
// 方法体结束
```
