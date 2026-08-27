---
name: StringReader
package: java.io
order: 198
---

## 介绍

`java.io.StringReader` 是**字符串字符输入流**，以字符串作为数据源的字符流，常用于测试和字符串解析。

## 方法

构造方法：
```java
public StringReader(String s)
```

### read / ready / skip

标准 Reader 方法。

## 测试

- 描述: 从字符串读取字符
- 断言: 读取结果正确

```java
// 方法体开始
System.out.println("=== StringReader ===");
StringReader sr = new StringReader("Hello Java");
char[] buf = new char[5];
int n = sr.read(buf);
assertEquals(5, n);
assertEquals("Hello", new String(buf));
n = sr.read(buf, 0, 4);
assertEquals(4, n);
assertEquals(" Jav", new String(buf, 0, 4));
sr.close();
System.out.println("StringReader 读取完成");
System.out.println("=== 测试通过 ===");
// 方法体结束
```
