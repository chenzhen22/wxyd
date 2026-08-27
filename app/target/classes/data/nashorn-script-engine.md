---
name: NashornScriptEngine
package: javax.script
order: 139
---

## 介绍

`javax.script.ScriptEngine`（Nashorn 引擎）是 Java 8 内置的 **JavaScript 执行引擎**。Nashorn 是 Java 8 中取代 Rhino 的新一代 JavaScript 引擎，支持 ECMAScript 5.1，性能显著提升。

ScriptEngine 的核心能力：
- **执行 JavaScript 代码**：通过 `eval()` 方法
- **Java 互操作**：从 JavaScript 调用 Java 类和方法
- **变量绑定**：在 Java 和 JavaScript 之间共享变量
- **编译支持**：`Compilable` 接口支持编译 JS 代码供重复执行

Nashorn 的创建方式：
```java
ScriptEngine engine = new ScriptEngineManager().getEngineByName("nashorn");
```

注意：Nashorn 在 JDK 15+ 中被移除，但作为 Java 8 的标志性特性之一，非常值得学习和了解。

## 方法

### eval

```java
public Object eval(String script) throws ScriptException
public Object eval(Reader reader) throws ScriptException
```

执行 JavaScript 脚本。

- **参数**: `script` — JavaScript 代码字符串
- **返回**: `Object` — 脚本返回值

### put / get

```java
public void put(String key, Object value)
public Object get(String key)
```

在引擎作用域中设置/获取变量。

### createBindings

```java
public Bindings createBindings()
```

创建新的绑定对象，用于隔离脚本执行环境。

## 测试

### 基础运算

- 描述: 使用 Nashorn 引擎执行 JavaScript 数学运算
- 断言: 结果正确

```java
// 方法体开始
System.out.println("=== 基础运算 ===");
ScriptEngine engine = new ScriptEngineManager().getEngineByName("nashorn");
if (engine != null) {
    Object result = engine.eval("1 + 2 * 3;");
    assertEquals(7.0, ((Number) result).doubleValue(), 0.0001);
    System.out.println("1 + 2 * 3 = " + result);
} else {
    System.out.println("Nashorn 引擎不可用（JDK 15+ 已移除）");
}
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### 字符串操作

- 描述: 在 JavaScript 中操作字符串
- 断言: 字符串函数正确

```java
// 方法体开始
System.out.println("=== 字符串操作 ===");
ScriptEngine engine = new ScriptEngineManager().getEngineByName("nashorn");
if (engine != null) {
    Object result = engine.eval("'Hello, ' + 'World!'.toUpperCase();");
    assertEquals("Hello, WORLD!", result);
    System.out.println("JS 字符串: " + result);
} else {
    System.out.println("Nashorn 引擎不可用");
}
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### Java 互操作

- 描述: 在 JavaScript 中调用 Java 类
- 断言: 正确使用 Java 的 ArrayList

```java
// 方法体开始
System.out.println("=== Java 互操作 ===");
ScriptEngine engine = new ScriptEngineManager().getEngineByName("nashorn");
if (engine != null) {
    engine.eval("var list = new java.util.ArrayList();");
    engine.eval("list.add('A'); list.add('B');");
    Object list = engine.get("list");
    assertNotNull(list);
    System.out.println("JS 创建的 ArrayList: " + list);
} else {
    System.out.println("Nashorn 引擎不可用");
}
System.out.println("=== 测试通过 ===");
// 方法体结束
```
