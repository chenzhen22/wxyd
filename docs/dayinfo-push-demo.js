/* ============================================================
   金融日报推送联调示例（外部脚本侧）
   协议：POST /api/common/dayinfo.do
     {
       "timestamp": <毫秒时间戳>,
       "data": "<SM2 密文 hex（C1C3C2）>",
       "sign": "<SM4-ECB(SM3(data + timestamp)) hex>"
     }
   明文结构：{"type":"finance","data":{"date":"YYYY-MM-DD","title":"...","md":"..."}}
     - type 留空 = 金融日报
     - data.date 缺省取服务器当天；同 type 同日重复推送覆盖更新
   后端校验顺序：时间戳 ±5 分钟 → SM4 验签 → SM2 解密

   使用：
     1. 后端 env.properties 配置：
        wxyd.dayinfo.sm2-private-key=<SM2 私钥 64 hex>
        wxyd.dayinfo.sm4-key=<SM4 密钥 32 hex>
        私钥用 com.cyz.util.SMUtil.generateSM2KeyPair() 生成，
        SM4 密钥用 SMUtil.generateSM4Key() 生成；公钥与 SM4 密钥交给推送方。
     2. npm install sm-crypto
     3. node dayinfo-push-demo.js
   ============================================================ */
const sm2 = require('sm-crypto').sm2;
const sm3 = require('sm-crypto').sm3;
const sm4 = require('sm-crypto').sm4;

/* ======== 配置（替换为实际值） ======== */
const URL = 'http://localhost:8090/api/common/dayinfo.do';
const PUBLIC_KEY = '' // SM2 公钥，130 hex（04 开头）
const SM4_KEY = '';   // SM4 密钥，32 hex

/* ======== 待推送数据 ======== */
const payload = {
    type: 'finance', // 留空也是金融日报
    data: {
        date: '2026-10-14', // 缺省取当天
        title: '金融日报 2026-10-14',
        md: '# 金融日报\n\n## 市场概览\n\n- 上证指数 **3600.00**（+0.50%）\n- 深证成指 12000.00（-0.20%）\n\n> 具体内容示例，正文支持 markdown。'
    }
};

function main() {
    if (!PUBLIC_KEY || !SM4_KEY) {
        console.error('请先填写 PUBLIC_KEY 与 SM4_KEY');
        process.exit(1);
    }
    const timestamp = Date.now();
    const plain = JSON.stringify(payload);

    // 1. SM2 加密（C1C3C2；sm-crypto 输出不含 04 前缀，后端 BC 需要，手动补上）
    const dataHex = '04' + sm2.doEncrypt(plain, PUBLIC_KEY, 1);

    // 2. SM4 加签：sign = SM4-ECB(SM3(data密文hex + timestamp))
    const digest = sm3(dataHex + timestamp);
    const sign = sm4.encrypt(digest, SM4_KEY); // 默认 ECB + pkcs#7，输出 hex

    // 3. 推送
    fetch(URL, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ timestamp, data: dataHex, sign })
    }).then(r => r.json()).then(res => {
        console.log('响应：', JSON.stringify(res));
        if (res.errorCode === '000000') console.log('推送成功');
        else console.error('推送失败：', res.errorMsg);
    }).catch(e => {
        console.error('请求异常：', e.message);
    });
}

main();
