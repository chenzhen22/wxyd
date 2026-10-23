package com.cyz.wbgateway.qoder;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import lombok.extern.slf4j.Slf4j;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Qoder device login（dsh-connect-qoder device-login.ts 的 Java 版）。
 * <p>
 * Qoder 桌面登录态被上游判死后，管理员在管理页点按钮 → 后端生成 PKCE + 授权链接
 * （{authBase}/device/selectAccounts，浏览器打开选账号授权）→ 前端轮询本服务 →
 * 授权完成后凭据落 {@code data/qoder/device-auth.json}，QoderClient 读取优先级
 * config > device > desktop。
 * <p>
 * 协议要点（照逆向源码，勿凭记忆改）：
 * <ul>
 *   <li>poll 打在 <b>openApiBase</b> 而非 authBase（CN: openapi.qoder.com.cn）；</li>
 *   <li>404 = 待授权（健康轮询态，非错误）；2xx 无 token 响应同样视为 pending；</li>
 *   <li>verifier 是 PKCE 秘密，绝不落日志（poll URL 含 verifier）；</li>
 *   <li>PKCE verifier 字母表 66 字符、取模取样（byte % 66），长度 43..128；</li>
 *   <li>refresh 走 /api/v1/deviceToken/refresh，body 带 machine_id；rotation 必须
 *       merge 而非替换（响应不带 user_id，丢 uid 会 403 Signature invalid）。</li>
 * </ul>
 */
@Service
@Slf4j
public class QoderDeviceLoginService {

    private static final String DEVICE_SELECT_PATH = "/device/selectAccounts";
    private static final String POLL_PATH = "/api/v1/deviceToken/poll";
    private static final String REFRESH_PATH = "/api/v1/deviceToken/refresh";
    private static final String PKCE_ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-._~";
    private static final int PKCE_MIN_LENGTH = 43;
    private static final int PKCE_LENGTH_SPAN = 86;
    /** 会话 TTL（照 device-login-flow.ts 的 5 分钟预算） */
    private static final long SESSION_TTL_MS = 5 * 60_000L;
    /** 1e12 阈值：秒级时间戳永远到不了，毫秒级不会低于它（照 asEpochMs） */
    private static final long MS_THRESHOLD = 1_000_000_000_000L;

    /** CN 区产品常量（本仓库只接国内版） */
    private static final String AUTH_BASE = "https://qoder.cn";
    private static final String OPEN_API_BASE = "https://openapi.qoder.com.cn";
    private static final String CLIENT_ID = "732aef47-9cf2-46a2-95fe-4cebb5d0d1fa";
    private static final String CLIENT_METADATA_QUERY =
            "&client_type=5&business_product=cli&business_type=agent&scene=assistant";

    private static final SecureRandom RANDOM = new SecureRandom();

    private OkHttpClient http;

