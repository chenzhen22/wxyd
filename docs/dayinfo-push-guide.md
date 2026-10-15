# 金融日报推送对接文档（推送方必读）

## 1. 接口概览

| 项 | 说明 |
|---|---|
| 地址 | `POST https://<服务域名>/api/common/dayinfo.do` |
| 认证 | 免登录，靠 **SM4 加签 + SM2 加密** 双重鉴权（无 session/token） |
| Content-Type | `application/json` |
| 频率 | 每天 1 次（建议固定时间推送）；同一天重复推送会**覆盖**当天数据，只保留最新 |
| 防重放 | 请求时间戳与服务器时间相差 **±5 分钟** 内有效 |

## 2. 你会拿到什么密钥

服务方线下提供两把密钥：

| 密钥 | 格式 | 用途 |
|---|---|---|
| SM2 公钥 | 130 位 hex，`04` 开头 | 对报文加密（服务端持私钥解密） |
| SM4 密钥 | 32 位 hex（16 字节） | 对 SM3 摘要加密实现「加签」（双方共用） |

> SM2 私钥只存在服务端，推送方**不需要也拿不到**。

## 3. 请求格式

```json
{
  "timestamp": 1760000000000,
  "data": "<SM2 密文 hex>",
  "sign": "<签名 hex>"
}
```

三个字段的生成步骤（顺序不能乱）：

```
第 1 步  plain     = 明文 JSON 字符串（见第 4 节）
第 2 步  timestamp = 当前毫秒时间戳
第 3 步  data      = SM2 加密(plain, 公钥)，C1C3C2 模式，hex 输出
第 4 步  digest    = SM3(data + timestamp)     ← 注意：签的是「密文+时间戳」字符串拼接
第 5 步  sign      = SM4-ECB 加密(digest, SM4密钥)，PKCS7 填充，hex 输出
第 6 步  POST {timestamp, data, sign}
```

服务端按 `时间戳窗口 → SM4 验签 → SM2 解密` 串行校验，任一步失败即拒绝并返回错误。

### 注意事项

- SM2 密文用 **C1C3C2** 顺序。JS 库 `sm-crypto` 的 `doEncrypt` 输出**不含 `04` 前缀**，需手动补 `04`（见示例）；Java BouncyCastle 默认输出含 `04`。
- `data` 参与签名时**不要做任何变换**（不要 trim、不要改大小写），就是最终提交的 hex 字符串。
- `timestamp` 拼接进签名的是**十进制数字字符串**（如 `1760000000000`），与 JSON 里提交的数值必须一致。

## 4. 明文结构（plain）

```json
{
  "type": "finance",
  "data": {
    "date": "2026-10-14",
    "title": "金融日报 2026-10-14",
    "md": "# 金融日报\n\n## 市场概览\n\n- 上证指数 **3600.00**（+0.50%）",
    "brief": "今日要点：央行开展逆回购；三家银行手机银行项目招标；三家银行手机银行项目招标"
  }
}
```

| 字段 | 必填 | 说明 |
|---|---|---|
| `type` | 否 | 日报类型。留空或 `finance` = 金融日报；当前只用这一个值 |
| `data.date` | 否 | 日报所属日期，`YYYY-MM-DD`。缺省取服务端当天 |
| `data.title` | 否 | 标题。缺省自动生成「金融日报 YYYY-MM-DD」 |
| `data.md` | **是** | 正文，markdown 原文。平台按 md 渲染展示 |
| `data.brief` | 否 | **简洁版**（今日要点/摘要，建议 ≤100 字）。订阅推送（钉钉/邮件）优先展示该字段；缺省由服务端自动截取正文前 100 字 |

**入库规则**：按 `type + date` 每天一条；同日再次推送 = 覆盖更新（title/content 均被替换），不会产生第二条。**首次**入库（非覆盖）时服务端会向已订阅用户推送钉钉/邮件提醒；覆盖更新不重复提醒。

## 5. 响应与错误

统一返回：

```json
{"errorCode": "000000", "errorMsg": "successful", "traceId": "...", "clientIp": "...", "body": ""}
```

| errorCode | 含义 | 排查 |
|---|---|---|
| `000000` | 成功 | — |
| `000015` + 「时间戳超出允许窗口」 | 时间戳偏差 > 5 分钟 | 检查本机时钟（NTP 对时） |
| `000015` + 「验签失败」 | sign 解密后与 SM3 摘要不匹配 | 检查 SM4 密钥、data+timestamp 拼接顺序、是否 hex 大小写/空格被改 |
| `000015` + 「解密失败」 | SM2 解密失败 | 检查公钥是否正确、密文是否漏了 `04` 前缀（sm-crypto）、是否用了 C1C2C3 |
| `000015` + 「数据结构非法/缺少 data/data.md 不能为空/data.date 非法」 | 解密成功但明文不对 | 按第 4 节修正明文结构 |

## 6. 完整示例（Node.js，`npm install sm-crypto`）

```js
const sm2 = require('sm-crypto').sm2;
const sm3 = require('sm-crypto').sm3;
const sm4 = require('sm-crypto').sm4;

const URL = 'https://<服务域名>/api/common/dayinfo.do';
const PUBLIC_KEY = '<04 开头的 130 位 SM2 公钥>';   // 服务方提供
const SM4_KEY = '<32 位 SM4 密钥>';                 // 服务方提供

const payload = {
    type: 'finance',
    data: {
        date: '2026-10-14',
        title: '金融日报 2026-10-14',
        md: '# 金融日报\n\n## 市场概览\n\n- 上证指数 **3600.00**（+0.50%）'
    }
};

const timestamp = Date.now();
// C1C3C2 加密；sm-crypto 输出不含 04 前缀，手动补上
const dataHex = '04' + sm2.doEncrypt(JSON.stringify(payload), PUBLIC_KEY, 1);
// 加签：SM4-ECB(SM3(data + timestamp))
const sign = sm4.encrypt(sm3(dataHex + timestamp), SM4_KEY); // sm4 默认 ECB + pkcs#7

fetch(URL, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ timestamp, data: dataHex, sign })
}).then(r => r.json()).then(res => {
    if (res.errorCode === '000000') console.log('推送成功');
    else console.error('推送失败：', res.errorCode, res.errorMsg);
});
```

可运行的完整脚本（填入密钥即可联调）：`docs/dayinfo-push-demo.js`。

其他语言要点：Python 可用 `gmssl` 库（`CryptSM2` 默认 C1C2C3，注意指定 C1C3C2）；Java 用 BouncyCastle `SM2Engine`（默认即 C1C3C2）+ `SM4Engine`（ECB 模式需自己分块 + PKCS7 填充）。

## 7. 联调自检清单

- [ ] 时钟已 NTP 对时（±5 分钟窗口内）
- [ ] 公钥为服务方提供的 130 位 hex，未加空格/换行
- [ ] SM2 密文是 C1C3C2 且以 `04` 开头
- [ ] 签名输入 = `data完整hex字符串 + timestamp十进制字符串` 直接拼接
- [ ] SM4 用 ECB 模式 + PKCS7 填充，输出 hex
- [ ] 收到 `000000` 后，到平台「金融日报」菜单确认展示正常
