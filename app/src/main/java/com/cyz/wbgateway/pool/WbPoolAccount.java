package com.cyz.wbgateway.pool;

import lombok.Data;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 池内账号：凭据 + 容错状态（照 xdpool WorkBuddyAccount 的冷却语义）。
 */
@Data
public class WbPoolAccount {

    private String id;
    private String label;
    private WbPoolCredential credential;
    /** 账号级冷却截止（积分耗尽等，无模型维度时用） */
    private long cooldownUntilMs;
    /** 按模型冷却：上游限流是模型级的（"可切换其他模型继续使用"） */
    private Map<String, Long> modelCooldowns = new LinkedHashMap<>();
    private int rateLimitHits;
    /** 上游拒绝登录态（401/403）后的长冷却；重新登录/刷新成功才清除 */
    private long credentialDeadUntilMs;
    private long lastSuccessAtMs;
    private String lastError;

    public boolean available(long now, String model) {
        if (credentialDeadUntilMs > now || cooldownUntilMs > now) return false;
        Long until = modelCooldowns.get(model);
        return until == null || until <= now;
    }
}