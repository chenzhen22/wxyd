package com.cyz.controller;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.cyz.pojo.WbGatewayKey;
import com.cyz.service.WbGatewayKeyService;
import com.cyz.wbgateway.WbGatewayChatService;
import com.cyz.wbgateway.WbProtocol;
import com.cyz.wbgateway.WbWorkBuddyClient;
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
 * Anthropic 协议网关入口（/api/anthropic/**，context-path /api）。
 * <p>
 * 不走 AuthInterceptor（不在保护清单），自带 x-api-key / Bearer 鉴权查 wb_gateway_key；
 * 照 DayInfoController 模式不实现 CommController，响应为 Anthropic 协议形状（非 Result 包裹）。
 */
@RestController
@Slf4j
public class WbAnthropicController {

    @Autowired
    private WbGatewayChatService chatService;

    @Autowired
    private WbGatewayKeyService keyService;

    @Autowired
    private WbWorkBuddyClient client;

    /** POST /anthropic/v1/messages（流式 + 非流式） */
    @PostMapping("anthropic/v1/messages")
    public void messages(HttpServletRequest req, HttpServletResponse res) throws IOException {
        WbGatewayKey key = requireAuth(req, res);
        if (key == null) return;
        JSONObject body = readBody(req);
        if (body == null) {
            WbGatewayChatService.writeError(res, WbGatewayChatService.PROTO_ANTHROPIC, 400,
                    "invalid_request_error", "请求体不是合法 JSON");
            return;
        }
        chatService.execute(res, body, key, WbGatewayChatService.PROTO_ANTHROPIC, body.getString("model"), body.toJSONString());
    }

    /** POST /anthropic/v1/messages/count_tokens（本地粗估，不经上游） */
    @PostMapping("anthropic/v1/messages/count_tokens")
    public void countTokens(HttpServletRequest req, HttpServletResponse res) throws IOException {
        WbGatewayKey key = requireAuth(req, res);
        if (key == null) return;
        JSONObject body = readBody(req);
        JSONObject out = new JSONObject();
        out.put("input_tokens", body == null ? 0 : WbProtocol.estimateRequestTokens(body));
        res.setStatus(200);
        res.setContentType("application/json; charset=utf-8");
        res.getWriter().write(out.toJSONString());
    }

    /** GET /anthropic/v1/models */
    @GetMapping("anthropic/v1/models")
    public void models(HttpServletRequest req, HttpServletResponse res) throws IOException {
        WbGatewayKey key = requireAuth(req, res);
        if (key == null) return;
        List<String> ids = catalogIds();
        JSONArray data = new JSONArray();
        for (String id : ids) {
            JSONObject m = new JSONObject();
            m.put("type", "model");
            m.put("id", id);
            m.put("display_name", id);
            m.put("created_at", "2026-01-01T00:00:00Z");
            data.add(m);
        }
        JSONObject out = new JSONObject();
        out.put("data", data);
        out.put("has_more", false);
        if (!ids.isEmpty()) {
            out.put("first_id", ids.get(0));
            out.put("last_id", ids.get(ids.size() - 1));
        }
        res.setStatus(200);
        res.setContentType("application/json; charset=utf-8");
        res.getWriter().write(out.toJSONString());
    }

    private List<String> catalogIds() {
        try {
            return client.models().ids();
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    private WbGatewayKey requireAuth(HttpServletRequest req, HttpServletResponse res) throws IOException {
        String raw = clientKeyOf(req);
        if (raw.isEmpty()) {
            WbGatewayChatService.writeError(res, WbGatewayChatService.PROTO_ANTHROPIC, 401,
                    "authentication_error", "缺少 API Key：请设置 x-api-key 或 Authorization: Bearer <key>");
            return null;
        }
        WbGatewayKey key = keyService.lookup(raw);
        if (key == null) {
            log.warn("[WBGATEWAY] 拒绝无效 API Key suffix={}", raw.substring(Math.max(0, raw.length() - 4)));
            WbGatewayChatService.writeError(res, WbGatewayChatService.PROTO_ANTHROPIC, 401,
                    "authentication_error", "API Key 无效");
            return null;
        }
        if (!keyService.isActive(key)) {
            WbGatewayChatService.writeError(res, WbGatewayChatService.PROTO_ANTHROPIC, 401,
                    "authentication_error", WbGatewayChatService.KEY_INACTIVE_MESSAGE);
            return null;
        }
        return key;
    }

    static String clientKeyOf(HttpServletRequest req) {
        String xKey = req.getHeader("x-api-key");
        if (xKey != null && !xKey.isEmpty()) return xKey.trim();
        String auth = req.getHeader("Authorization");
        if (auth != null && auth.matches("(?i)^Bearer\\s+.+$")) {
            return auth.replaceFirst("(?i)^Bearer\\s+", "").trim();
        }
        return "";
    }

    static JSONObject readBody(HttpServletRequest req) throws IOException {
        StringBuilder sb = new StringBuilder();
        char[] buf = new char[8192];
        int n;
        try (java.io.Reader r = req.getReader()) {
            while ((n = r.read(buf)) > 0) sb.append(buf, 0, n);
        }
        String raw = sb.toString();
        if (raw.isEmpty()) return new JSONObject();
        try {
            return JSON.parseObject(raw);
        } catch (Exception e) {
            return null;
        }
    }
}
