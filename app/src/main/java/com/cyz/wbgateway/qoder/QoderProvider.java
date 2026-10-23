package com.cyz.wbgateway.qoder;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.cyz.wbgateway.WbUpstreamException;
import com.cyz.wbgateway.provider.ModelCatalog;
import com.cyz.wbgateway.provider.UpstreamProvider;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Qoder 适配器（国内版 qoder-cn）：单账号凭据 + COSY 协议 + 双层 SSE 解包。
 * 模型 id 统一前缀 qoder:，与 WorkBuddy 目录并存互不冲突。
 */
@Component
@Order(20)
@Slf4j
public class QoderProvider implements UpstreamProvider {

    @Autowired
    private QoderClient client;

    /**
     * 当日额度用尽（如 "Billing daily count exceeded" / 免费模型日计数）：
     * 到该时间点前直接跳过本 provider，避免每次请求都先排队/失败一次再回落。
     */
    private volatile long creditExhaustedUntilMs;
    private volatile String creditExhaustedReason;

    @Override
    public String id() {
        return "qoder-cn";
    }

    @Override
    public String displayName() {
        return "Qoder CN";
    }

    @Override
    public ModelCatalog catalog() {
        return client.models();
    }

    @Override
    public String defaultModel() {
        QoderCatalog cat = client.models();
        for (String id : cat.ids()) {
            if (id.equalsIgnoreCase(QoderClient.prefix() + "Auto")) return id;
        }
        List<String> ids = cat.ids();
        return ids.isEmpty() ? "" : ids.get(0);
    }

    @Override
    public String modelPrefix() {
        return QoderClient.prefix();
    }

    @Override
    public int usableAccountCount() {
        if (creditExhausted()) return 0;
        return client.credentialUsable() ? 1 : 0;
    }

    /** 只有「按日/按账单计数用尽」才值得整天跳过；单个模型欠费不该黑掉整个通道 */
    private static boolean isDailyLimit(String msg) {
        String s = msg == null ? "" : msg.toLowerCase();
        return s.contains("daily count") || s.contains("daily limit")
                || s.contains("billing") || s.contains("credit usage limit");
    }

    private boolean creditExhausted() {
        return creditExhaustedUntilMs > System.currentTimeMillis();
    }

    /** 额度类失败：跳过到次日（额度按自然日重置） */
    private void markCreditExhausted(String reason) {
        if (creditExhausted()) return;
        java.time.LocalDateTime next = java.time.LocalDate.now().plusDays(1).atStartOfDay().plusMinutes(5);
        creditExhaustedUntilMs = next.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli();
        creditExhaustedReason = reason;
        log.warn("[QODER] 当日额度用尽，跳过本 provider 至 {}：{}", next, reason);
    }

    @Override
    public long remainingCredits() {
        QoderClient.Usage u = client.usage();
        return u == null ? -1 : (long) u.remaining;
    }

    @Override
    public long creditsExpireAtMs() {
        QoderClient.Usage u = client.usage();
        return u == null || u.expiresAtMs <= 0 ? Long.MAX_VALUE : u.expiresAtMs;
    }

    @Override
    public List<Map<String, Object>> accountRows() {
        List<Map<String, Object>> rows = new ArrayList<>();
        QoderClient.Credential cred = client.credential();
        if (cred == null) return rows;
        QoderClient.Usage usage = client.usage();
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", id());
        row.put("label", "备用通道 · " + (cred.name == null ? "" : cred.name));
        row.put("source", cred.source);
        row.put("domain", "");
        row.put("credits", usage == null ? null : usage.remaining);
        row.put("creditsError", usage == null ? "额度未知" : null);
        row.put("expiresAtMs", cred.expiresAtMs);
        if (!client.credentialUsable()) row.put("credentialDeadUntilMs", 1L);
        rows.add(row);
        return rows;
    }

    @Override
    public Map<String, Object> status() {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("enabled", client.isEnabled());
        QoderClient.Credential cred = client.credential();
        out.put("signedIn", cred != null);
        if (cred != null) {
            out.put("user", cred.name);
            out.put("uid", cred.uid);
            out.put("source", cred.source);
            out.put("expiresAtMs", cred.expiresAtMs);
            out.put("usable", cred.expiresAtMs <= 0 || cred.expiresAtMs > System.currentTimeMillis());
        }
        QoderClient.Usage usage = client.usage();
        if (usage != null) {
            out.put("credits", usage.remaining);
            out.put("creditsTotal", usage.total);
            out.put("creditsUnit", usage.unit);
        }
        QoderCatalog cat = client.models();
        out.put("models", cat.ids().size());
        out.put("freeModels", cat.freeIds().size());
        out.put("catalogSource", cat.source());
        return out;
    }

