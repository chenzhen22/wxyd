package com.cyz.wbgateway;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import com.cyz.wbgateway.pool.WbPoolCredential;
import okhttp3.MediaType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * WorkBuddy 上游客户端（dsh-claude-gateway 的 Java 移植）。
 * <p>
 * 凭据来自 com.cyz.wbgateway.pool.WbAccountPool（纯 Java 解密桌面 App auth 文件，
 * 多账号自动容错；dsh-xdpool 的 Java 移植），本类只做上游 HTTP（chat SSE / 目录 / 令牌刷新 / 积分）。
 * 协议细节照 dsh-workbuddy-connect 0.7.x 与 dsh-xdpool 逆向对齐，WorkBuddy 更新可能需要随之调整。
 */
@Component
@Slf4j
public class WbWorkBuddyClient {

    private static final String CN_CHAT_BASE = "https://copilot.tencent.com";
    private static final String CN_BILLING_BASE = "https://www.codebuddy.cn";
    /** CLI 形态 UA（模型目录用，目录按客户端身份分家，不能换成 App 形态） */
    private static final String CATALOG_UA = "CLI/2.63.2 CodeBuddy/2.63.2";
    /** 插件 0.7.x 内置兜底目录（实时目录拉不到时使用）；免费模型以实时目录为准 */
    private static final String[] FALLBACK_MODELS = {
            "hy4-preview", "hy3", "hy3-x", "deepseek-v4.1-flash", "glm-5.3", "glm-5.3-flash",
            "glm-5.2", "glm-5.1", "glm-5v-turbo", "minimax-m3", "minimax-m2.7", "kimi-k3-1",
            "kimi-k2.8-preview", "kimi-k2.7", "kimi-k2.6", "deepseek-v4-pro"
    };
    private static final String[] FALLBACK_FREE_MODELS = {"hy3"};

    /** 积分不足类标记（含中文），照插件 HARD_CREDIT_MARKERS */
    private static final String[] HARD_CREDIT_MARKERS = {
            "insufficient credit", "no credit", "credit exhausted", "credits exhausted",
            "out of credit", "quota exceeded", "quota exhaust", "payment required",
            "credit not enough", "not enough credit",
            "积分不足", "额度不足", "余额不足", "积分用完", "额度用尽", "没有积分"
    };
    /** 会话失效标记：命中说明桌面登录态被上游判死，需重新桥接取新令牌 */
    private static final String[] SESSION_DEAD_MARKERS = {"Offline user session not found", "12153"};

    @Value("${wxyd.wbgateway.client-version:5.5.6}")
    private String clientVersion;

    @Value("${wxyd.wbgateway.chat-timeout-seconds:300}")
    private int chatTimeoutSeconds;

    @Value("${wxyd.wbgateway.default-model:glm-5.3}")
    private String defaultModel;

    @Resource
    private com.cyz.wbgateway.pool.WbAccountPool accountPool;

    private OkHttpClient http;

    // ---- 凭据缓存（TTL）与模型目录缓存 ----
    /** accountId -> 积分（10 分钟/号缓存） */
    private final Map<String, CreditsInfo> creditsCache = new java.util.concurrent.ConcurrentHashMap<>();

    private final Object modelLock = new Object();
    private WbModelCatalog cachedModels;
    private long cachedModelsAt;

    private final Object statusLock = new Object();
    private Map<String, Object> cachedStatus;
    private long cachedStatusAt;

    @PostConstruct
    public void init() {
        OkHttpClient.Builder b = new OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .readTimeout(chatTimeoutSeconds, TimeUnit.SECONDS)
                .retryOnConnectionFailure(true);
        http = b.build();
    }

    /* ==================== 凭据（多账号池） ==================== */

