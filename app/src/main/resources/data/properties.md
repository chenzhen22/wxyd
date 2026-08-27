---
name: Properties
package: java.util
order: 51
---

## 介绍

`java.util.Properties` 是 Java 中用于处理**配置属性文件**的类，以键值对形式存储配置。常用于读取 `.properties` 配置文件。

## 方法

### Properties()

```java
public Properties()
```

创建空的 Properties。

### setProperty

```java
public Object setProperty(String key, String value)
```

设置属性。

- **参数**: `key` — 键；`value` — 值
- **返回**: `Object` — 旧值

### getProperty

```java
public String getProperty(String key)
```

获取属性值。

- **参数**: `key` — 键
- **返回**: `String` — 值，不存在返回 null

### getProperty(String, String)

```java
public String getProperty(String key, String defaultValue)
```

获取属性值，带默认值。

- **参数**: `key` — 键；`defaultValue` — 默认值
- **返回**: `String`

### stringPropertyNames

```java
public Set<String> stringPropertyNames()
```

返回所有键的集合。

- **返回**: `Set<String>`

### load

```java
public void load(InputStream inStream) throws IOException
```

从输入流加载属性文件。

- **参数**: `inStream` — 输入流
- **异常**: `IOException`

### store

```java
public void store(OutputStream out, String comments) throws IOException
```

将属性保存到输出流。

- **参数**: `out` — 输出流；`comments` — 注释

### size

```java
public int size()
```

返回属性数量。

- **返回**: `int`

### remove

```java
public Object remove(Object key)
```

移除属性。

- **参数**: `key` — 键
- **返回**: `Object` — 旧值

### containsKey

```java
public boolean containsKey(Object key)
```

判断是否包含指定键。

- **参数**: `key` — 键
- **返回**: `boolean`

## 测试

### Properties

- 描述: 设置并获取属性
- 断言: 属性值正确

```java
// 方法体开始
System.out.println("=== setGetProperty ===");
Properties props = new Properties();
props.setProperty("name", "Alice");
props.setProperty("age", "25");
assertEquals("Alice", props.getProperty("name"));
assertEquals("25", props.getProperty("age"));
assertNull(props.getProperty("unknown"));
assertEquals("default", props.getProperty("unknown", "default"));
System.out.println("name: " + props.getProperty("name") + ", age: " + props.getProperty("age"));
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### stringPropertyNames

- 描述: 获取所有属性名
- 断言: 键集合正确

```java
// 方法体开始
System.out.println("=== stringPropertyNames ===");
Properties props = new Properties();
props.setProperty("a", "1");
props.setProperty("b", "2");
props.setProperty("c", "3");
Set<String> names = props.stringPropertyNames();
assertEquals(3, names.size());
assertTrue(names.contains("a"));
assertTrue(names.contains("b"));
assertTrue(names.contains("c"));
System.out.println("属性名: " + names);
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### size

- 描述: 获取属性数量
- 断言: 数量正确

```java
// 方法体开始
System.out.println("=== size ===");
Properties props = new Properties();
assertEquals(0, props.size());
props.setProperty("x", "10");
assertEquals(1, props.size());
props.setProperty("y", "20");
assertEquals(2, props.size());
System.out.println("属性数量: " + props.size());
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### remove

- 描述: 移除属性
- 断言: 移除后不存在

```java
// 方法体开始
System.out.println("=== remove ===");
Properties props = new Properties();
props.setProperty("key", "value");
assertEquals("value", props.getProperty("key"));
props.remove("key");
assertNull(props.getProperty("key"));
System.out.println("移除 key 后: " + props.getProperty("key"));
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### containsKey

- 描述: 判断键是否存在
- 断言: 存在返回 true

```java
// 方法体开始
System.out.println("=== containsKey ===");
Properties props = new Properties();
props.setProperty("name", "test");
assertTrue(props.containsKey("name"));
assertFalse(props.containsKey("notexist"));
System.out.println("containsKey: " + props.containsKey("name"));
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### load

- 描述: 从字符串加载属性
- 断言: 加载后属性正确

```java
// 方法体开始
System.out.println("=== load ===");
Properties props = new Properties();
String content = "host=localhost\nport=8080";
java.io.ByteArrayInputStream bis = new java.io.ByteArrayInputStream(content.getBytes("UTF-8"));
props.load(bis);
bis.close();
assertEquals("localhost", props.getProperty("host"));
assertEquals("8080", props.getProperty("port"));
System.out.println("host: " + props.getProperty("host") + ", port: " + props.getProperty("port"));
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### store

- 描述: 保存属性到输出流
- 断言: 保存后可以再次加载

```java
// 方法体开始
System.out.println("=== store ===");
Properties props = new Properties();
props.setProperty("user", "admin");
java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream();
props.store(bos, "test comment");
bos.close();
String saved = bos.toString("UTF-8");
assertTrue(saved.contains("user") || saved.contains("admin"));
System.out.println("保存成功，内容: " + saved.substring(0, Math.min(50, saved.length())));
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### Properties

- 描述: 创建 Properties
- 断言: 对象不为 null

```java
// 方法体开始
System.out.println("=== Properties ===");
Properties props = new Properties();
assertNotNull(props);
System.out.println("Properties: " + props);
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### setProperty

- 描述: 设置属性
- 断言: 属性设置成功

```java
// 方法体开始
System.out.println("=== setProperty ===");
Properties props = new Properties();
props.setProperty("k", "v");
assertEquals("v", props.getProperty("k"));
System.out.println("setProperty: k=v");
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### getProperty

- 描述: 获取属性
- 断言: 返回值正确

```java
// 方法体开始
System.out.println("=== getProperty ===");
Properties props = new Properties();
props.setProperty("key", "val");
assertEquals("val", props.getProperty("key"));
assertNull(props.getProperty("none"));
System.out.println("getProperty key: " + props.getProperty("key"));
System.out.println("=== 测试通过 ===");
// 方法体结束
```