    @Override
    public ProviderStream open(List<String> chain, String openAiBodyJson, boolean allowModelFallback) throws WbUpstreamException {
        if (!client.isEnabled()) {
            throw new WbUpstreamException(WbUpstreamException.KIND_SERVER, 503, "Qoder 未启用");
        }
        if (creditExhausted()) {
            throw new WbUpstreamException(WbUpstreamException.KIND_SOFT_RATE, 429,
                    "模型服务当日额度已用尽，已跳过：" + creditExhaustedReason);
        }
        List<String> owned = new ArrayList<>();
        for (String id : chain) if (owns(id)) owned.add(id);
        if (owned.isEmpty()) {
            throw new WbUpstreamException(WbUpstreamException.KIND_CLIENT, 400, "没有可用的 Qoder 模型");
        }
        JSONObject body;
        try {
            body = JSON.parseObject(openAiBodyJson);
        } catch (Exception e) {
            throw new WbUpstreamException(WbUpstreamException.KIND_CLIENT, 400, "请求体不是合法 JSON");
        }
        QoderCatalog cat = client.models();
        List<QoderCatalog.ModelInfo> candidates = new ArrayList<>();
        for (String id : owned) {
            QoderCatalog.ModelInfo info = cat.byId(id);
            if (info != null) candidates.add(info);
        }
        if (candidates.isEmpty()) {
            throw new WbUpstreamException(WbUpstreamException.KIND_NOT_FOUND, 404, "模型不在 Qoder 目录中：" + owned.get(0));
        }
        WbUpstreamException last = null;
        for (int i = 0; i < candidates.size(); i++) {
            QoderCatalog.ModelInfo info = candidates.get(i);
            QoderStream stream = null;
            try {
                stream = new QoderStream(client.chat(info.key, body, info.alwaysThinking), info.id);
                // 预读首个事件：排队失败（10605）/鉴权失效在 open 阶段就暴露，网关可立刻换模型或换 provider，
                // 而不是把「排队失败」当成流式响应里的解析错误（原来会变成 502 解析响应失败）。
                stream.prefetch();
                return stream;
            } catch (WbUpstreamException e) {
                if (stream != null) stream.close();
                last = e;
                log.warn("[QODER] 模型 {} 打开失败（{}）：{}", info.id, e.getKind(), e.getMessage());
                if (WbUpstreamException.KIND_HARD_CREDIT.equals(e.getKind()) && isDailyLimit(e.getMessage())) {
                    markCreditExhausted(e.getMessage());
                }
                boolean canChain = i < candidates.size() - 1
                        && (WbUpstreamException.KIND_SOFT_RATE.equals(e.getKind())
                        || (allowModelFallback && WbUpstreamException.KIND_CLIENT.equals(e.getKind())));
                if (!canChain) throw e;
            }
        }
        throw last == null
                ? new WbUpstreamException(WbUpstreamException.KIND_SERVER, 503, "Qoder 上游不可用")
                : last;
    }

    /**
     * 双层信封 SSE 读取器：data 帧的 body 字段本身是 OpenAI chunk 的 JSON 字符串。
     * <p>
     * prefetch() 抢先读第一帧，把上游的「排队失败」信封（code=10605）转成
     * {@link WbUpstreamException#KIND_SOFT_RATE}，让网关在还没写响应头时就换模型/换 provider；
     * 已开始出字之后的错误无法再换路由，按流中断处理。
     */
    private static final class QoderStream implements ProviderStream {

        private final Response resp;
        private final BufferedReader in;
        private final String modelId;
        /** prefetch 抢出来的第一帧，read 时先吐出来 */
        private final ArrayDeque<JSONObject> ahead = new ArrayDeque<>();
        private boolean prefetched;

        QoderStream(Response resp, String modelId) {
            this.resp = resp;
            this.modelId = modelId;
            this.in = resp.body() == null ? null
                    : new BufferedReader(new InputStreamReader(resp.body().byteStream(), StandardCharsets.UTF_8), 8192);
        }

        @Override
        public String usedModel() {
            return modelId;
        }

        /** 读到第一个有效 chunk（或流结束）为止；错误信封抛 WbUpstreamException */
        void prefetch() throws WbUpstreamException {
            if (prefetched) return;
            prefetched = true;
            JSONObject first;
            try {
                first = nextChunk();
            } catch (IOException e) {
                throw new WbUpstreamException(WbUpstreamException.KIND_SERVER, 0, "读取上游响应失败：" + e.getMessage());
            }
            if (first != null) ahead.add(first);
        }

        @Override
        public void read(Consumer<JSONObject> onChunk) throws IOException {
            try {
                JSONObject c;
                while ((c = ahead.poll()) != null) onChunk.accept(c);
                while ((c = nextChunk()) != null) onChunk.accept(c);
            } catch (WbUpstreamException e) {
                // 已经进入流式阶段：只能中断，交给网关记 usage/发出错误事件
                throw new IOException(e.getMessage(), e);
            } finally {
                close();
            }
        }

        @Override
        public void close() {
            try {
                if (in != null) in.close();
            } catch (Exception ignore) {
            }
            try {
                resp.close();
            } catch (Exception ignore) {
            }
        }

