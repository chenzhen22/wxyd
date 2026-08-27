---
name: CookieManager
package: java.net
order: 271
---

## 介绍

`java.net.CookieManager` 是 **HTTP Cookie 管理器**类，用于管理 HTTP 请求和响应的 Cookie。

## 方法

构造方法：
```java
public CookieManager()
public CookieManager(CookieStore store, CookiePolicy policy)
```

### setDefault

设置默认 Cookie 管理器。

### getCookieStore

获取 Cookie 存储。

## 测试

- 描述: 使用 CookieManager 管理 Cookie
- 断言: Cookie 存取正常

```java
// 方法体开始
System.out.println("=== CookieManager ===");
CookieManager cm = new CookieManager();
CookieStore store = cm.getCookieStore();
assertNotNull(store);
URI uri = URI.create("http://example.com");
HttpCookie cookie = new HttpCookie("name", "value");
store.add(uri, cookie);
List<HttpCookie> cookies = store.get(uri);
assertEquals(1, cookies.size());
assertEquals("name", cookies.get(0).getName());
assertEquals("value", cookies.get(0).getValue());
System.out.println("Cookie 存取成功");
System.out.println("=== 测试通过 ===");
// 方法体结束
```
