package com.cyz.wbgateway;

import com.alibaba.fastjson2.JSONObject;
import lombok.Data;

/**
 * WorkBuddy 桌面 App 登录态凭据（由 Windows 侧 win-bridge 解密后经 interop 返回）。
 * 字段语义与 dsh-workbuddy-connect 插件的 WorkBuddyCredential 一致；
 * domain 为空=国内版（copilot.tencent.com），workbuddy.ai=国际版。
 */
@Data
public class WbCredential {

    private String accessToken = "";
    private String refreshToken = "";
    private String uid = "";
    private String enterpriseId = "";
    private String domain = "";
    private Long expiresAtMs = 0L;

    public static WbCredential parse(JSONObject o) {
        WbCredential c = new WbCredential();
        if (o == null) return c;
        c.accessToken = o.getString("accessToken");
        c.refreshToken = o.getString("refreshToken");
        c.uid = o.getString("uid");
        c.enterpriseId = o.getString("enterpriseId");
        c.domain = o.getString("domain");
        Long exp = o.getLong("expiresAtMs");
        c.expiresAtMs = exp == null ? 0L : exp;
        if (c.accessToken == null) c.accessToken = "";
        if (c.refreshToken == null) c.refreshToken = "";
        if (c.uid == null) c.uid = "";
        if (c.enterpriseId == null) c.enterpriseId = "";
        if (c.domain == null) c.domain = "";
        return c;
    }

    public boolean isGlobal() {
        String d = domain == null ? "" : domain.trim().toLowerCase();
        return d.equals("workbuddy.ai") || d.endsWith(".workbuddy.ai");
    }
}
