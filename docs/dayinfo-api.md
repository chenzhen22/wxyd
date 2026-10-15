# 金融日报接口文档（平台前端 / 开发者）

> 推送方（外部系统）对接见 `docs/dayinfo-push-guide.md`；本文档面向平台页面与开发者，
> 说明全部金融日报接口的调用方式。前端对应实现：`Assets/js/dayinfo.js`（桌面）、`Assets/js/h5-dayinfo.js`（H5）。

## 0. 通用约定

- Base path：页面与接口同源同上下文，相对路径即可（如 `dayinfo/list` → 实际 `/api/dayinfo/list`）
- 统一响应：`{errorCode, errorMsg, traceId, clientIp, body}`，成功 `errorCode === "000000"`
- 未登录调用受保护接口 → HTTP 401，由全局 `ajaxError` 统一跳登录页（桌面 `index.js` / H5 `h5.js`）
- POST 类接口请求体用 Result 包裹：`{"body": {...}}`（`h5.js` 有 `postJSON(url, body)` 封装）

## 1. 日报查询（登录后可用）

### 1.1 分页列表

```
GET dayinfo/list?type=finance&pageNum=1&pageSize=50
```

```js
$.getJSON('dayinfo/list', { pageNum: 1, pageSize: 50 }).done(function (res) {
    var body = res.body;          // { total: 12, list: [DayInfo...] }
    body.list.forEach(function (d) { /* d.infoDate, d.title, d.brief... */ });
});
```

响应 `body`：

| 字段 | 类型 | 说明 |
|---|---|---|
| `total` | int | 该类型总条数 |
| `list` | DayInfo[] | 按 `infoDate` 倒序 |

### 1.2 最新一期

```
GET dayinfo/latest?type=finance
```

`body` = DayInfo 或 null（尚无数据）。

### 1.3 详情

```
GET dayinfo/detail?id=1
```

`body` = DayInfo。

**DayInfo 结构**：

| 字段 | 说明 |
|---|---|
| `id` | 详情/分享链接用 |
| `type` | 日报类型，当前恒为 `finance` |
| `infoDate` | 所属日期 `YYYY-MM-DD`（type+date 唯一，同日覆盖） |
| `title` | 标题 |
| `content` | md 原文（前端用 `window.WelfareMd.render(content)` 渲染，防 XSS） |
| `brief` | 简洁版/今日要点（可空）。推送方提供时，页面在正文前展示「📌 今日要点」卡 |
| `createTime` / `updateTime` | `YYYY-MM-DD HH:mm` |

## 2. 订阅提醒（登录后可用，一人多条：多个钉钉机器人 + 多个邮箱，上限 10 条）

| 接口 | 方式 | 请求 body | 说明 |
|---|---|---|---|
| `dayinfo/subscribe/my` | GET | — | 我的订阅列表，`body` = DayInfoSubscribe[] |
| `dayinfo/subscribe/add` | POST | `{channel, robotId?, email?}` | 新增一条；钉钉带 `robotId`，邮件带 `email` |
| `dayinfo/subscribe/delete` | POST | `{id}` | 删除一条（SQL 层校验归属） |
| `dayinfo/subscribe/toggle` | POST | `{id, enabled}` | 启用/暂停一条 |
| `dayinfo/subscribe/test` | POST | `{id}` | 立即试推最新一期到该订阅（自检通道） |

**DayInfoSubscribe 结构**：`{id, userId, channel("dingtalk"|"email"), robotId, email, enabled(1|0), createTime, updateTime}`

调用示例（桌面 `dayinfo.js` 与 H5 `h5-dayinfo.js` 同构）：

```js
// POST {body:{...}} 包裹
function postJSON(url, body) {
    return $.ajax({ url: url, type: 'POST', contentType: 'application/json',
        data: JSON.stringify({ body: body || {} }) });
}

// 添加钉钉订阅
postJSON('dayinfo/subscribe/add', { channel: 'dingtalk', robotId: 16 });
// 添加邮件订阅
postJSON('dayinfo/subscribe/add', { channel: 'email', email: 'a@b.com' });
// 测试推送
postJSON('dayinfo/subscribe/test', { id: 1 }).done(function (res) {
    res.errorCode === '000000' ? alert(res.body) : alert(res.errorMsg);
});
```

钉钉订阅要求机器人归属当前用户（推送时由 `PushService` 校验 access_token/secret，钉钉返回的 `errcode≠0` 会解析成可读失败原因，如自定义关键词/IP 白名单问题）。

## 3. 推送入库（免登录，外部系统专用）

```
POST common/dayinfo.do
```

请求体 `{timestamp, data(SM2 密文), sign(SM4 加签)}`，明文 `{"type","data":{date,title,md,brief}}`。
协议细节、密钥、错误码见 `docs/dayinfo-push-guide.md`。前端页面**不调用**此接口。

## 4. 公开只读（免登录，分享链接）

| 项 | 说明 |
|---|---|
| `GET wdayinfo/detail?id=` | 公开详情，只暴露日报内容本身，不含用户数据 |
| `dayinfo-share.html?id=<id>` | 独立分享页（免登录，含注册引导），钉钉/邮件推送里的链接即指向此页 |

```js
$.getJSON('wdayinfo/detail', { id: id }).done(function (res) { /* res.body = DayInfo */ });
```

> 路径刻意避开 `/dayinfo/**` 的登录拦截（WebMvcConfig 保护清单），照 `/welfare/**` 公开只读模式。

## 5. 前端接入要点（新会话/改造参考）

- 菜单由 `menu_item` 表驱动（`DataInitializer.MENU_SEED` 播种 `dayinfo`，desktop+h5）；页面注册在 `api.html` 的 `<div class="module-box" id="dayinfo">`，`index.js showPage()` 懒加载 `window.DayInfo.init()`（幂等）；H5 为 `#page-dayinfo` section + `registerH5Module('dayinfo', renderDayInfo)`
- md 渲染统一用 `Assets/js/welfare-md.js`（`window.WelfareMd.render(mdText)`，整体转义防 XSS；链接只留文字、无本地映射的图片丢弃）
- 静态资源改动记得 bump 引用版本号（`?v=N`）做缓存刷新
