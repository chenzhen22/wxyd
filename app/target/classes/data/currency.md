---
name: Currency
package: java.util
order: 132
---

## 介绍

`java.util.Currency` 表示**货币**。Java 8 新增了 `getAvailableCurrencies()` 方法，返回所有可用货币的 Set。

## 方法

### getInstance

```java
public static Currency getInstance(String currencyCode)
```

根据货币代码创建 Currency。

### getCurrencyCode

```java
public String getCurrencyCode()
```

获取货币的 ISO 4217 代码。

### getSymbol

```java
public String getSymbol()
```

获取货币符号。

### getAvailableCurrencies

```java
public static Set<Currency> getAvailableCurrencies()
```

返回所有可用货币的 Set（Java 8 新增）。

## 测试

### 获取货币信息

- 描述: 获取人民币和美元的信息
- 断言: 货币代码和符号正确

```java
// 方法体开始
System.out.println("=== 货币信息 ===");
Currency cny = Currency.getInstance("CNY");
Currency usd = Currency.getInstance("USD");
assertEquals("CNY", cny.getCurrencyCode());
assertEquals("USD", usd.getCurrencyCode());
System.out.println("人民币: " + cny.getSymbol() + " (" + cny.getCurrencyCode() + ")");
System.out.println("美元: " + usd.getSymbol() + " (" + usd.getCurrencyCode() + ")");
System.out.println("=== 测试通过 ===");
// 方法体结束
```

### 可用货币

- 描述: 获取所有可用货币
- 断言: 至少包含主要货币

```java
// 方法体开始
System.out.println("=== 可用货币 ===");
Set<Currency> currencies = Currency.getAvailableCurrencies();
assertTrue(currencies.size() > 100);
Set<String> codes = currencies.stream()
        .map(Currency::getCurrencyCode)
        .collect(Collectors.toSet());
assertTrue(codes.contains("CNY"));
assertTrue(codes.contains("USD"));
assertTrue(codes.contains("EUR"));
System.out.println("总货币数: " + currencies.size());
System.out.println("包含人民币、美元、欧元: true");
System.out.println("=== 测试通过 ===");
// 方法体结束
```
