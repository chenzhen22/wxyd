package com.cyz.controller;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.cyz.pojo.WbGatewayKey;
import com.cyz.service.WbGatewayKeyService;
import com.cyz.wbgateway.WbGatewayChatService;
import com.cyz.wbgateway.WbProtocol;
import com.cyz.wbgateway.provider.ProviderRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * OpenAI 协议网关入口（/api/openai/**，context-path /api）。
 * 入站 OpenAI chat-completions 先转成内部 Anthropic 形态，再复用整套上游管线。
 */
@RestController
@Slf4j
public class WbOpenAiController {

    @Autowired
    private WbGatewayChatService chatService;

    @Autowired
    private WbGatewayKeyService keyService;

    @Autowired
    private ProviderRegistry providers;

    /**
     * POST /openai/v1/chat/completions（也接受 /openai/chat/completions）。
     * 兼容把 base_url 误填成 .../api/anthropic 的 OpenAI 客户端：
     * 这类客户端会拼出 /anthropic/chat/completions（或 /anthropic/v1/chat/completions），
     * 按 OpenAI 协议同样处理，避免 404。
     */
    @PostMapping({"openai/v1/chat/completions", "openai/chat/completions",
            "anthropic/v1/chat/completions", "anthropic/chat/completions"})
    public void chatCompletions(HttpServletRequest req, HttpServletResponse res) throws IOException {
        WbGatewayKey key = requireAuth(req, res);
        if (key == null) return;
        JSONObject raw = WbAnthropicController.readBody(req);
        if (raw == null) {
            WbGatewayChatService.writeError(res, WbGatewayChatService.PROTO_OPENAI, 400,
                    "invalid_request_error", "请求体不是合法 JSON");
            return;
        }
        if (raw.getJSONArray("messages") == null || raw.getJSONArray("messages").isEmpty()) {
            WbGatewayChatService.writeError(res, WbGatewayChatService.PROTO_OPENAI, 400,
                    "invalid_request_error", "messages 不能为空");
            return;
        }
        String requestedModel = raw.getString("model");
        JSONObject internal = WbProtocol.openAiInboundToAnthropic(raw);
        chatService.execute(res, internal, key, WbGatewayChatService.PROTO_OPENAI, requestedModel, raw.toJSONString());
    }

    /** GET /openai/v1/models（也接受 /openai/models） */
    @GetMapping({"openai/v1/models", "openai/models"})
    public void models(HttpServletRequest req, HttpServletResponse res) throws IOException {
        WbGatewayKey key = requireAuth(req, res);
        if (key == null) return;
        List<String> ids;
        try {
            ids = providers.allModelIds(); // 合并去重（同名模型只出现一次）
        } catch (Exception e) {
            ids = new ArrayList<>();
        }
        long created = System.currentTimeMillis() / 1000L;
        JSONArray data = new JSONArray();
        for (String id : ids) {
            JSONObject m = new JSONObject();
            m.put("id", id);
            m.put("object", "model");
            m.put("created", created);
            m.put("owned_by", "model-gateway");
            data.add(m);
        }
        JSONObject out = new JSONObject();
        out.put("object", "list");
        out.put("data", data);
        res.setStatus(200);
        res.setContentType("application/json; charset=utf-8");
        res.getWriter().write(out.toJSONString());
    }

    private WbGatewayKey requireAuth(HttpServletRequest req, HttpServletResponse res) throws IOException {
        String raw = WbAnthropicController.clientKeyOf(req);
        if (raw.isEmpty()) {
            WbGatewayChatService.writeError(res, WbGatewayChatService.PROTO_OPENAI, 401,
                    "authentication_error", "缺少 API Key：请设置 Authorization: Bearer <key> 或 x-api-key");
            return null;
        }
        WbGatewayKey key = keyService.lookup(raw);
        if (key == null) {
            log.warn("[WBGATEWAY] 拒绝无效 API Key suffix={}", raw.substring(Math.max(0, raw.length() - 4)));
            WbGatewayChatService.writeError(res, WbGatewayChatService.PROTO_OPENAI, 401,
                    "authentication_error", "API Key 无效");
            return null;
        }
        if (!keyService.isActive(key)) {
            WbGatewayChatService.writeError(res, WbGatewayChatService.PROTO_OPENAI, 401,
                    "authentication_error", WbGatewayChatService.KEY_INACTIVE_MESSAGE);
            return null;
        }
        return key;
    }
}