    /**
     * 选一个可用账号并返回；令牌临期（<60s）自动刷新。
     * 刷新失败抛 WbUpstreamException(session_dead)，由调用方决定换号。
     */
    public com.cyz.wbgateway.pool.WbPoolAccount acquireAccount(String model) {
        accountPool.scanIfNeeded();
        com.cyz.wbgateway.pool.WbPoolAccount acc = accountPool.pick(model, null);
        WbPoolCredential c = acc.getCredential();
        long now = System.currentTimeMillis();
        boolean expiring = c.getExpiresAtMs() <= 0 || c.getExpiresAtMs() < now + 60_000L;
        if (expiring && c.getRefreshToken() != null && !c.getRefreshToken().isEmpty()) {
            refreshAccount(acc.getId());
        }
        return acc;
    }

    /** 调上游刷新端点换新令牌（xdpool refreshToken 的 Java 版），成功回写池并清冷却 */
    public void refreshAccount(String accountId) {
        com.cyz.wbgateway.pool.WbPoolAccount acc = accountPool.get(accountId);
        if (acc == null) throw new WbUpstreamException(WbUpstreamException.KIND_SESSION_DEAD, 0, "account gone: " + accountId);
        WbPoolCredential c = acc.getCredential();
        if (c.getRefreshToken() == null || c.getRefreshToken().isEmpty()) {
            throw new WbUpstreamException(WbUpstreamException.KIND_SESSION_DEAD, 0, "no refresh token（需在桌面 App 重新登录）");
        }
        try {
            Request req = new Request.Builder()
                    .url(chatBase(c.toWbCredential()) + "/v2/plugin/auth/token/refresh")
                    .header("Authorization", "Bearer " + c.getAccessToken())
                    .header("Accept", "application/json")
                    .header("X-Refresh-Token", c.getRefreshToken())
                    .header("X-Auth-Refresh-Source", "workbuddy")
                    .header("X-Requested-With", "XMLHttpRequest")
                    .header("Origin", originReferer(c.toWbCredential()))
                    .header("Referer", originReferer(c.toWbCredential()) + "/")
                    .header("User-Agent", chatUserAgent(c.toWbCredential()))
                    .header(c.getUid() == null || c.getUid().isEmpty() ? "X-No-User-Id" : "X-User-Id",
                            c.getUid() == null || c.getUid().isEmpty() ? "1" : c.getUid())
                    .header(c.getEnterpriseId() == null || c.getEnterpriseId().isEmpty() ? "X-No-Enterprise-Id" : "X-Enterprise-Id",
                            c.getEnterpriseId() == null || c.getEnterpriseId().isEmpty() ? "1" : c.getEnterpriseId())
                    .header(c.getDomain() == null || c.getDomain().isEmpty() ? "X-No-Department-Info" : "X-Domain",
                            c.getDomain() == null || c.getDomain().isEmpty() ? "1" : c.getDomain())
                    .header("X-IDE-Type", "WorkBuddy")
                    .header("X-IDE-Name", "WorkBuddy")
                    .header("X-IDE-Version", clientVersion)
                    .header("X-Product", "SaaS")
                    .post(RequestBody.create(MediaType.parse("application/json; charset=utf-8"), new byte[0]))
                    .build();
            try (Response resp = http.newCall(req).execute()) {
                String text = resp.body() == null ? "" : resp.body().string();
                if (!resp.isSuccessful()) throw new WbUpstreamException(classify(resp.code(), text), resp.code(), excerpt(text));
                JSONObject envelope = JSON.parseObject(text);
                if (envelope == null || envelope.getIntValue("code") != 0) {
                    String msg = envelope == null ? "空响应" : envelope.getString("msg");
                    throw new WbUpstreamException(classify(resp.code(), msg), resp.code(), excerpt(msg));
                }
                JSONObject data = envelope.getJSONObject("data");
                String access = data == null ? null : data.getString("accessToken");
                if (access == null || access.isEmpty()) {
                    throw new WbUpstreamException(WbUpstreamException.KIND_SESSION_DEAD, 0, "refresh 无 accessToken（需重新登录）");
                }
                WbPoolCredential n = new WbPoolCredential();
                n.setAccessToken(access);
                n.setRefreshToken(data.getString("refreshToken") != null ? data.getString("refreshToken") : c.getRefreshToken());
                Integer expiresIn = data == null ? null : data.getInteger("expiresIn");
                n.setExpiresAtMs(expiresIn != null && expiresIn > 0
                        ? System.currentTimeMillis() + expiresIn * 1000L
                        : (c.getExpiresAtMs() == null ? 0L : c.getExpiresAtMs()));
                n.setDomain(data != null && data.getString("domain") != null ? data.getString("domain") : c.getDomain());
                n.setUid(c.getUid());
                n.setEnterpriseId(c.getEnterpriseId());
                n.setUin(c.getUin());
                n.setNickname(c.getNickname());
                n.setLastRefreshAtMs(System.currentTimeMillis());
                accountPool.onRefreshed(accountId, n);
            }
        } catch (WbUpstreamException e) {
            throw e;
        } catch (Exception e) {
            throw new WbUpstreamException(WbUpstreamException.KIND_SERVER, 0, "refresh transport error: " + e.getMessage());
        }
    }