    @PostConstruct
    public void init() {
        http = new OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .writeTimeout(15, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build();
    }

    /* ==================== 会话 ==================== */

    public static class Session {
        public String id;
        String verifier;
        String nonce;
        String machineId;
        public String authUrl;
        long createdAt;
        /** pending | authorized | failed */
        public volatile String status = "pending";
        public volatile String message = "";
    }

    private final Map<String, Session> sessions = new ConcurrentHashMap<>();

    /* ==================== 对外接口 ==================== */

    /** 发起登录：生成 PKCE 会话与授权链接 */
    public synchronized Session start() {
        long now = System.currentTimeMillis();
        sessions.values().removeIf(s -> now - s.createdAt > SESSION_TTL_MS || !"pending".equals(s.status));
        try {
            Session s = new Session();
            s.id = UUID.randomUUID().toString().replace("-", "");
            s.verifier = pkceVerifier();
            s.nonce = UUID.randomUUID().toString();
            s.machineId = UUID.randomUUID().toString();
            // authUrl：{authBase}/device/selectAccounts?challenge&challenge_method&nonce&machine_id&client_id(+metadata)
            String challenge = sha256Base64Url(s.verifier);
            s.authUrl = AUTH_BASE + DEVICE_SELECT_PATH
                    + "?challenge=" + q(challenge)
                    + "&challenge_method=S256"
                    + "&nonce=" + q(s.nonce)
                    + "&machine_id=" + q(s.machineId)
                    + "&client_id=" + q(CLIENT_ID)
                    + CLIENT_METADATA_QUERY;
            s.createdAt = now;
            sessions.put(s.id, s);
            log.info("[QODER] device 登录会话创建：{}", s.id);
            return s;
        } catch (Exception e) {
            throw new IllegalStateException("生成登录会话失败：" + brief(e.getMessage()), e);
        }
    }

    /**
     * 轮询一次（前端每 2s 调用）：GET {openApiBase}/api/v1/deviceToken/poll?nonce&verifier&challenge_method。
     * 404 / 2xx 无 token = pending（健康轮询态，非错误）。
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
        // poll URL 含 verifier（PKCE 规定明文传输），绝不打日志
        String url = OPEN_API_BASE + POLL_PATH
                + "?nonce=" + q(s.nonce)
                + "&verifier=" + q(s.verifier)
                + "&challenge_method=S256";
        try {
            Request req = new Request.Builder().url(url)
                    .header("Accept", "application/json")
                    .get().build();
            try (Response resp = http.newCall(req).execute()) {
                int code = resp.code();
                if (code == 404) return s;
                if (code < 200 || code >= 300) {
                    s.status = "failed";
                    s.message = "poll HTTP " + code;
                    log.warn("[QODER] device poll 失败：HTTP {}", code);
                    return s;
                }
                String text = resp.body() == null ? "" : resp.body().string();
                JSONObject payload = parsePayload(text);
                if (payload == null) return s; // 无 token = 未授权
                persist(payload, s.machineId);
                s.status = "authorized";
                String who = payload.getString("userName");
                s.message = "登录成功：" + (who == null || who.isEmpty() ? payload.getString("userId") : who);
                log.info("[QODER] device 登录成功：会话 {}，uid={}", s.id, payload.getString("userId"));
            }
        } catch (java.io.IOException e) {
            // 网络抖动：保持 pending，下个周期重试（会话 5 分钟预算兜底）
            log.warn("[QODER] device poll 网络错误（保持 pending）：{}", e.getMessage());
        }
        return s;
    }

    public void cancel(String sessionId) {
        Session s = sessions.remove(sessionId);
        if (s != null) log.info("[QODER] device 登录会话 {} 已取消", s.id);
    }

    /* ==================== 凭据持久化 ==================== */

    /** device 凭据文件（QoderClient 读取优先级 config > device > desktop） */
    static Path storePath() {
        return Paths.get(System.getProperty("user.dir"), "data", "qoder", "device-auth.json");
    }

    private static synchronized void persist(JSONObject payload, String machineId) {
        try {
            JSONObject doc = new JSONObject();
            doc.put("version", 1);
            doc.put("savedAtMs", System.currentTimeMillis());
            doc.put("machineId", machineId);
            doc.put("payload", payload);
            atomicWrite(doc);
        } catch (Exception e) {
            throw new IllegalStateException("凭据保存失败：" + brief(e.getMessage()), e);
        }
    }

    /** 读 device 凭据 payload（无则返回 null）；给 QoderClient 用 */
    public static JSONObject loadStore() {
        try {
            Path p = storePath();
            if (!Files.exists(p)) return null;
            JSONObject doc = JSON.parseObject(new String(Files.readAllBytes(p), StandardCharsets.UTF_8));
            return doc == null ? null : doc.getJSONObject("payload");
        } catch (Exception e) {
            return null;
        }
    }

    /** 读 machineId（与凭据一同保存；poll 信封不含它，refresh 必须回传） */
    public static String loadMachineId() {
        try {
            Path p = storePath();
            if (!Files.exists(p)) return "";
            JSONObject doc = JSON.parseObject(new String(Files.readAllBytes(p), StandardCharsets.UTF_8));
            return doc == null || doc.getString("machineId") == null ? "" : doc.getString("machineId");
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * device refresh（照 refreshOnce + mergeRotation）：POST {openApiBase}/api/v1/deviceToken/refresh，
     * body 带 refresh_token + machine_id。rotation 必须 merge——响应不带 user_id，
     * 丢 uid 会 403 Signature invalid。成功返回 merge 后的 payload 并写回 store；
     * 401/403（refresh token 失效，需重登）或其它失败返回 null。
     */
    public static JSONObject refreshIfNeeded(String refreshToken, String machineId) {
        if (refreshToken == null || refreshToken.isEmpty() || machineId == null || machineId.isEmpty()) return null;
        try {
            OkHttpClient client = new OkHttpClient.Builder()
                    .connectTimeout(15, TimeUnit.SECONDS)
                    .readTimeout(30, TimeUnit.SECONDS).build();
            JSONObject body = new JSONObject();
            body.put("refresh_token", refreshToken);
            body.put("machine_id", machineId);
            Request req = new Request.Builder()
                    .url(OPEN_API_BASE + REFRESH_PATH)
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .post(RequestBody.create(MediaType.parse("application/json; charset=utf-8"), body.toJSONString()))
                    .build();
            try (Response resp = client.newCall(req).execute()) {
                if (resp.code() == 401 || resp.code() == 403) return null;
                if (resp.code() < 200 || resp.code() >= 300) return null;
                String text = resp.body() == null ? "" : resp.body().string();
                JSONObject rotated = parsePayload(text);
                if (rotated == null) return null;
                JSONObject current = loadStore();
                // mergeRotation：逐字段 merge，非空/正的新值才覆盖（uid/name 必然保留）
                JSONObject merged = new JSONObject();
                merged.put("token", keepText(str(current == null ? null : current.getString("token")), rotated.getString("token")));
                merged.put("refresh_token", keepText(str(current == null ? null : current.getString("refresh_token")), rotated.getString("refresh_token")));
                long exp = rotated.getLongValue("expires_at");
                merged.put("expires_at", exp > 0 ? exp : (current == null ? 0 : current.getLongValue("expires_at")));
                long refExp = rotated.getLongValue("refresh_token_expires_at");
                merged.put("refresh_token_expires_at", refExp > 0 ? refExp : (current == null ? 0 : current.getLongValue("refresh_token_expires_at")));
                merged.put("userId", keepText(str(current == null ? null : current.getString("userId")), rotated.getString("userId")));
                merged.put("userName", keepText(str(current == null ? null : current.getString("userName")), rotated.getString("userName")));
                merged.put("email", keepText(str(current == null ? null : current.getString("email")), rotated.getString("email")));
                persist(merged, machineId);
                return merged;
            }
        } catch (Exception e) {
            log.warn("[QODER] device refresh 失败：{}", brief(e.getMessage()));
            return null;
        }
    }

    /* ==================== 解析（照 parseDeviceTokenPayload：字段是拼写 LIST，缺失给空串而非 undefined） ==================== */

    /** 解析 poll/refresh 响应：无 access token 返回 null（即 pending 态） */
    static JSONObject parsePayload(String text) {
        if (text == null || text.trim().isEmpty()) return null;
        JSONObject envelope;
        try {
            envelope = JSON.parseObject(text);
        } catch (Exception e) {
            return null;
        }
        if (envelope == null) return null;
        String token = firstNonEmpty(
                envelope.getString("token"),
                envelope.getString("device_token"),
                envelope.getString("access_token"));
        if (token == null || token.isEmpty()) return null;
        JSONObject out = new JSONObject();
        out.put("token", token);
        out.put("refresh_token", str2(envelope.getString("refresh_token"), envelope.getString("refreshToken")));
        out.put("expires_at", epochMs(envelope, "expires_at", "expiresAt"));
        out.put("refresh_token_expires_at", epochMs(envelope, "refresh_token_expires_at", "refreshTokenExpiresAt"));
        out.put("userId", str2(envelope.getString("user_id"), envelope.getString("userId")));
        out.put("userName", str2(envelope.getString("user_name"), envelope.getString("userName")));
        out.put("email", str2(envelope.getString("email"), envelope.getString("user_email")));
        return out;
    }

    /** 两个候选拼写，取第一个非空；全缺失给空串 */
    private static String str2(String a, String b) {
        if (a != null && !a.isEmpty()) return a;
        if (b != null && !b.isEmpty()) return b;
        return "";
    }

    /** 单值转非空串：null 给空串 */
    private static String str(String v) {
        return v == null ? "" : v;
    }

    private static String firstNonEmpty(String... xs) {
        for (String x : xs) if (x != null && !x.isEmpty()) return x;
        return "";
    }

    /** rotation 的字符串字段：新值为空保留旧值（keepText） */
    private static String keepText(String original, String rotated) {
        return rotated == null || rotated.isEmpty() ? original : rotated;
    }

    /** epoch ms / epoch s / ISO 字符串 → ms（1e12 阈值判单位；照 asEpochMs） */
    private static long epochMs(JSONObject envelope, String... keys) {
        for (String k : keys) {
            Object v = envelope.get(k);
            if (v == null) continue;
            if (v instanceof Number) {
                long n = ((Number) v).longValue();
                if (n > 0) return n < MS_THRESHOLD ? n * 1000L : n;
            } else {
                String t = String.valueOf(v).trim();
                if (t.isEmpty()) continue;
                try {
                    long n = Long.parseLong(t);
                    if (n > 0) return n < MS_THRESHOLD ? n * 1000L : n;
                    continue;
                } catch (NumberFormatException ignore) {
                    // 非纯数字，落 ISO 解析
                }
                try {
                    return java.time.Instant.parse(t).toEpochMilli();
                } catch (Exception ignore) {
                    // 该 key 无效，试下一个
                }
            }
        }
        return 0;
    }

    /* ==================== PKCE ==================== */

    /** RFC 7636 S256 verifier：字母表 66 字符、byte % 66 取样，长度 43..128（照 createPkce） */
    private static String pkceVerifier() {
        int length = PKCE_MIN_LENGTH + RANDOM.nextInt(PKCE_LENGTH_SPAN + 1);
        byte[] bytes = new byte[length];
        RANDOM.nextBytes(bytes);
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) sb.append(PKCE_ALPHABET.charAt(bytes[i] % 66));
        return sb.toString();
    }

    private static String sha256Base64Url(String verifier) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] digest = md.digest(verifier.getBytes(StandardCharsets.US_ASCII));
        return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
    }

    private static String q(String v) {
        try {
            return java.net.URLEncoder.encode(v, "UTF-8");
        } catch (java.io.UnsupportedEncodingException e) {
            throw new IllegalStateException(e); // UTF-8 恒可用
        }
    }

    private static void atomicWrite(JSONObject doc) throws Exception {
        Path p = storePath();
        Files.createDirectories(p.getParent());
        Path tmp = p.resolveSibling(p.getFileName() + ".tmp");
        Files.write(tmp, doc.toJSONString().getBytes(StandardCharsets.UTF_8));
        Files.move(tmp, p, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
    }

    private static String brief(String s) {
        return s == null ? "" : (s.length() > 200 ? s.substring(0, 200) : s);
    }
}
