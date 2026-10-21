package com.cyz.service;

import com.cyz.config.Sm4KeyHolder;
import com.cyz.mapper.gateway.WbGatewayMapper;
import com.cyz.pojo.WbGatewayKey;
import com.cyz.util.SMUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * AI 工具网关 API Key 管理：每用户最多 {@link WbGatewayKey#MAX_KEYS_PER_USER} 把。
 * <p>
 * 存储照 wxyd 惯例：api_key 列存 SM4 密文（随机 IV，展示时可解密回显掩码），
 * key_hash 存明文 SM3 摘要供网关鉴权等值查询。
 */
@Service
@Slf4j
public class WbGatewayKeyService {

    private static final char[] HEX = "0123456789abcdef".toCharArray();
    private static final SecureRandom RANDOM = new SecureRandom();

    /** last_used_time 更新节流：同一 Key 60s 内只写一次，避免流式请求写风暴 */
    private final Map<Long, Long> lastTouch = new ConcurrentHashMap<>();

    @Autowired
    private WbGatewayMapper wbGatewayMapper;

    @Autowired
    private Sm4KeyHolder sm4KeyHolder;

    public int countByUser(Long userId) {
        Integer n = wbGatewayMapper.countByUserId(userId);
        return n == null ? 0 : n;
    }

    public List<WbGatewayKey> listByUser(Long userId) {
        expireInactive();
        return wbGatewayMapper.listByUserId(userId);
    }

    /** 生成新 Key（明文只在此返回一次），model 可空 */
    public WbGatewayKey create(Long userId, String name, String model) {
        if (countByUser(userId) >= WbGatewayKey.MAX_KEYS_PER_USER) {
            throw new IllegalArgumentException("每个用户最多创建 " + WbGatewayKey.MAX_KEYS_PER_USER + " 把 Key");
        }
        String raw = generateKey();
        WbGatewayKey key = new WbGatewayKey();
        key.setUserId(userId);
        key.setName(name == null ? "" : name.trim());
        key.setApiKey(sm4KeyHolder.encrypt(raw));
        key.setKeyHash(SMUtil.sm3(raw));
        key.setModel(model == null ? "" : model.trim());
        key.setEnabled(1);
        key.setActiveDate(today());
        wbGatewayMapper.insertKey(key);
        key.setApiKey(raw); // 返回明文，前端只展示这一次
        return key;
    }

    /**
     * 删除（先验归属；行存在即成功——避免 MySQL useAffectedRows 下同值更新 0 行被误判无权）
     */
    public boolean delete(Long userId, Long id) {
        if (wbGatewayMapper.queryKeyById(id, userId) == null) return false;
        wbGatewayMapper.deleteKey(id, userId);
        return true;
    }

    /**
     * 启用/停用（先验归属再无条件更新，幂等）。
     * 启用即把生效日期设为当天——Key 只在启用当天有效，次日自动停用，需再次点击启用。
     */
    public boolean toggle(Long userId, Long id, boolean enabled) {
        if (wbGatewayMapper.queryKeyById(id, userId) == null) return false;
        wbGatewayMapper.updateKeyEnabled(id, userId, enabled ? 1 : 0);
        return true;
    }

    /**
     * Key 当前是否可用：用户手动启用且生效日期为当天。
     * 生效日期早于今天（或为空）即视为过期，需用户登录后重新点击启用。
     */
    public boolean isActive(WbGatewayKey key) {
        if (key == null || !Integer.valueOf(1).equals(key.getEnabled())) return false;
        return today().equals(key.getActiveDate());
    }

    /** 自动停用：把生效日期不是今天的 Key 置为停用（定时任务 + 每次列表时兜底） */
    public int expireInactive() {
        try {
            return wbGatewayMapper.expireInactiveKeys();
        } catch (Exception e) {
            log.warn("[WBGATEWAY] 自动停用过期 Key 失败：{}", e.getMessage());
            return 0;
        }
    }

    /** 每日 00:00 把昨天的 Key 自动停用 */
    @Scheduled(cron = "0 0 0 * * ?")
    public void expireDaily() {
        int n = expireInactive();
        if (n > 0) log.info("[WBGATEWAY] 已自动停用 {} 把昨日 Key（次日需用户重新启用）", n);
    }

    private static String today() {
        return java.time.LocalDate.now().toString();
    }

    /** 修改自选模型（先验归属再无条件更新，幂等） */
    public boolean updateModel(Long userId, Long id, String model) {
        if (wbGatewayMapper.queryKeyById(id, userId) == null) return false;
        wbGatewayMapper.updateKeyModel(id, userId, model == null ? "" : model.trim());
        return true;
    }

    /** 管理员：全部用户的 Key（api_key 解密为掩码；modelOverride 转成 auto/on/off 字符串） */
    public List<Map<String, Object>> adminListKeys() {
        expireInactive();
        List<Map<String, Object>> out = new ArrayList<>();
        for (Map<String, Object> row : wbGatewayMapper.adminListKeys()) {
            Map<String, Object> m = new HashMap<>(row);
            String cipher = (String) m.remove("apiKey");
            try {
                m.put("maskedKey", WbGatewayKeyService.mask(sm4KeyHolder.decrypt(cipher)));
            } catch (Exception e) {
                m.put("maskedKey", "wb-****");
            }
            Object ov = m.get("modelOverride");
            m.put("override", ov == null ? "auto" : (Integer.valueOf(1).equals(((Number) ov).intValue()) ? "on" : "off"));
            out.add(m);
        }
        return out;
    }

    /** 管理员：设置单条 Key 的模型开关（auto=跟随全局 null / on=1 / off=0）；Key 不存在返回 false */
    public boolean adminSetModelOverride(Long id, Integer modelOverride) {
        if (wbGatewayMapper.adminQueryKeyById(id) == null) return false;
        wbGatewayMapper.adminUpdateKeyModelOverride(id, modelOverride);
        return true;
    }

    /** 本人 Key 明文（复制用；非本人返回 null） */
    public String reveal(Long userId, Long id) {
        WbGatewayKey key = wbGatewayMapper.queryKeyById(id, userId);
        if (key == null) return null;
        try {
            return sm4KeyHolder.decrypt(key.getApiKey());
        } catch (Exception e) {
            log.error("[WBGATEWAY] Key 解密失败 id={}", id);
            return null;
        }
    }

    /** 网关鉴权：明文 Key -> Key 行（未命中/已删除返回 null；是否当天有效由 {@link #isActive} 判定） */
    public WbGatewayKey lookup(String rawKey) {
        if (rawKey == null || rawKey.isEmpty()) return null;
        WbGatewayKey key = wbGatewayMapper.queryKeyByHash(SMUtil.sm3(rawKey));
        if (key == null) return null;
        try {
            String stored = sm4KeyHolder.decrypt(key.getApiKey());
            if (!stored.equals(rawKey)) return null; // 哈希碰撞兜底（概率可忽略）
        } catch (Exception e) {
            log.error("[WBGATEWAY] Key 解密校验失败 id={}", key.getId());
            return null;
        }
        return key;
    }

    /** 掩码展示：wb-****abcd */
    public static String mask(String rawKey) {
        if (rawKey == null || rawKey.length() < 8) return "wb-****";
        return rawKey.substring(0, 3) + "****" + rawKey.substring(rawKey.length() - 4);
    }

    /** 最近使用时间节流更新 */
    public void touch(Long keyId) {
        long now = System.currentTimeMillis();
        Long prev = lastTouch.get(keyId);
        if (prev != null && now - prev < 60_000L) return;
        lastTouch.put(keyId, now);
        try {
            wbGatewayMapper.touchKeyLastUsed(keyId);
        } catch (Exception e) {
            log.warn("[WBGATEWAY] 更新 last_used_time 失败 id={}", keyId);
        }
    }

    private static String generateKey() {
        byte[] buf = new byte[16];
        RANDOM.nextBytes(buf);
        char[] out = new char[32];
        for (int i = 0; i < 32; i++) out[i] = HEX[(buf[i / 2] >> ((i % 2) * 4)) & 0xf];
        return "wb-" + new String(out);
    }
}
