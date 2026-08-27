---
name: ResourceBundle
package: java.util
order: 222
---

## 介绍

`java.util.ResourceBundle` 是**资源包**类，用于国际化和本地化，加载特定语言环境的属性文件。

## 方法

### getBundle

```java
public static ResourceBundle getBundle(String baseName)
public static ResourceBundle getBundle(String baseName, Locale locale)
```

加载资源包。

### getString

```java
public String getString(String key)
```

获取本地化字符串。

### keySet / containsKey

获取/检查键集合。

## 测试

- 描述: 加载资源包
- 断言: 获取本地化字符串

```java
// 方法体开始
System.out.println("=== ResourceBundle ===");
ResourceBundle bundle = ResourceBundle.getBundle("application", Locale.getDefault());
if (bundle != null) {
    System.out.println("资源包加载成功");
} else {
    System.out.println("资源包不可用");
}
System.out.println("=== 测试通过 ===");
// 方法体结束
```
