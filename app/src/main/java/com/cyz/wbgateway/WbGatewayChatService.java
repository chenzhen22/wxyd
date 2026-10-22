package com.cyz.wbgateway;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.cyz.mapper.gateway.WbGatewayMapper;
import com.cyz.pojo.WbGatewayKey;
import com.cyz.pojo.WbGatewayUsage;
import com.cyz.service.WbGatewayKeyService;
import com.cyz.wbgateway.provider.ModelCatalog;
import com.cyz.wbgateway.provider.ProviderRegistry;
import com.cyz.wbgateway.provider.UpstreamProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * 网关执行管线（server.js handleMessages + handleWorkbuddy 的 Java 移植）：
 * 内部 Anthropic 形态请求体 -> OpenAI 上游请求（WorkBuddy）-> 双协议响应/流。
 * <p>
 * 模型解析（管理员模型开关）：开=用户自选/请求模型命中目录，失败兜底免费链；
 * 关=静默映射免费模型（按目录顺序重试），响应中的 model 字段回写为用户视角的名字，映射对用户不可见。
 */
@Service
@Slf4j
public class WbGatewayChatService {

    public static final String PROTO_ANTHROPIC = "anthropic";
    public static final String PROTO_OPENAI = "openai";

    /** Key 过期/未启用时的对外提示（不暴露上游任何信息） */
    public static final String KEY_INACTIVE_MESSAGE =
            "该 API Key 今日未启用或已过期，请登录「小泽助手 → 模型网关」点击「启用」后再试";

    /** 用量记录保留天数（wb_gateway_usage），默认 7 天 */
    @Value("${wxyd.wbgateway.usage-retention-days:7}")
    private int usageRetentionDays;

    /** 上游适配器注册表：按模型 id 路由到 WorkBuddy / Qoder 等 provider */
    @Autowired
    private ProviderRegistry providers;

    @Autowired
    private WbGatewayMapper wbGatewayMapper;

    @Autowired
    private WbGatewayKeyService keyService;


