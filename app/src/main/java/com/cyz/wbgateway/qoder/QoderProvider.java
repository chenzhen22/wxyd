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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

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
        return client.credentialUsable() ? 1 : 0;
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
        QoderCatalog.ModelInfo info = cat.byId(owned.get(0));
        if (info == null) {
            throw new WbUpstreamException(WbUpstreamException.KIND_NOT_FOUND, 404, "模型不在 Qoder 目录中：" + owned.get(0));
        }
        Response resp = client.chat(info.key, body, info.alwaysThinking);
        return new ProviderStream() {
            @Override
            public String usedModel() {
                return info.id;
            }

            @Override
            public void read(Consumer<JSONObject> onChunk) throws IOException {
                readEnvelopes(resp, onChunk);
            }

            @Override
            public void close() {
                try {
                    resp.close();
                } catch (Exception ignore) {
                }
            }
        };
    }

    /** 双层信封 SSE：data 帧的 body 字段本身是 OpenAI chunk 的 JSON 字符串 */
    private static void readEnvelopes(Response resp, Consumer<JSONObject> onChunk) throws IOException {
        try (Response r = resp) {
            if (r.body() == null) return;
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(r.body().byteStream(), StandardCharsets.UTF_8), 8192);
            String line;
            while ((line = reader.readLine()) != null) {
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
                if (chunk == null) continue;
                if (chunk.get("choices") != null || chunk.get("usage") != null) {
                    onChunk.accept(chunk);
                    continue;
                }
                Object code = chunk.get("code");
                if (code != null && !"200".equals(String.valueOf(code)) && !"OK".equals(String.valueOf(code))) {
                    String msg = chunk.getString("message");
                    if (msg == null || msg.isEmpty()) msg = chunk.getString("details");
                    throw new IOException("Qoder 上游失败：" + (msg == null ? String.valueOf(code) : msg));
                }
            }
        }
    }
}
