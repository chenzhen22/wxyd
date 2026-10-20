package com.cyz.wbgateway.pool;

import lombok.Data;

/**
 * 池内凭据（.info 文档解析产物），字段语义对齐 dsh-xdpool WorkBuddyCredential。
 */
@Data
public class WbPoolCredential {

    private String accessToken = "";
    private String refreshToken = "";
    private Long expiresAtMs = 0L;
    private Long refreshExpiresAtMs;
    /** 上游签发时间（auth.lastRefreshTime），比 expiresAt 可靠的新鲜度信号 */
    private Long lastRefreshAtMs;
    private String nickname;
    private String uin;
    private String uid;
    private String enterpriseId;
    private String domain = "";
    /** 来源 .info 路径（诊断用） */
    private String sourcePath = "";
    /** 是否桌面 App 的 live 登录文件（workbuddy-desktop.info） */
    private boolean live;

    /** 是否国际版（workbuddy.ai）；国际版无成长体系 */
    public boolean isGlobal() {
        String d = domain == null ? "" : domain.trim().toLowerCase();
        return d.equals("workbuddy.ai") || d.endsWith(".workbuddy.ai");
    }

    /** 转成 WbWorkBuddyClient 用的视图 */
    public com.cyz.wbgateway.WbCredential toWbCredential() {
        com.cyz.wbgateway.WbCredential c = new com.cyz.wbgateway.WbCredential();
        c.setAccessToken(accessToken);
        c.setRefreshToken(refreshToken);
        c.setUid(uid == null ? "" : uid);
        c.setEnterpriseId(enterpriseId == null ? "" : enterpriseId);
        c.setDomain(domain == null ? "" : domain);
        c.setExpiresAtMs(expiresAtMs == null ? 0L : expiresAtMs);
        return c;
    }
}