    /**
     * 执行一次对话并写响应。internal 为内部 Anthropic 形态请求体；
     * protocol 决定输出格式与错误形状（anthropic / openai）。
     */
    public void execute(HttpServletResponse res, JSONObject internal, WbGatewayKey key,
                        String protocol, String requestedModel, String clientRequestJson) throws IOException {
        boolean wantStream = Boolean.TRUE.equals(internal.getBoolean("stream"));
        JSONArray messages = internal.getJSONArray("messages");
        if (messages == null || messages.isEmpty()) {
            writeError(res, protocol, 400, "invalid_request_error", "messages 不能为空");
            return;
        }
        if (internal.getLong("max_tokens") == null) {
            writeError(res, protocol, 400, "invalid_request_error", "max_tokens 必填");
            return;
        }
        long estimateIn = WbProtocol.estimateRequestTokens(internal);
        long started = System.currentTimeMillis();
        log.info("[WBGATEWAY] messages userId={} keyId={} model={} stream={} msgs={} tools={} proto={}",
                key.getUserId(), key.getId(), requestedModel, wantStream, messages.size(),
                internal.getJSONArray("tools") == null ? 0 : internal.getJSONArray("tools").size(), protocol);

        // ---- 模型候选链 + 对外模型名 ----
        // 模型开关判定：Key 级覆盖优先（管理员设置），否则跟随全局开关
        Integer keyOverride = key.getModelOverride();
        boolean switchOn = keyOverride != null
                ? keyOverride == 1
                : !Integer.valueOf(0).equals(wbGatewayMapper.queryModelSwitch());
        String selected = key.getModel() == null ? "" : key.getModel().trim();
        String requested = requestedModel == null ? "" : requestedModel.trim();
        // 校验：仅当 Key 绑定了具体模型时，才要求请求模型与之匹配（大小写不敏感）；
        // Key 未绑定模型 = 「跟随系统」：不校验用户请求的模型，模型开关开启且请求模型在目录内时直接采用
        // 请求未带模型名时放行，按 Key 配置的模型执行
        if (!selected.isEmpty() && !requested.isEmpty() && !selected.equalsIgnoreCase(requested)) {
            log.warn("[WBGATEWAY] 模型与 Key 配置不匹配 userId={} keyId={} 请求模型={} Key模型={}",
                    key.getUserId(), key.getId(), requested, selected);
            recordUsage(key, protocol, requested, selected, wantStream, 0, 0, System.currentTimeMillis() - started, "error", "model_mismatch", clientRequestJson, null);
            writeError(res, protocol, 400, "invalid_request_error",
                    "请求的模型 " + requested + " 与 Key 配置的模型 " + selected
                            + " 不匹配，请在 AI 工具中将模型名改为 Key 配置的模型");
            return;
        }
        // ---- 合并目录（同名模型只展示一条）+ provider 自动路由 ----
        // 路由依据：该模型在哪些 provider 有货，取「账号可用且余额充足」评分最高者
        // （评分 10 分钟缓存，不会每次请求都刷余额）
        com.cyz.wbgateway.provider.CompositeCatalog catalog = providers.publicCatalog();
        String routeBy = !selected.isEmpty() ? selected : requested;
        ProviderRegistry.Route route = providers.resolve(routeBy);

        List<String> chain = new ArrayList<>();
        String publicModel;
        if (switchOn) {
            String canon = catalog.match(selected);
            if (canon != null) {
                chain.add(canon);
                publicModel = canon;
            } else {
                canon = catalog.match(requested);
                if (canon != null) {
                    chain.add(canon);
                    publicModel = canon;
                } else {
                    String dflt = catalog.match(route.provider.defaultModel());
                    if (dflt == null) dflt = requested.isEmpty() ? route.provider.defaultModel() : requested;
                    chain.add(dflt);
                    publicModel = requested.isEmpty() ? dflt : requested;
                }
            }
            // 同 provider 的免费模型作为候选链兜底
            for (String id : catalog.freeIdsOf(route.provider)) if (!chain.contains(id)) chain.add(id);
        } else {
            // 开关关闭：静默映射到该 provider 的免费模型，用户不可知；对外仍显示用户视角名字
            chain.addAll(catalog.freeIdsOf(route.provider));
            if (chain.isEmpty()) {
                String dflt = catalog.match(route.provider.defaultModel());
                chain.add(dflt == null ? route.provider.defaultModel() : dflt);
            }
            publicModel = !selected.isEmpty() ? selected
                    : (!requested.isEmpty() ? requested : route.provider.defaultModel());
            log.info("[WBGATEWAY] 模型开关关闭：静默映射 userId={} keyId={} 视角模型={} 实际链={}",
                    key.getUserId(), key.getId(), publicModel, chain);
        }

        // 展示 id -> 选中 provider 的原生模型 id（Qoder 为 qoder:xxx，WorkBuddy 为原名）
        UpstreamProvider provider = providers.resolve(chain.get(0)).provider;
        List<String> nativeChain = new ArrayList<>();
        for (String display : chain) {
            String nativeId = catalog.nativeIdFor(display, provider.id());
            if (nativeId != null && !nativeChain.contains(nativeId)) nativeChain.add(nativeId);
        }
        if (nativeChain.isEmpty()) {
            nativeChain.add(provider.defaultModel());
        }
        log.info("[WBGATEWAY] 路由 userId={} keyId={} 展示模型={} provider={} 原生链={}",
                key.getUserId(), key.getId(), chain.get(0), provider.id(), nativeChain);

        // ---- 组装上游请求（Anthropic -> OpenAI + WAF 清洗 + prepareBody）----
        JSONObject openaiReq = WbProtocol.toOpenAIRequest(internal, nativeChain.get(0));
        openaiReq.put("stream", true); // 上游只接受流式，客户端要非流式时在网关聚合
        WbProtocol.sanitizeOpenAiMessages(openaiReq);

        // ---- 发起上游请求（provider 内部负责凭据/账号轮换与链内重试）----
        UpstreamProvider.ProviderStream stream;
        try {
            stream = provider.open(nativeChain, openaiReq.toJSONString());
        } catch (WbUpstreamException failure) {
            String detail = failure.getMessage() == null ? "" : failure.getMessage();
            log.error("[WBGATEWAY] 上游错误 provider={} kind={} http={} detail={}",
                    provider.id(), failure.getKind(), failure.getHttpStatus(), detail);
            recordUsage(key, protocol, requested, nativeChain.get(0), wantStream, 0, 0,
                    System.currentTimeMillis() - started, "error", failure.getKind(), clientRequestJson,
                    JSON.toJSONString(java.util.Collections.singletonMap("error", detail)));
            writeError(res, protocol, failure.outboundStatus(), failure.outboundErrorType(),
                    failure.getKind() + ": " + detail);
            return;
        }
        String usedModel = stream.usedModel();

        keyService.touch(key.getId());

        // ---- 输出 ----
        if (wantStream) {
            res.setStatus(200);
            res.setContentType("text/event-stream; charset=utf-8");
            res.setHeader("Cache-Control", "no-cache, no-transform");
            res.setHeader("X-Accel-Buffering", "no");
            res.setCharacterEncoding("UTF-8");
            WbProtocol.Aggregated agg = new WbProtocol.Aggregated();
            agg.usedModel = publicModel;
            if (PROTO_ANTHROPIC.equals(protocol)) {
                WbAnthropicSseWriter writer = new WbAnthropicSseWriter(res, publicModel, estimateIn);
                boolean streamOk = false;
                try {
                    writer.start();
                    stream.read(chunk -> {
                        WbProtocol.feedChunk(agg, chunk);
                        try { writer.handleChunk(chunk); } catch (IOException ie) { throw new RuntimeException(ie); }
                    });
                    writer.finish();
                    streamOk = true;
                } catch (IOException e) {
                    if (!writer.isClosed()) writer.fail("流失败: " + e.getMessage());
                }
                recordUsage(key, protocol, requested, publicModel, true,
                        agg.usageIn > 0 ? agg.usageIn : estimateIn,
                        agg.usageOut > 0 ? agg.usageOut : writer.outputTokens(), System.currentTimeMillis() - started,
                        streamOk ? "ok" : "error", streamOk ? null : "stream_interrupted",
                        clientRequestJson, WbProtocol.toTraceJson(agg).toJSONString());
            } else {
                WbOpenAiSseWriter writer = new WbOpenAiSseWriter(res, publicModel);
                boolean streamOk = false;
                try {
                    stream.read(chunk -> {
                        WbProtocol.feedChunk(agg, chunk);
                        try { writer.handleChunk(chunk); } catch (IOException ie) { throw new RuntimeException(ie); }
                    });
                    writer.finish();
                    streamOk = true;
                } catch (IOException e) {
                    log.warn("[WBGATEWAY] OpenAI 流式输出中断：{}", e.getMessage());
                }
                recordUsage(key, protocol, requested, publicModel, true,
                        agg.usageIn > 0 ? agg.usageIn : estimateIn,
                        agg.usageOut, System.currentTimeMillis() - started,
                        streamOk ? "ok" : "error", streamOk ? null : "stream_interrupted",
                        clientRequestJson, WbProtocol.toTraceJson(agg).toJSONString());
            }
            log.info("[WBGATEWAY] 流式完成 ms={}", System.currentTimeMillis() - started);
            return;
        }

        WbProtocol.Aggregated agg = new WbProtocol.Aggregated();
        agg.usedModel = usedModel;
        try {
            stream.read(chunk -> WbProtocol.feedChunk(agg, chunk));
        } catch (IOException e) {
            recordUsage(key, protocol, requested, usedModel, false, 0, 0,
                    System.currentTimeMillis() - started, "error", "parse_failed", clientRequestJson, null);
            writeError(res, protocol, 502, "api_error", "解析响应失败: " + e.getMessage());
            return;
        }
        JSONObject out = PROTO_ANTHROPIC.equals(protocol)
                ? WbProtocol.toAnthropicResponse(agg, publicModel, estimateIn)
                : WbProtocol.toOpenAiResponse(agg, publicModel,
                        "chatcmpl-" + WbProtocol.rand(20), System.currentTimeMillis() / 1000L);
        res.setStatus(200);
        res.setContentType("application/json; charset=utf-8");
        res.setCharacterEncoding("UTF-8");
        res.getWriter().write(out.toJSONString());
        res.flushBuffer();
        recordUsage(key, protocol, requested, usedModel, wantStream,
                agg.usageIn > 0 ? agg.usageIn : estimateIn, agg.usageOut, System.currentTimeMillis() - started, "ok", null,
                clientRequestJson, WbProtocol.toTraceJson(agg).toJSONString());
        log.info("[WBGATEWAY] 完成 ms={} in={} out={}", System.currentTimeMillis() - started,
                agg.usageIn > 0 ? agg.usageIn : estimateIn, agg.usageOut);
    }

