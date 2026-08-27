---
name: Appendable
package: java.lang
order: 324
---

## 介绍

`java.lang.Appendable` 是**可追加接口**，可追加字符的对象。

## 方法

### append

```java
public Appendable append(CharSequence csq) throws IOException
```

## 测试

- 描述: 使用 Appendable
- 断言: 追加成功

```java
// 方法体开始
System.out.println("=== Appendable ===");
StringBuilder sb = new StringBuilder();
Appendable app = sb;
app.append("Hello");
app.append(" World");
assertEquals("Hello World", sb.toString());
System.out.println("Appendable: " + sb.toString());
System.out.println("=== 测试通过 ===");
// 方法体结束
```
