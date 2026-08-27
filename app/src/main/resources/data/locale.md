---
name: Locale
package: java.util
order: 131
---

## 介绍

`java.util.Locale` 表示**语言和国家/地区**。Java 8 新增了过滤和筛选方法，可以按语言标签过滤 Locale。

Java 8 新增的方法：
- `filter(List<Locale>, Predicate<Locale>)` — 过滤已安装的 Locale
- `filterTags(List<String>, Predicate<String>)` — 过滤语言标签
- `lookup(List<Locale>, Collection<Locale>)` — 查找最匹配的 Locale
- `lookupTag(List<String>, Collection<String>)` — 查找最匹配的语言标签

## 方法

### getAvailableLocales

```java
public static Locale[] getAvailableLocales()
```

返回所有已安装的 Locale 数组。

## 测试

### 过滤 Locale

- 描述: 过滤出英语相关的 Locale
- 断言: 至少包含 English

```java
// 方法体开始
System.out.println("=== 过滤 Locale ===");
List<Locale> locales = Arrays.asList(Locale.getAvailableLocales());
List<Locale> englishLocales = locales.stream()
        .filter(l -> l.getLanguage().equals("en"))
        .limit(5)
        .collect(Collectors.toList());
assertTrue(englishLocales.size() > 0);
assertEquals("en", englishLocales.get(0).getLanguage());
System.out.println("英语 Locale 示例:");
englishLocales.forEach(l -> System.out.println("  " + l + " - " + l.getDisplayName()));
System.out.println("=== 测试通过 ===");
// 方法体结束
```