    private static final int TRACE_MAX_CHARS = 400_000;

    private static String capTrace(String s) {
        if (s == null) return null;
        return s.length() > TRACE_MAX_CHARS ? s.substring(0, TRACE_MAX_CHARS) + "\n...(已截断)" : s;
    }

    /** 落一条令牌消耗记录（失败不影响主流程） */
    private void recordUsage(WbGatewayKey key, String protocol, String requestedModel, String usedModel,
                             boolean stream, long in, long out, long durationMs, String status, String errorKind,
                             String requestData, String responseData) {
        try {
            WbGatewayUsage u = new WbGatewayUsage();
            u.setUserId(key.getUserId());
            u.setKeyId(key.getId());
            u.setProtocol(protocol);
            u.setRequestedModel(requestedModel == null ? "" : requestedModel);
            u.setUsedModel(usedModel == null ? "" : usedModel);
            u.setStream(stream ? 1 : 0);
            u.setInputTokens(Math.max(0, in));
            u.setOutputTokens(Math.max(0, out));
            u.setDurationMs(Math.max(0, durationMs));
            u.setStatus(status);
            u.setErrorKind(errorKind);
            u.setRequestData(capTrace(requestData));
            u.setResponseData(capTrace(responseData));
            wbGatewayMapper.insertUsage(u);
        } catch (Exception e) {
            log.warn("[WBGATEWAY] 用量记录失败：{}", e.getMessage());
        }
    }

