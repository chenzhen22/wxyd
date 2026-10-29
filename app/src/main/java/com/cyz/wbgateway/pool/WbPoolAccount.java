package com.cyz.wbgateway.pool;

import lombok.Data;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 池内账号：凭据 + 容错状态（照 xdpool WorkBuddyAccount 的冷却语义）。
 */
@Data
public class WbPoolAccount {

    private String id;
    private String label;
    private WbPoolCredential credential;
    /**
     * 同一账号的备用凭据：同一个人的桌面 App 与浏览器登录会各产出一份，去重后仍全部保留。
     * 主凭据被上游判死（401/刷新失败）时轮换到下一份，判死不再等于整个账号不能用。
     */
    private List<WbPoolCredential> alternates = new ArrayList<>();
    /** 账号级冷却截止（积分耗尽等，无模型维度时用） */
    private long cooldownUntilMs;
    /** 按模型冷却：上游限流是模型级的（"可切换其他模型继续使用"） */
    private Map<String, Long> modelCooldowns = new LinkedHashMap<>();
    private int rateLimitHits;
    /** 上游拒绝登录态（401/403）后的长冷却；重新登录/刷新成功才清除 */
    private long credentialDeadUntilMs;
    private long lastSuccessAtMs;
    private String lastError;
    /** 管理员开关：false = 模型调用不再使用该账号（仅影响 pick()，展示/积分查询不受影响） */
    private boolean enabled = true;

    /** 主凭据 + 备用凭据（主在前） */
    public List<WbPoolCredential> allCredentials() {
        List<WbPoolCredential> all = new ArrayList<>();
        if (credential != null) all.add(credential);
        if (alternates != null) {
            for (WbPoolCredential c : alternates) if (c != null) all.add(c);
        }
        return all;
    }

    public boolean available(long now, String model) {
        if (!enabled) return false;
        if (credentialDeadUntilMs > now || cooldownUntilMs > now) return false;
        Long until = modelCooldowns.get(model);
        return until == null || until <= now;
    }
}