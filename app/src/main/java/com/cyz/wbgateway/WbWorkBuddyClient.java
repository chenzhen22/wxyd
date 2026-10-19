package com.cyz.wbgateway;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.MediaType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
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
 * 凭据解密依赖 Windows 侧 WorkBuddy 桌面 App（密钥派生在桌面端完成），因此本类经 WSL interop
 * 调 Windows node.exe 执行 win-bridge.mjs（resolve/status），凭据本地按 TTL 缓存；
 * 上游 HTTP（chat SSE / 模型目录）由本类经 OkHttp 直连 copilot.tencent.com 完成。
 * 协议细节照 dsh-workbuddy-connect 插件 0.7.x 逆向对齐，WorkBuddy 更新可能需要随之调整。
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

    @Value("${wxyd.wbgateway.win-node:}")
    private String winNode;

    @Value("${wxyd.wbgateway.win-bridge-script:/mnt/d/app/dsh-claude-gateway/workbuddy/win-bridge.mjs}")
    private String winBridgeScript;

    @Value("${wxyd.wbgateway.bridge-ttl-minutes:10}")
    private int bridgeTtlMinutes;

    @Value("${wxyd.wbgateway.client-version:5.5.6}")
    private String clientVersion;

    @Value("${wxyd.wbgateway.chat-timeout-seconds:300}")
    private int chatTimeoutSeconds;

    @Value("${wxyd.wbgateway.bridge-timeout-seconds:60}")
    private int bridgeTimeoutSeconds;

    @Value("${wxyd.wbgateway.default-model:glm-5.3}")
    private String defaultModel;

    @Value("${wxyd.wbgateway.electron-bin:}")
    private String electronBin;

    private OkHttpClient http;

    // ---- 凭据缓存（TTL）与模型目录缓存 ----
    private final Object credLock = new Object();
    private WbCredential cachedCredential;
    private long cachedCredentialAt;
    private boolean credInflight;

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

    /* ==================== 凭据（win-bridge 桥接） ==================== */

    /** 找可用的 Windows node.exe（interop 桥接必需） */
    private String findWinNode() {
        List<String> candidates = new ArrayList<>();
        if (winNode != null && !winNode.trim().isEmpty()) candidates.add(winNode.trim());
        boolean onWindows = System.getProperty("os.name", "").toLowerCase().contains("windows");
        if (onWindows) candidates.add("node.exe");
        candidates.add("/mnt/c/Program Files/nodejs/node.exe");
        candidates.add("/mnt/c/Program Files (x86)/nodejs/node.exe");
        for (String c : candidates) {
            try {
                ProcessBuilder pb = new ProcessBuilder(c, "--version").redirectErrorStream(true);
                Process proc = pb.start();
                if (proc.waitFor(15, TimeUnit.SECONDS) && proc.exitValue() == 0) return c;
                proc.destroyForcibly();
            } catch (Exception ignore) {
                // 试下一个
            }
        }
        throw new IllegalStateException("找不到可用的 Windows node.exe（WSL interop 桥接需要）。"
                + "请确认 WSL interop 已开启，或配置 wxyd.wbgateway.win-node 指定 Windows node 路径。已尝试：" + candidates);
    }

    /** /mnt/x/... -> X:\...；其余原样返回（Windows 本机跑或已是 Windows 路径） */
    static String toWinPath(String p) {
        if (p == null) return null;
        java.util.regex.Matcher m = java.util.regex.Pattern
                .compile("^/mnt/([a-zA-Z])/(.+)$").matcher(p);
        if (m.matches()) {
            return m.group(1).toUpperCase() + ":\\" + m.group(2).replace('/', '\\');
        }
        return p;
    }

    /** 调 Windows 侧 win-bridge helper，返回解析后的 JSON */
    private JSONObject callWinBridge(String cmd) {
        String node = findWinNode();
        String script = toWinPath(winBridgeScript);
        boolean onLinux = System.getProperty("os.name", "").toLowerCase().contains("linux");
        if (onLinux && !winBridgeScript.startsWith("/mnt/")
                && !winBridgeScript.matches("^[A-Za-z]:[\\\\/].*")) {
            throw new IllegalStateException("win-bridge 脚本路径无法换算成 Windows 路径（需 /mnt/x/... 或 X:\\... 形式）："
                    + winBridgeScript + "，可用 wxyd.wbgateway.win-bridge-script 配置");
        }
        try {
            ProcessBuilder pb = new ProcessBuilder(node, script, cmd);
            pb.redirectErrorStream(true);
            if (electronBin != null && !electronBin.trim().isEmpty()) {
                pb.environment().put("WORKBUDDY_ELECTRON_BIN", electronBin.trim());
            }
            Process p = pb.start();
            StringBuilder out = new StringBuilder();
            try (BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = r.readLine()) != null) {
                    out.append(line).append('\n');
                }
            }
            if (!p.waitFor(bridgeTimeoutSeconds, TimeUnit.SECONDS)) {
                p.destroyForcibly();
                throw new IllegalStateException("win-bridge 超时（" + bridgeTimeoutSeconds + "s）");
            }
            String text = out.toString();
            int brace = text.indexOf('{');
            if (brace >= 0) {
                try {
                    return JSON.parseObject(text.substring(brace));
                } catch (Exception ignore) {
                    // 落到下面的兜底
                }
            }
            throw new IllegalStateException("win-bridge 无有效输出：" + text.substring(0, Math.min(text.length(), 300)));
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("win-bridge 调用失败：" + e.getMessage(), e);
        }
    }

    /**
     * 取当前凭据（TTL 缓存；过期或被上游判死后重新桥接）。
     * Windows 侧 store 在 resolve 时顺带刷新令牌并持久化，本侧不做刷新。
     */
    public WbCredential resolveCredential() {
        synchronized (credLock) {
            long ttlMs = bridgeTtlMinutes * 60_000L;
            long now = System.currentTimeMillis();
            if (cachedCredential != null && now - cachedCredentialAt < ttlMs
                    && cachedCredential.getExpiresAtMs() != null && cachedCredential.getExpiresAtMs() > now + 60_000L) {
                return cachedCredential;
            }
            if (credInflight) {
                // 极端并发下的兜底：直接再解析一次（正常不会走到）
                return doResolve();
            }
            credInflight = true;
            try {
                WbCredential c = doResolve();
                cachedCredential = c;
                cachedCredentialAt = System.currentTimeMillis();
                return c;
            } finally {
                credInflight = false;
            }
        }
    }

    private WbCredential doResolve() {
        JSONObject resp = callWinBridge("resolve");
        if (resp == null || !Boolean.TRUE.equals(resp.getBoolean("ok"))) {
            String err = resp == null ? "无输出" : resp.getString("error");
            throw new IllegalStateException("凭据不可用：" + err);
        }
        WbCredential c = WbCredential.parse(resp.getJSONObject("credential"));
        if (c.getAccessToken() == null || c.getAccessToken().isEmpty()) {
            throw new IllegalStateException("凭据缺少accessToken");
        }
        return c;
    }

    /** 令牌被上游判死（session_dead）后失效缓存，下次 resolve 重新向 Windows 桥接取新令牌 */
    public void invalidateCredential() {
        synchronized (credLock) {
            cachedCredential = null;
        }
    }

    /** 登录态 + 积分 + 模型来源（Windows 侧诊断，短缓存 60s） */
    public Map<String, Object> bridgeStatus() {
        synchronized (statusLock) {
            long now = System.currentTimeMillis();
            if (cachedStatus != null && now - cachedStatusAt < 60_000L) return cachedStatus;
            JSONObject resp = callWinBridge("status");
            if (resp == null || !Boolean.TRUE.equals(resp.getBoolean("ok"))) {
                String err = resp == null ? "无输出" : resp.getString("error");
                throw new IllegalStateException("状态诊断失败：" + err);
            }
            JSONObject st = resp.getJSONObject("status");
            Map<String, Object> m = new LinkedHashMap<>();
            if (st != null) {
                m.put("provider", st.getString("provider"));
                m.put("displayName", st.getString("displayName"));
                m.put("region", st.getString("region"));
                m.put("signIn", st.getString("signIn"));
                m.put("nickname", st.getString("nickname"));
                m.put("source", st.getString("source"));
                m.put("credits", st.get("credits"));
                m.put("creditsError", st.getString("creditsError"));
                m.put("expiresAtMs", st.getLong("expiresAtMs"));
            }
            m.put("modelsSource", cachedModels != null ? cachedModels.source : null);
            cachedStatus = m;
            cachedStatusAt = System.currentTimeMillis();
            return m;
        }
    }

    /* ==================== 模型目录 ==================== */

    /** 实时模型目录（10 分钟缓存），失败退回内置兜底目录，不抛异常 */
    public WbModelCatalog models() {
        synchronized (modelLock) {
            long now = System.currentTimeMillis();
            if (cachedModels != null && now - cachedModelsAt < 600_000L) return cachedModels;
            try {
                WbCredential cred = resolveCredential();
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

    private String originReferer(WbCredential cred) {
        return cred.isGlobal() ? "https://www.workbuddy.ai" : CN_BILLING_BASE;
    }

    /** chat UA：WorkBuddy/{v} WorkBuddy/{v}（国内版形态，照插件 chatUserAgent） */
    private String chatUserAgent(WbCredential cred) {
        String product = cred.isGlobal() ? "WorkBuddy AI" : "WorkBuddy";
        return "WorkBuddy/" + clientVersion + " " + product + "/" + clientVersion;
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
