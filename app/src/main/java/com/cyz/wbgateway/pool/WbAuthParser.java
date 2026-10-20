package com.cyz.wbgateway.pool;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

/**
 * WorkBuddy auth 文档解析（dsh-xdpool accounts.ts parseWorkBuddyAuth 的 Java 移植）。
 * 接受 {auth:{...},account:{...}} 嵌套（桌面 App 形态）与扁平形态；
 * 字段可能是明文字符串（旧版）或 $wbEncrypted 包装（5.6.0+）。
 */
@Slf4j
public final class WbAuthParser {

    private WbAuthParser() {
    }

    /**
     * 解析一份 .info 文本。
     * @return 无可用 accessToken 时返回 null；加密字段打不开抛 IllegalStateException
     *         （区分「未登录」与「有登录但拿不到密钥」——后者重登无效）
     */
    public static WbPoolCredential parse(String text, String sourcePath, boolean live,
                                         Map<String, byte[]> keysById) {
        JSONObject doc;
        try {
            doc = JSON.parseObject(text);
        } catch (Exception e) {
            return null;
        }
        if (doc == null) return null;
        JSONObject auth, identity;
        if (doc.getJSONObject("auth") != null) {
            auth = doc.getJSONObject("auth");
            identity = doc.getJSONObject("account") != null ? doc.getJSONObject("account") : new JSONObject();
        } else {
            auth = doc;
            identity = doc;
        }

        FieldRes res = field(auth.get("accessToken"), keysById, sourcePath);
        if (res.encryptedFailed) {
            throw new IllegalStateException("凭据已加密但拿不到密钥（App 未装或不在探测路径，重登无效）：" + sourcePath);
        }
        if (res.value.isEmpty()) return null;

        WbPoolCredential c = new WbPoolCredential();
        c.setAccessToken(res.value);
        c.setRefreshToken(field(auth.get("refreshToken"), keysById, sourcePath).value);
        c.setExpiresAtMs(ms(auth.get("expiresAt")));
        Long rExp = optMs(auth.get("refreshExpiresAt"));
        c.setRefreshExpiresAtMs(rExp);
        if (rExp != null && rExp > 0 && rExp < System.currentTimeMillis()) {
            return null; // 刷新窗口已关闭，无法恢复
        }
        c.setLastRefreshAtMs(optMs(auth.get("lastRefreshTime")));
        c.setNickname(str(field(identity.get("nickname"), keysById, sourcePath).value));
        c.setUin(str(identity.getString("uin")));
        c.setUid(str(identity.getString("uid")));
        c.setEnterpriseId(str(identity.getString("enterpriseId")));
        c.setDomain(auth.getString("domain") == null ? "" : auth.getString("domain"));
        c.setSourcePath(sourcePath);
        c.setLive(live);
        return c;
    }

    /** 字段读取：明文 / $wbEncrypted（解不开置 encryptedFailed，不误报为空值） */
    private static FieldRes field(Object v, Map<String, byte[]> keys, String sourcePath) {
        FieldRes r = new FieldRes();
        if (v instanceof String) {
            r.value = (String) v;
            return r;
        }
        if (WbAtRestCrypto.isEncryptedField(v)) {
            r.encrypted = true;
            try {
                r.value = WbAtRestCrypto.openField((JSONObject) v, keys);
            } catch (Exception e) {
                log.warn("[WBPOOL] 加密字段打不开 {}：{}", sourcePath, e.getMessage());
                r.encryptedFailed = true;
            }
        }
        return r;
    }

    private static class FieldRes {
        String value = "";
        boolean encrypted;
        boolean encryptedFailed;
    }

    /** 秒/毫秒归一为毫秒；无效为 0 */
    private static long ms(Object v) {
        Long x = optMs(v);
        return x == null ? 0L : x;
    }

    private static Long optMs(Object v) {
        if (!(v instanceof Number)) return null;
        long n = ((Number) v).longValue();
        if (n <= 0) return null;
        return n > 1_000_000_000_000L ? n : n * 1000L;
    }

    private static String str(String s) {
        return s == null || s.isEmpty() ? null : s;
    }
}