    /** 池概览：每账号登录态/积分/冷却（积分 10 分钟/号缓存，整体 60s 缓存） */
    public Map<String, Object> bridgeStatus() {
        synchronized (statusLock) {
            long now = System.currentTimeMillis();
            if (cachedStatus != null && now - cachedStatusAt < 60_000L) return cachedStatus;
            accountPool.scanIfNeeded();
            List<Map<String, Object>> rows = accountPool.statusRows();
            long healthy = 0;
            for (Map<String, Object> row : rows) {
                String id = String.valueOf(row.get("id"));
                CreditsInfo ci = creditsFor(id);
                row.put("credits", ci.value);
                row.put("creditsError", ci.error);
                boolean alive = row.get("cooldownUntilMs") == null && row.get("credentialDeadUntilMs") == null;
                if (alive) healthy++;
            }
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("poolSize", rows.size());
            m.put("healthy", healthy);
            m.put("accounts", rows);
            m.put("skipped", accountPool.skipped());
            m.put("modelsSource", cachedModels != null ? cachedModels.source : null);
            cachedStatus = m;
            cachedStatusAt = now;
            return m;
        }
    }

    /** 单账号积分查询（xdpool fetchCredits 的 Java 版）：401 会先刷新令牌重试一次 */
    private CreditsInfo creditsFor(String accountId) {
        long now = System.currentTimeMillis();
        CreditsInfo cached = creditsCache.get(accountId);
        if (cached != null && now - cached.at < 600_000L) return cached;
        CreditsInfo ci = new CreditsInfo();
        try {
            ci.value = queryCredits(accountId);
        } catch (WbUpstreamException e) {
            if (WbUpstreamException.KIND_SESSION_DEAD.equals(e.getKind())) {
                // 死 token（存量 expires 是假象）：刷新一次再重试；仍失败则账号判死
                try {
                    refreshAccount(accountId);
                    ci.value = queryCredits(accountId);
                } catch (Exception e2) {
                    // 带上刷新失败的具体原因（上游 msg / refresh 无 accessToken 等），便于区分
                    // 「真过期需重新登录」与「上游抖动误判」
                    log.warn("[WBGATEWAY] 账号 {} 积分查询 401 且刷新失败：{}", accountId, e2.getMessage());
                    accountPool.onCredentialDead(accountId, "积分查询 401 且刷新失败：" + e2.getMessage());
                    ci.error = "登录态失效且刷新失败，需在桌面 App 重新登录";
                }
            } else {
                ci.error = brief(e.getMessage());
            }
        } catch (Exception e) {
            ci.error = brief(e.getMessage());
        }
        ci.at = now;
        creditsCache.put(accountId, ci);
        return ci;
    }

