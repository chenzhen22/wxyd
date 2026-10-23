package com.cyz.wbgateway;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.cyz.wbgateway.pool.WbAccountPool;
import com.cyz.wbgateway.pool.WbPoolCredential;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 浏览器扫码/网页登录（vendor channel-pack buddy-oauth 协议的 Java 版）。
 * <p>
 * 账号登录态被上游判死后，管理员在管理页点按钮 → 后端取 authUrl（腾讯 SSO 扫码页）
 * → 用户浏览器打开并扫码 → 前端轮询本服务的 poll 接口 → 授权完成后凭据直接注入
 * WbAccountPool（与桌面 App 登录文件同构，现有刷新管线照常工作）。
 * <p>
 * 协议：POST /v2/plugin/auth/state 取 {state, authUrl}；轮询 GET /v2/plugin/auth/token
 * （业务码 11217=未就绪）；再轮询 GET /v2/plugin/login/account（业务码 12151=未就绪）。
 * 登录成功的凭据字段与 WbPoolCredential 一一对应。
 */
@Service
@Slf4j
public class WbBrowserLoginService {

    private static final String AUTH_STATE_PATH = "/v2/plugin/auth/state";
    private static final String AUTH_TOKEN_PATH = "/v2/plugin/auth/token";
    private static final String LOGIN_ACCOUNT_PATH = "/v2/plugin/login/account";
    /** token/账号未就绪的业务码（轮询中，继续等待） */
    private static final int CODE_TOKEN_NOT_READY = 11217;
    private static final int CODE_ACCOUNT_NOT_READY = 12151;
    /** 单次登录会话有效期（照 pack LOGIN_TIMEOUT_MS） */
    private static final long SESSION_TTL_MS = 5 * 60_000L;

    /* 产品常量（pack.js CODEBUDDY / WORKBUDDY）：同协议，只换 endpoint/UA/productCode */
    private static final String UA_CODEBUDDY = "CodeBuddyIDE/1.106.1";
    private static final String UA_WORKBUDDY = "WorkBuddy/5.5.2 WorkBuddy AI/5.5.2 CLI/5.5.2";

    /** 登录产品：buddy=国内版（与 CodeBuddy 共用端点与积分），workbuddy=国际版 */
    private static final Map<String, Product> PRODUCTS = new LinkedHashMap<>();

    static {
        Product buddy = new Product();
        buddy.id = "buddy";
        buddy.endpoint = "https://copilot.tencent.com";
        buddy.apiDomain = "copilot.tencent.com";
        buddy.platform = "ide";
        buddy.userAgent = UA_CODEBUDDY;
        PRODUCTS.put(buddy.id, buddy);
        Product wb = new Product();
        wb.id = "workbuddy";
        wb.endpoint = "https://www.workbuddy.ai";
        wb.apiDomain = "www.workbuddy.ai";
        wb.platform = "workbuddy-ai";
        wb.userAgent = UA_WORKBUDDY;
        PRODUCTS.put(wb.id, wb);
    }

    private static class Product {
        String id;
        String endpoint;
        String apiDomain;
        String platform;
        String userAgent;
    }

    /** 进行中/已完成的登录会话 */
    public static class Session {
        public String id;
        public Product product;
        public String state;
        public String authUrl;
        public long createdAt;
        /** pending | authorized | failed */
        public volatile String status = "pending";
        public volatile String message = "";
        /** 阶段推进：0=未取 token，1=已取 token 待取账号，2=完成 */
        public volatile int stage = 0;
        volatile JSONObject token;
        volatile JSONObject account;
        volatile WbPoolCredential credential;
    }

    @Value("${wxyd.wbgateway.client-version:5.5.6}")
    private String clientVersion;

    @Resource
    private WbAccountPool accountPool;

    private OkHttpClient http;