    /**
     * 用量记录过期清理：每日 03:30 删除超过保留期（默认 7 天，wxyd.wbgateway.usage-retention-days）的记录；
     * 启动时也清一次。
     */
    @Scheduled(cron = "0 30 3 * * ?")
    public void cleanupUsage() {
        int n = wbGatewayMapper.deleteUsageExpired(Math.max(1, usageRetentionDays));
        if (n > 0) {
            log.info("[WBGATEWAY] 已清理 {} 条过期用量记录（保留 {} 天）", n, usageRetentionDays);
        }
    }

    @javax.annotation.PostConstruct
    public void cleanupUsageOnStartup() {
        try {
            cleanupUsage();
        } catch (Exception e) {
            log.warn("[WBGATEWAY] 启动清理用量记录失败：{}", e.getMessage());
        }
    }

    private String defaultOrFirst(ModelCatalog catalog, UpstreamProvider provider) {
        String dflt = provider.defaultModel();
        String canon = catalog.match(dflt);
        if (canon != null) return canon;
        List<String> ids = catalog.ids();
        return ids.isEmpty() ? dflt : ids.get(0);
    }

    /** 按协议输出错误形状 */
    /** 对外应答脱敏：任何上游厂商信息都不出现在返回给客户端的内容里 */
    public static String sanitize(String message) {
        if (message == null) return "";
        return message.replaceAll("(?i)workbuddy", "模型服务");
    }

    public static void writeError(HttpServletResponse res, String protocol, int status,
                                  String type, String message) throws IOException {
        message = sanitize(message);
        JSONObject out = new JSONObject();
        if (PROTO_OPENAI.equals(protocol)) {
            JSONObject err = new JSONObject();
            err.put("message", message);
            err.put("type", type);
            err.put("code", null);
            out.put("error", err);
        } else {
            JSONObject err = new JSONObject();
            err.put("type", type);
            err.put("message", message);
            out.put("type", "error");
            out.put("error", err);
        }
        res.setStatus(status);
        res.setContentType("application/json; charset=utf-8");
        res.setCharacterEncoding("UTF-8");
        res.getWriter().write(out.toJSONString());
        res.flushBuffer();
    }
}
