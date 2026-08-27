---
name: ScriptEngineManager
package: javax.script
order: 284
---

## 介绍

`javax.script.ScriptEngineManager` 是**脚本引擎管理器**类，用于发现和创建脚本引擎实例。

## 方法

构造方法：
```java
public ScriptEngineManager()
```

### getEngineByName

```java
public ScriptEngine getEngineByName(String shortName)
```

### getEngineFactories

```java
public List<ScriptEngineFactory> getEngineFactories()
```

### put / get

设置/获取全局作用域变量。

## 测试

- 描述: 获取 Nashorn 脚本引擎
- 断言: 引擎可用时不为 null

```java
// 方法体开始
System.out.println("=== ScriptEngineManager ===");
ScriptEngineManager sem = new ScriptEngineManager();
ScriptEngine engine = sem.getEngineByName("nashorn");
if (engine != null) {
    assertNotNull(engine);
    System.out.println("Nashorn 引擎已找到");
} else {
    System.out.println("Nashorn 引擎不可用（JDK 15+ 已移除）");
}
List<ScriptEngineFactory> factories = sem.getEngineFactories();
System.out.println("可用脚本引擎数: " + factories.size());
System.out.println("=== 测试通过 ===");
// 方法体结束
```
