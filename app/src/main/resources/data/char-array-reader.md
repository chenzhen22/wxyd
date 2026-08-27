---
name: CharArrayReader
package: java.io
order: 209
---

## 介绍

`java.io.CharArrayReader` 是**字符数组输入流**，以字符数组作为数据源。

## 方法

构造方法：
```java
public CharArrayReader(char[] buf)
public CharArrayReader(char[] buf, int offset, int length)
```

### read / ready / skip

标准 Reader 方法。

## 测试

- 描述: 从字符数组读取
- 断言: 读取结果正确

```java
// 方法体开始
System.out.println("=== CharArrayReader ===");
char[] chars = {'H', 'e', 'l', 'l', 'o'};
CharArrayReader car = new CharArrayReader(chars);
char[] buf = new char[5];
int n = car.read(buf);
assertEquals(5, n);
assertEquals("Hello", new String(buf));
car.close();
System.out.println("读取: " + new String(buf));
System.out.println("=== 测试通过 ===");
// 方法体结束
```