    private final Map<String, Session> sessions = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        http = new OkHttpClient.Builder()
                .connectTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
                .writeTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
                .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                .build();
    }

    /* ==================== 对外接口 ==================== */

    /** 发起登录：取 authUrl 返回给前端打开；同一管理员同时只保留一个进行中的会话 */
    public synchronized Session start(String productId) {
        Product p = PRODUCTS.get(productId);
        if (p == null) throw new IllegalArgumentException("未知登录通道：" + productId);
        // 清掉过期会话
        long now = System.currentTimeMillis();
        sessions.values().removeIf(s -> now - s.createdAt > SESSION_TTL_MS || !"pending".equals(s.status));
        try {
            Request req = new Request.Builder()
                    .url(p.endpoint + AUTH_STATE_PATH + "?platform=" + p.platform)
                    .header("X-Domain", p.apiDomain)
                    .header("X-No-Authorization", "true")
                    .header("X-No-User-Id", "true")
                    .header("X-No-Enterprise-Id", "true")
                    .header("X-No-Department-Info", "true")
                    .header("User-Agent", p.userAgent)
                    .post(okhttp3.RequestBody.create(okhttp3.MediaType.parse("application/json; charset=utf-8"), new byte[0]))
                    .build();
            JSONObject data = execute(req, "auth/state");
            String state = data.getString("state");
            String authUrl = data.getString("authUrl");
            if (state == null || state.isEmpty() || authUrl == null || authUrl.isEmpty()) {
                throw new IllegalStateException("auth/state 响应缺少 state/authUrl");
            }
            Session s = new Session();
            s.id = UUID.randomUUID().toString().replace("-", "");
            s.product = p;
            s.state = state;
            s.authUrl = authUrl;
            s.createdAt = now;
            sessions.put(s.id, s);
            log.info("[WBGATEWAY] 浏览器登录会话创建：通道 {}，会话 {}", p.id, s.id);
            return s;
        } catch (Exception e) {
            throw new IllegalStateException("获取登录链接失败：" + brief(e.getMessage()), e);
        }
    }

    /**
     * 轮询一次（前端每 2s 调用）：按阶段推进 token → 账号 → 注入账号池。
     * 返回会话快照（不含任何 token）。
     */
    public Session poll(String sessionId) {
        Session s = sessions.get(sessionId);
        if (s == null) throw new IllegalStateException("登录会话不存在或已过期，请重新发起");
        if (!"pending".equals(s.status)) return s;
        if (System.currentTimeMillis() - s.createdAt > SESSION_TTL_MS) {
            s.status = "failed";
            s.message = "登录超时（5 分钟），请重新发起";
            return s;
        }
        try {
            if (s.stage == 0) {
                pollToken(s);
            } else if (s.stage == 1) {
                pollAccount(s);
            }
        } catch (Exception e) {
            s.status = "failed";
            s.message = brief(e.getMessage());
            log.warn("[WBGATEWAY] 浏览器登录会话 {} 失败：{}", s.id, s.message);
        }
        return s;
    }

    public void cancel(String sessionId) {
        Session s = sessions.remove(sessionId);
        if (s != null) log.info("[WBGATEWAY] 浏览器登录会话 {} 已取消", s.id);
    }

    /** 可用通道 id（前端下拉用） */
    public java.util.List<String> productIds() {
        return new java.util.ArrayList<>(PRODUCTS.keySet());
    }

    /* ==================== 协议步骤 ==================== */

    /** 第一步：轮询 token（11217=未就绪，前端下次再调） */
    private void pollToken(Session s) {
        Request req = new Request.Builder()
                .url(s.product.endpoint + AUTH_TOKEN_PATH + "?state=" + s.state)
                .header("X-No-Authorization", "true")
                .header("User-Agent", s.product.userAgent)
                .get()
                .build();
        JSONObject envelope = executeRaw(req, "auth/token");
        int code = envelope.getIntValue("code");
        if (code == CODE_TOKEN_NOT_READY) return; // 未就绪，保持 pending
        if (code != 0) throw new IllegalStateException("auth/token code=" + code + "：" + brief(envelope.getString("msg")));
        JSONObject data = envelope.getJSONObject("data");
        if (data == null) return;
        s.token = data;
        s.stage = 1;
    }

    /** 第二步：轮询账号（12151=未就绪）；拿到后组装凭据并注入账号池 */
    private void pollAccount(Session s) throws Exception {
        Request req = new Request.Builder()
                .url(s.product.endpoint + LOGIN_ACCOUNT_PATH + "?state=" + s.state)
                .header("X-Domain", s.token.getString("domain") == null || s.token.getString("domain").isEmpty()
                        ? s.product.apiDomain : s.token.getString("domain"))
                .header("Authorization", "Bearer " + s.token.getString("accessToken"))
                .header("X-No-User-Id", "true")
                .header("X-No-Enterprise-Id", "true")
                .header("User-Agent", s.product.userAgent)
                .get()
                .build();
        JSONObject envelope = executeRaw(req, "login/account");
        int code = envelope.getIntValue("code");
        if (code == CODE_ACCOUNT_NOT_READY) return;
        if (code != 0) throw new IllegalStateException("login/account code=" + code + "：" + brief(envelope.getString("msg")));
        JSONObject data = envelope.getJSONObject("data");
        if (data == null) return;
        s.account = data;
        s.credential = buildCredential(s.token, data);
        accountPool.inject(s.credential);
        s.stage = 2;
        s.status = "authorized";
        s.message = "登录成功：" + (s.credential.getNickname() == null ? "" : s.credential.getNickname());
        log.info("[WBGATEWAY] 浏览器登录成功：会话 {}，账号 {} 已注入账号池", s.id, s.credential.getNickname());
    }

    /**
     * 组装凭据（pack buildCredential 的 Java 版）：expiresAt 绝对值(秒/毫秒/ISO)，
     * 缺失时退回 expiresIn 相对秒（基于 now），再缺失退回 JWT exp。
     */
    private WbPoolCredential buildCredential(JSONObject token, JSONObject account) {
        WbPoolCredential c = new WbPoolCredential();
        c.setAccessToken(str(token.getString("accessToken")));
        c.setRefreshToken(str(token.getString("refreshToken")));
        c.setExpiresAtMs(parseExpiry(token.getString("expiresAt"), token.getLong("expiresIn"), c.getAccessToken()));
        c.setRefreshExpiresAtMs(parseExpiry(token.getString("refreshExpiresAt"), token.getLong("refreshExpiresIn"), c.getAccessToken()));
        c.setLastRefreshAtMs(System.currentTimeMillis());
        c.setNickname(str(account.getString("nickname")));
        c.setUid(str(account.getString("uid")));
        c.setEnterpriseId(str(account.getString("enterpriseId")));
        c.setDomain(str(token.getString("domain")));
        c.setSourcePath("browser-login");
        return c;
    }

    private static String str(String v) {
        return v == null ? "" : v.replaceAll("[\\u0000-\\u001F\\u007F]+", " ").trim();
    }

    /** 过期时间解析：绝对值（秒/毫秒时间戳或 ISO 字符串）→ 相对秒 → JWT exp → 0 */
    static long parseExpiry(String absolute, Long relativeSeconds, String accessToken) {
        long fallback = jwtExpiresAtMs(accessToken);
        if (absolute != null && !absolute.trim().isEmpty()) {
            String v = absolute.trim();
            if (v.matches("^\\d+$")) {
                long n = Long.parseLong(v);
                long ms = n > 1_000_000_000_000L ? n : n * 1000L;
                return ms;
            }
            try {
                return java.time.Instant.parse(v).toEpochMilli();
            } catch (Exception ignore) {
                // 非 ISO，落到相对/JWT 分支
            }
        }
        if (relativeSeconds != null && relativeSeconds > 0) {
            return System.currentTimeMillis() + relativeSeconds * 1000L;
        }
        return fallback;
    }

    private static long jwtExpiresAtMs(String token) {
        if (token == null || token.isEmpty()) return 0;
        String[] parts = token.split("\\.");
        if (parts.length < 2) return 0;
        try {
            byte[] json = java.util.Base64.getUrlDecoder().decode(parts[1]);
            JSONObject payload = JSON.parseObject(new String(json, java.nio.charset.StandardCharsets.UTF_8));
            Long exp = payload == null ? null : payload.getLong("exp");
            return exp == null ? 0 : exp * 1000L;
        } catch (Exception e) {
            return 0;
        }
    }

    /* ==================== HTTP 小工具 ==================== */

    /** 请求并要求 code=0（无 11217/12151 特例的场景） */
    private JSONObject execute(Request req, String what) throws Exception {
        JSONObject envelope = executeRaw(req, what);
        if (envelope.getIntValue("code") != 0) {
            throw new IllegalStateException(what + " code=" + envelope.getIntValue("code")
                    + "：" + brief(envelope.getString("msg")));
        }
        JSONObject data = envelope.getJSONObject("data");
        if (data == null) throw new IllegalStateException(what + " 响应缺少 data");
        return data;
    }

    private JSONObject executeRaw(Request req, String what) {
        try (Response resp = http.newCall(req).execute()) {
            String text = resp.body() == null ? "" : resp.body().string();
            if (!resp.isSuccessful() && text.isEmpty()) {
                throw new IllegalStateException(what + " HTTP " + resp.code());
            }
            JSONObject envelope;
            try {
                envelope = JSON.parseObject(text);
            } catch (Exception e) {
                throw new IllegalStateException(what + " HTTP " + resp.code() + " 非 JSON 响应");
            }
            if (envelope == null) throw new IllegalStateException(what + " 空响应");
            return envelope;
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException(what + " 网络错误：" + brief(e.getMessage()));
        }
    }

    private static String brief(String s) {
        if (s == null) return "";
        // 不透传 token 等敏感内容：截断即可（state/token 均不出现在我们的异常消息里）
        return s.length() > 200 ? s.substring(0, 200) : s;
    }
}