    private long queryCredits(String accountId) throws Exception {
        com.cyz.wbgateway.pool.WbPoolAccount acc = accountPool.get(accountId);
        if (acc == null) throw new IllegalStateException("账号不在池内");
        WbCredential cred = acc.getCredential().toWbCredential();
        String fmt = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date());
        String end = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
                .format(new java.util.Date(System.currentTimeMillis() + 365L * 101 * 24 * 3600 * 1000));
        JSONObject body = new JSONObject();
        body.put("PageNumber", 1);
        body.put("PageSize", 100);
        body.put("ProductCode", "p_tcaca");
        body.put("Status", new int[]{0, 3});
        body.put("PackageEndTimeRangeBegin", fmt);
        body.put("PackageEndTimeRangeEnd", end);
        Request.Builder rb = new Request.Builder()
                .url(billingBase(cred) + "/v2/billing/meter/get-user-resource")
                .header("Authorization", "Bearer " + cred.getAccessToken())
                .header("Accept", "application/json")
                .header("Content-Type", "application/json")
                .post(RequestBody.create(MediaType.parse("application/json; charset=utf-8"), body.toJSONString()));
        if (!cred.getUid().isEmpty()) rb.header("X-User-Id", cred.getUid());
        if (!cred.getEnterpriseId().isEmpty()) {
            rb.header("X-Enterprise-Id", cred.getEnterpriseId());
            rb.header("X-Tenant-Id", cred.getEnterpriseId());
        }
        if (!cred.getDomain().isEmpty()) rb.header("X-Domain", cred.getDomain());
        try (Response resp = http.newCall(rb.build()).execute()) {
            String text = resp.body() == null ? "" : resp.body().string();
            JSONObject envelope = null;
            try {
                envelope = JSON.parseObject(text);
            } catch (Exception parseError) {
                // WAF/网关返回 HTML 等非 JSON：只保留状态码，不透传原文
                throw new WbUpstreamException(classify(resp.code(), text), resp.code(),
                        "HTTP " + resp.code() + " 非 JSON 响应");
            }
            if (!resp.isSuccessful() || envelope == null || envelope.getIntValue("code") != 0) {
                String msg = envelope == null ? "空响应" : envelope.getString("msg");
                throw new WbUpstreamException(classify(resp.code(), text), resp.code(), excerpt(msg));
            }
            JSONObject data = envelope.getJSONObject("data");
            JSONObject resp0 = data == null ? null : data.getJSONObject("Response");
            JSONObject d = resp0 == null ? null : resp0.getJSONObject("Data");
            JSONArray accounts = d == null ? null : d.getJSONArray("Accounts");
            long total = 0;
            if (accounts != null) {
                for (int i = 0; i < accounts.size(); i++) {
                    JSONObject a = accounts.getJSONObject(i);
                    if (a == null) continue;
                    boolean monthly = a.getIntValue("CapacityType") == 4;
                    long remain = monthly ? a.getLongValue("CycleCapacityRemain") : a.getLongValue("CapacityRemain");
                    if (remain < 0) remain = 0;
                    if (!monthly) {
                        long expMs = parseApiDate(a.getString("ExpiredTime"));
                        if (remain <= 0 || (expMs > 0 && expMs <= now())) continue;
                    }
                    total += remain;
                }
            }
            return total;
        }
    }

    private static long now() {
        return System.currentTimeMillis();
    }

    private static long parseApiDate(String s) {
        if (s == null || s.isEmpty()) return 0;
        try {
            return new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(s).getTime();
        } catch (Exception e) {
            try { return Long.parseLong(s.trim()); } catch (Exception ignore) { return 0; }
        }
    }

    private static class CreditsInfo {
        Object value;
        String error;
        long at;
    }

    /* ==================== 模型目录 ==================== */

    /** 实时模型目录（10 分钟缓存），失败退回内置兜底目录，不抛异常 */
    public WbModelCatalog models() {
        synchronized (modelLock) {
            long now = System.currentTimeMillis();
            if (cachedModels != null && now - cachedModelsAt < 600_000L) return cachedModels;
            try {
                WbCredential cred = acquireAccount("catalog").getCredential().toWbCredential();
                Request req = new Request.Builder()
                        .url(chatBase(cred) + "/v3/config")
                        .header("Authorization", "Bearer " + cred.getAccessToken())
                        .header("Accept", "application/json")
                        .header("Origin", originReferer(cred))
                        .header("Referer", originReferer(cred) + "/")
                        .header("User-Agent", CATALOG_UA)
                        .get()
                        .build();
                try (Response resp = http.newCall(req).execute()) {
                    String text = resp.body() == null ? "" : resp.body().string();
                    if (!resp.isSuccessful()) {
                        throw new WbUpstreamException(classify(resp.code(), text), resp.code(), excerpt(text));
                    }
                    JSONObject envelope = JSON.parseObject(text);
                    if (envelope == null || envelope.getIntValue("code") != 0) {
                        String msg = envelope == null ? "空响应" : envelope.getString("msg");
                        throw new WbUpstreamException(classify(resp.code(), msg), resp.code(), excerpt(msg));
                    }
                    WbModelCatalog catalog = WbModelCatalog.parse(envelope.getJSONObject("data"));
                    catalog.source = "live";
                    cachedModels = catalog;
                    cachedModelsAt = System.currentTimeMillis();
                    return catalog;
                }
            } catch (Exception e) {
                log.warn("[WBGATEWAY] 实时模型目录拉取失败，退回兜底目录：{}", e.getMessage());
                WbModelCatalog fallback = new WbModelCatalog();
                fallback.source = "fallback";
                for (String id : FALLBACK_MODELS) {
                    WbModelCatalog.ModelInfo info = new WbModelCatalog.ModelInfo();
                    info.id = id;
                    info.name = id;
                    info.free = false;
                    fallback.models.add(info);
                }
                for (String id : FALLBACK_FREE_MODELS) {
                    WbModelCatalog.ModelInfo info = fallback.byId(id);
                    if (info != null) info.free = true;
                }
                if (cachedModels == null) {
                    cachedModels = fallback;
                    cachedModelsAt = System.currentTimeMillis();
                }
                return fallback;
            }
        }
    }

    /** 清目录缓存（管理端点排查用） */
    public void invalidateModels() {
        synchronized (modelLock) {
            cachedModels = null;
        }
    }

    /* ==================== chat 上游 ==================== */

    /**
     * 发起上游 chat（POST /v2/chat/completions，body 已是 OpenAI 形态 + stream:true）。
     * 成功返回未消费的 Response（调用方负责关闭，body 为 OpenAI 风格 SSE）；
     * 失败抛 {@link WbUpstreamException}。
     */
    public Response chatStream(WbCredential cred, String bodyJson) {
        Request req = new Request.Builder()
                .url(chatBase(cred) + "/v2/chat/completions")
                .header("Authorization", "Bearer " + cred.getAccessToken())
                .header("Accept", "application/json, text/plain, */*")
                .header("X-Requested-With", "XMLHttpRequest")
                .header("Origin", originReferer(cred))
                .header("Referer", originReferer(cred) + "/")
                .header("User-Agent", chatUserAgent(cred))
                .header("Content-Type", "application/json")
                .header(cred.getUid().isEmpty() ? "X-No-User-Id" : "X-User-Id",
                        cred.getUid().isEmpty() ? "1" : cred.getUid())
                .header(cred.getEnterpriseId().isEmpty() ? "X-No-Enterprise-Id" : "X-Enterprise-Id",
                        cred.getEnterpriseId().isEmpty() ? "1" : cred.getEnterpriseId())
                .header(cred.getDomain().isEmpty() ? "X-No-Department-Info" : "X-Domain",
                        cred.getDomain().isEmpty() ? "1" : cred.getDomain())
                .header("X-IDE-Type", "WorkBuddy")
                .header("X-IDE-Name", "WorkBuddy")
                .header("X-IDE-Version", clientVersion)
                .header("X-Product", "SaaS")
                .post(RequestBody.create(MediaType.parse("application/json; charset=utf-8"), bodyJson))
                .build();
        try {
            Response resp = http.newCall(req).execute();
            if (resp.isSuccessful()) return resp;
            String text = resp.body() == null ? "" : resp.body().string();
            int code = resp.code();
            resp.close();
            throw new WbUpstreamException(classify(code, text), code, excerpt(text));
        } catch (WbUpstreamException e) {
            throw e;
        } catch (Exception e) {
            throw new WbUpstreamException(WbUpstreamException.KIND_SERVER, 0, "transport error: " + e.getMessage());
        }
    }

    /* ==================== 请求体整理（照插件 prepareChatBody / prepareCnChatBody） ==================== */

    /**
     * 把 OpenAI chat-completions 请求体整理成上游接受的形态：
     * 强制 stream:true、developer→system、摊平 tool_choice，再剥离不支持的 effort 拼写。
     */
    public static String prepareBody(String bodyJson) {
        JSONObject body;
        try {
            body = JSON.parseObject(bodyJson);
        } catch (Exception e) {
            return bodyJson;
        }
        if (body == null) return bodyJson;
        body.put("stream", true);
        normalizeDeveloperRole(body);
        normalizeToolChoice(body);
        dropUnsupportedEffort(body);
        return body.toJSONString();
    }

    private static void normalizeDeveloperRole(JSONObject body) {
        JSONArray messages = body.getJSONArray("messages");
        if (messages == null) return;
        for (int i = 0; i < messages.size(); i++) {
            JSONObject m = messages.getJSONObject(i);
            if (m != null && "developer".equals(m.getString("role"))) m.put("role", "system");
        }
    }

    /** OpenAI tool_choice 拼写 -> 上游字符串形态；none 时连 tools 一起删 */
    private static void normalizeToolChoice(JSONObject body) {
        if (!body.containsKey("tool_choice")) return;
        Object choice = body.get("tool_choice");
        if (choice instanceof String) {
            if (((String) choice).trim().equalsIgnoreCase("none")) {
                body.remove("tool_choice");
                body.remove("tools");
                body.remove("functions");
            }
            return;
        }
        if (choice instanceof JSONObject) {
            JSONObject c = (JSONObject) choice;
            String type = c.getString("type");
            type = type == null ? "" : type.trim().toLowerCase();
            if (type.equals("none")) {
                body.remove("tool_choice");
                body.remove("tools");
                body.remove("functions");
            } else if (type.equals("auto")) {
                body.put("tool_choice", "auto");
            } else if (type.equals("required") || type.equals("any")) {
                body.put("tool_choice", "required");
            } else if (type.equals("function")) {
                JSONObject fn = c.getJSONObject("function");
                if (fn != null && fn.getString("name") != null) {
                    body.put("tool_choice", "required");
                } else {
                    body.put("tool_choice", "auto");
                }
            }
        }
    }

    /** 上游不接受 effort "off"（adapter 的关闭思考拼写），从线上剥掉 */
    private static void dropUnsupportedEffort(JSONObject body) {
        if ("off".equals(body.getString("reasoning_effort"))) body.remove("reasoning_effort");
    }

    /* ==================== 错误分类 ==================== */

    /** 照插件 classifyUpstreamError：402/积分标记 -> hard_credit；401/会话标记 -> session_dead */
    public static String classify(int status, String body) {
        String text = body == null ? "" : body;
        if (status == 402) return WbUpstreamException.KIND_HARD_CREDIT;
        if (status == 401) return WbUpstreamException.KIND_SESSION_DEAD;
        String lower = text.toLowerCase();
        for (String marker : HARD_CREDIT_MARKERS) {
            if (lower.contains(marker.toLowerCase()) || text.contains(marker)) return WbUpstreamException.KIND_HARD_CREDIT;
        }
        for (String marker : SESSION_DEAD_MARKERS) {
            if (text.contains(marker)) return WbUpstreamException.KIND_SESSION_DEAD;
        }
        if (status == 429) return WbUpstreamException.KIND_SOFT_RATE;
        if (status == 404) return WbUpstreamException.KIND_NOT_FOUND;
        if (status >= 500) return WbUpstreamException.KIND_SERVER;
        return WbUpstreamException.KIND_CLIENT;
    }

    /** 从上游失败详情里抽出可读信息（displayMsg.zh/en 优先），隐藏 token */
    public static String extractDisplayErrorMessage(String body) {
        String trimmed = body == null ? "" : body.trim();
        if (!trimmed.startsWith("{")) return null;
        try {
            JSONObject obj = JSON.parseObject(trimmed);
            if (obj == null) return null;
            JSONObject displayMsg = obj.getJSONObject("displayMsg");
            if (displayMsg != null) {
                String zh = displayMsg.getString("zh");
                if (zh != null && !zh.trim().isEmpty()) return zh.trim();
                String en = displayMsg.getString("en");
                if (en != null && !en.trim().isEmpty()) return en.trim();
            }
            String msg = obj.getString("msg");
            if (msg != null && !msg.trim().isEmpty()) return msg.trim();
        } catch (Exception ignore) {
            // 非 JSON 保持原样
        }
        return null;
    }

    /** 上游失败 -> 重试候选链上的下一个免费模型：积分不足/模型不可用/无权限类错误；WAF 拦截不算 */
    public static boolean isModelFallbackRetryable(WbUpstreamException e) {
        String detail = e.getMessage() == null ? "" : e.getMessage();
        if (WbUpstreamException.KIND_HARD_CREDIT.equals(e.getKind())
                || WbUpstreamException.KIND_NOT_FOUND.equals(e.getKind())) return true;
        if (WbUpstreamException.KIND_CLIENT.equals(e.getKind()) && !detail.contains("拦截")
                && detail.matches("(?i).*(积分|额度|credit|免费|权限|forbidden|not allowed|无权|模型不存在|invalid model|unknown model).*")) {
            return true;
        }
        return false;
    }

    /* ==================== 小工具 ==================== */

    private String chatBase(WbCredential cred) {
        return cred.isGlobal() ? "https://www.workbuddy.ai" : CN_CHAT_BASE;
    }

    private String billingBase(WbCredential cred) {
        return cred.isGlobal() ? "https://www.workbuddy.ai" : CN_BILLING_BASE;
    }

    private String originReferer(WbCredential cred) {
        return cred.isGlobal() ? "https://www.workbuddy.ai" : CN_BILLING_BASE;
    }

    /** chat UA：WorkBuddy/{v} WorkBuddy/{v}（国内版形态，照插件 chatUserAgent） */
    private String chatUserAgent(WbCredential cred) {
        String product = cred.isGlobal() ? "WorkBuddy AI" : "WorkBuddy";
        return "WorkBuddy/" + clientVersion + " " + product + "/" + clientVersion;
    }

    /* ---------------- 池记账委托（ChatService 调用） ---------------- */

    public void onSuccess(String accountId) { accountPool.onSuccess(accountId); }

    public void onCredentialDead(String accountId, String reason) { accountPool.onCredentialDead(accountId, reason); }

    public void onModelRateLimited(String accountId, String model) { accountPool.onModelRateLimited(accountId, model); }

    public void onHardCredit(String accountId) { accountPool.onHardCredit(accountId); }

    private static String brief(String s) {
        if (s == null) return "";
        return s.length() > 160 ? s.substring(0, 160) : s;
    }

    private static String excerpt(String s) {
        if (s == null) return "";
        String display = extractDisplayErrorMessage(s);
        String text = display != null ? display : s;
        return text.length() > 400 ? text.substring(0, 400) : text;
    }

    /** 默认模型（wxyd.wbgateway.default-model，未知模型/未自选时兜底） */
    public String getDefaultModel() {
        return defaultModel;
    }

    public List<String> fallbackModelIds() {
        return Collections.unmodifiableList(java.util.Arrays.asList(FALLBACK_MODELS));
    }
}