        private JSONObject nextChunk() throws IOException, WbUpstreamException {
            if (in == null) return null;
            String line;
            while ((line = in.readLine()) != null) {
                String t = line.trim();
                if (!t.startsWith("data:")) continue;
                String payload = t.substring(5).trim();
                if (payload.isEmpty() || "[DONE]".equals(payload)) continue;
                JSONObject envelope;
                try {
                    envelope = JSON.parseObject(payload);
                } catch (Exception e) {
                    continue;
                }
                Object b = envelope == null ? null : envelope.get("body");
                JSONObject chunk = null;
                if (b instanceof String) {
                    if ("[DONE]".equals(((String) b).trim())) continue;
                    try {
                        chunk = JSON.parseObject((String) b);
                    } catch (Exception ignore) {
                        continue;
                    }
                } else if (b instanceof JSONObject) {
                    chunk = (JSONObject) b;
                }
                if (chunk != null && (chunk.get("choices") != null || chunk.get("usage") != null)) return chunk;
                // 错误既可能落在 body 里，也可能直接是外层信封（含 subCode/RESOURCE_LIMIT 等）
                String code = codeOf(chunk);
                if (code == null) code = codeOf(envelope);
                if (chunk == null && envelope == null) continue;
                if (code != null && !isOkCode(code)) throw envelopeError(code, payload);
                // 兜底：信封层级/字段名不确定，直接在原始帧里找排队标记
                if (looksQueued(null, payload)) throw envelopeError(null, payload);
            }
            return null;
        }
    }

    /* ==================== 上游错误信封 ==================== */

    private static String codeOf(JSONObject obj) {
        if (obj == null) return null;
        Object code = obj.get("code");
        return code == null ? null : String.valueOf(code);
    }

    private static boolean isOkCode(String code) {
        return "200".equals(code) || "0".equals(code) || "OK".equals(code);
    }

    /** 排队失败：code=10605，或帧里带 isQueued/serviceAvailable=false */
    private static boolean looksQueued(String code, String raw) {
        if ("10605".equals(code)) return true;
        return raw != null && (raw.contains("10605") || raw.contains("isQueued"));
    }

    /** 排队帧里的等待秒数（字段名上游不固定：retryAfterSec/retryAfterSeconds/waitTime） */
    private static Integer retrySeconds(String raw) {
        if (raw == null) return null;
        for (String key : new String[]{"retryAfterSec", "retryAfterSeconds", "waitTime"}) {
            Matcher m = Pattern.compile("\"" + key + "\"\\s*:\\s*(\\d+)").matcher(raw);
            if (m.find()) {
                try {
                    return Integer.parseInt(m.group(1));
                } catch (Exception ignore) {
                }
            }
        }
        return null;
    }

    /** 从错误帧里抽可读文案：message/msg/details，message 本身是 JSON 时再往里剥一层 */
    private static String readable(String raw) {
        String t = raw == null ? "" : raw.trim();
        if (!t.startsWith("{")) return t;
        try {
            JSONObject o = JSON.parseObject(t);
            if (o != null) {
                for (String key : new String[]{"message", "msg", "details"}) {
                    String v = o.getString(key);
                    if (v == null || v.trim().isEmpty()) continue;
                    v = v.trim();
                    if (v.startsWith("{")) {
                        JSONObject in = JSON.parseObject(v);
                        String m = in == null ? null : in.getString("message");
                        if (m == null || m.trim().isEmpty()) continue;
                        v = m.trim();
                    }
                    return v;
                }
            }
        } catch (Exception ignore) {
        }
        return t.length() > 300 ? t.substring(0, 300) : t;
    }

    /** 上游错误信封 -> 分类异常；10605 是排队失败（不是致命错误，换模型/换 provider 即可） */
    private static WbUpstreamException envelopeError(String code, String raw) {
        if (looksQueued(code, raw)) {
            Integer retryAfter = retrySeconds(raw);
            StringBuilder sb = new StringBuilder("模型服务繁忙（排队失败 10605），请稍后重试或切换模型");
            if (retryAfter != null && retryAfter > 0) sb.append("，建议 ").append(retryAfter).append(" 秒后重试");
            return new WbUpstreamException(WbUpstreamException.KIND_SOFT_RATE, 429, sb.toString());
        }
        String detail = readable(raw);
        if (detail.isEmpty()) detail = code == null ? "空错误帧" : code;
        String kind;
        if ("105".equals(code) || detail.contains("TOKEN_EXPIRE")) {
            kind = WbUpstreamException.KIND_SESSION_DEAD;
        } else if ("110".equals(code) || "112".equals(code)
                || detail.toLowerCase().contains("daily count")
                || detail.toLowerCase().contains("daily limit")) {
            kind = WbUpstreamException.KIND_HARD_CREDIT;
        } else {
            int status;
            try {
                status = Integer.parseInt(code == null ? "" : code);
            } catch (Exception e) {
                status = 0;
            }
            kind = status >= 500 ? WbUpstreamException.KIND_SERVER : WbUpstreamException.KIND_CLIENT;
        }
        return new WbUpstreamException(kind, 0, "模型服务 上游失败：" + detail);
    }
}
