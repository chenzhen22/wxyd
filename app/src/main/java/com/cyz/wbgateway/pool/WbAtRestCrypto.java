package com.cyz.wbgateway.pool;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

/**
 * $wbEncrypted 信封开箱（dsh-xdpool at-rest.ts openEncryptedField 的 Java 移植）。
 * envelope = base64(JSON{suite,keyId,nonce,authTag,ciphertext})，
 * AES-256-GCM（nonce 12B、tag 16B），AAD 为长度前缀转录（field 帧、sym-v1 套件）。
 */
public final class WbAtRestCrypto {

    private WbAtRestCrypto() {
    }

    /** 是否 $wbEncrypted 字段包装 */
    public static boolean isEncryptedField(Object v) {
        if (!(v instanceof JSONObject)) return false;
        JSONObject o = (JSONObject) v;
        return o.getIntValue("$wbEncrypted") == 1 && o.getString("envelope") != null;
    }

    /** 解一个加密字段，返回明文（keyId 不匹配 / tag 校验失败抛异常，绝不降级为截断 token） */
    public static String openField(JSONObject wrapper, Map<String, byte[]> keys) {
        try {
            JSONObject e = JSON.parseObject(new String(
                    Base64.getDecoder().decode(wrapper.getString("envelope")), StandardCharsets.UTF_8));
            String keyId = e.getString("keyId");
            int suite = e.getIntValue("suite");
            byte[] nonce = Base64.getDecoder().decode(e.getString("nonce"));
            byte[] tag = Base64.getDecoder().decode(e.getString("authTag"));
            byte[] ct = Base64.getDecoder().decode(e.getString("ciphertext"));
            byte[] key = keys.get(keyId);
            if (key == null) {
                throw new IllegalStateException("无可用密钥：信封 keyId=" + keyId + "，现有 " + keys.keySet());
            }
            Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
            c.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, "AES"), new GCMParameterSpec(128, nonce));
            c.updateAAD(fieldAad(keyId, suite));
            byte[] plain = c.doFinal(concat(ct, tag));
            return new String(plain, StandardCharsets.UTF_8);
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("信封解密失败：" + brief(e.getMessage()), e);
        }
    }

    /** AAD 转录（照 App buildAuthenticatedContextAad / xdpool fieldAad，逐字段核对过真机） */
    private static byte[] fieldAad(String keyId, int suite) {
        try {
            return concat(
                    "WB-AAD\0".getBytes("ASCII"),
                    new byte[]{1},
                    lp("WBEV1"),
                    lp("sym-v1"),
                    u32(suite),
                    lp(keyId),
                    new byte[]{2},   // field 帧
                    new byte[]{0},   // 无序列号
                    new byte[]{0});  // 无 final
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private static byte[] u32(int v) {
        return new byte[]{(byte) (v >>> 24), (byte) (v >>> 16), (byte) (v >>> 8), (byte) v};
    }

    private static byte[] lp(String s) {
        byte[] b = s.getBytes(StandardCharsets.UTF_8);
        byte[] o = new byte[4 + b.length];
        System.arraycopy(u32(b.length), 0, o, 0, 4);
        System.arraycopy(b, 0, o, 4, b.length);
        return o;
    }

    private static byte[] concat(byte[]... parts) {
        int n = 0;
        for (byte[] p : parts) n += p.length;
        byte[] o = new byte[n];
        int off = 0;
        for (byte[] p : parts) {
            System.arraycopy(p, 0, o, off, p.length);
            off += p.length;
        }
        return o;
    }

    private static String brief(String s) {
        return s == null ? "" : (s.length() > 120 ? s.substring(0, 120) : s);
    }
}