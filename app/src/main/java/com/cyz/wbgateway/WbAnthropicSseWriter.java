package com.cyz.wbgateway;

import com.alibaba.fastjson2.JSONObject;
import lombok.extern.slf4j.Slf4j;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Anthropic 兼容 SSE 输出器（server.js AnthropicStreamWriter 的 Java 移植）：
 * 把上游 OpenAI chunk 流实时翻译成 message_start / content_block_* / message_delta / message_stop 事件。
 */
@Slf4j
public class WbAnthropicSseWriter {

    private final HttpServletResponse res;
    private final String reqModel;
    private final long estimateIn;

    private int nextIndex;
    private String openKind; // text | thinking | tool
    private int openIndex = -1;
    private ToolBlock openTool;
    private final Map<Integer, ToolBlock> toolBlocks = new HashMap<>();
    private long usageIn;
    private long usageOut;
    private long countedOut;
    private String finishReason;
    private boolean anyBlock;
    private boolean closed;

    private static class ToolBlock {
        int index;
        String id;
        String name;
        StringBuilder args = new StringBuilder();
    }

    public WbAnthropicSseWriter(HttpServletResponse res, String reqModel, long estimateIn) {
        this.res = res;
        this.reqModel = reqModel;
        this.estimateIn = estimateIn;
    }

    private void send(String event, Object data) throws IOException {
        if (closed) return;
        res.getWriter().write("event: " + event + "\ndata: " + WbProtocol.safeJson(data) + "\n\n");
        res.flushBuffer();
    }

    public void start() throws IOException {
        JSONObject message = new JSONObject();
        message.put("id", "msg_" + WbProtocol.rand(20));
        message.put("type", "message");
        message.put("role", "assistant");
        message.put("model", reqModel);
        message.put("content", new com.alibaba.fastjson2.JSONArray());
        message.put("stop_reason", null);
        message.put("stop_sequence", null);
        JSONObject usage = new JSONObject();
        usage.put("input_tokens", estimateIn);
        usage.put("output_tokens", 0);
        message.put("usage", usage);
        JSONObject ev = new JSONObject();
        ev.put("type", "message_start");
        ev.put("message", message);
        send("message_start", ev);
    }

    private void openText() throws IOException {
        int index = nextIndex++;
        anyBlock = true;
        openKind = "text";
        openIndex = index;
        JSONObject block = new JSONObject();
        block.put("type", "text");
        block.put("text", "");
        send("content_block_start", blockEvent(index, block));
    }

    private void openThinking() throws IOException {
        int index = nextIndex++;
        anyBlock = true;
        openKind = "thinking";
        openIndex = index;
        JSONObject block = new JSONObject();
        block.put("type", "thinking");
        block.put("thinking", "");
        send("content_block_start", blockEvent(index, block));
    }

    private JSONObject blockEvent(int index, JSONObject block) {
        JSONObject ev = new JSONObject();
        ev.put("type", "content_block_start");
        ev.put("index", index);
        ev.put("content_block", block);
        return ev;
    }

    private void delta(String deltaType, String key, String value) throws IOException {
        JSONObject d = new JSONObject();
        d.put("type", deltaType);
        d.put(key, value);
        JSONObject ev = new JSONObject();
        ev.put("type", "content_block_delta");
        ev.put("index", openIndex);
        ev.put("delta", d);
        send("content_block_delta", ev);
    }

    public void pushThinking(String text) throws IOException {
        if (text == null || text.isEmpty()) return;
        if (!"thinking".equals(openKind)) {
            closeBlock();
            openThinking();
        }
        countedOut += WbProtocol.estimateTextTokens(text);
        delta("thinking_delta", "thinking", text);
    }

    public void pushText(String text) throws IOException {
        if (text == null || text.isEmpty()) return;
        if (!"text".equals(openKind)) {
            closeBlock();
            openText();
        }
        countedOut += WbProtocol.estimateTextTokens(text);
        delta("text_delta", "text", text);
    }

    public void pushToolCall(JSONObject tc) throws IOException {
        Integer idxBox = tc.getInteger("index");
        int idx = idxBox == null ? 0 : idxBox;
        ToolBlock block = toolBlocks.get(idx);
        if (block == null) {
            closeBlock();
            block = new ToolBlock();
            block.index = nextIndex++;
            block.id = tc.getString("id") == null ? "toolu_" + WbProtocol.rand(20) : tc.getString("id");
            JSONObject fn = tc.getJSONObject("function");
            block.name = fn == null || fn.getString("name") == null ? "tool" : fn.getString("name");
            toolBlocks.put(idx, block);
            openKind = "tool";
            openTool = block;
            openIndex = block.index;
            anyBlock = true;
            JSONObject cb = new JSONObject();
            cb.put("type", "tool_use");
            cb.put("id", block.id);
            cb.put("name", block.name);
            cb.put("input", new JSONObject());
            send("content_block_start", blockEvent(block.index, cb));
        }
        JSONObject fn = tc.getJSONObject("function");
        String args = fn == null ? null : fn.getString("arguments");
        if (args != null && !args.isEmpty()) {
            block.args.append(args);
            countedOut += WbProtocol.estimateTextTokens(args);
            JSONObject d = new JSONObject();
            d.put("type", "input_json_delta");
            d.put("partial_json", args);
            JSONObject ev = new JSONObject();
            ev.put("type", "content_block_delta");
            ev.put("index", block.index);
            ev.put("delta", d);
            send("content_block_delta", ev);
        }
    }

    public void closeBlock() throws IOException {
        if (openKind == null) return;
        JSONObject ev = new JSONObject();
        ev.put("type", "content_block_stop");
        ev.put("index", openIndex);
        send("content_block_stop", ev);
        openKind = null;
        openTool = null;
        openIndex = -1;
    }

    /** 喂一个上游 OpenAI chunk */
    public void handleChunk(JSONObject chunk) throws IOException {
        if (chunk == null) return;
        JSONObject usage = chunk.getJSONObject("usage");
        if (usage != null) {
            Long p = usage.getLong("prompt_tokens");
            Long c = usage.getLong("completion_tokens");
            if (p != null && p > 0) usageIn = p;
            if (c != null && c > 0) usageOut = c;
        }
        com.alibaba.fastjson2.JSONArray choices = chunk.getJSONArray("choices");
        if (choices == null) return;
        for (int i = 0; i < choices.size(); i++) {
            JSONObject choice = choices.getJSONObject(i);
            if (choice == null) continue;
            JSONObject d = choice.getJSONObject("delta");
            if (d != null) {
                if (d.get("reasoning_content") instanceof String) pushThinking(d.getString("reasoning_content"));
                if (d.get("content") instanceof String) {
                    pushText(d.getString("content"));
                } else if (d.get("content") instanceof com.alibaba.fastjson2.JSONArray) {
                    for (Object o : d.getJSONArray("content")) {
                        if (o instanceof JSONObject && "text".equals(((JSONObject) o).getString("type"))) {
                            String t = ((JSONObject) o).getString("text");
                            if (t != null) pushText(t);
                        }
                    }
                }
                com.alibaba.fastjson2.JSONArray toolCalls = d.getJSONArray("tool_calls");
                if (toolCalls != null) {
                    for (int k = 0; k < toolCalls.size(); k++) {
                        JSONObject tc = toolCalls.getJSONObject(k);
                        if (tc != null) pushToolCall(tc);
                    }
                }
            }
            if (choice.getString("finish_reason") != null) finishReason = choice.getString("finish_reason");
        }
    }

    public void finish() throws IOException {
        if (closed) return;
        closeBlock();
        if (!anyBlock) {
            // 上游一个字都没给：补一个空文本块，避免客户端解析空 content
            openText();
            closeBlock();
        }
        boolean hasToolUse = !toolBlocks.isEmpty();
        JSONObject deltaObj = new JSONObject();
        deltaObj.put("stop_reason", WbProtocol.mapFinishReason(finishReason, hasToolUse));
        deltaObj.put("stop_sequence", null);
        JSONObject ev = new JSONObject();
        ev.put("type", "message_delta");
        ev.put("delta", deltaObj);
        JSONObject usage = new JSONObject();
        usage.put("input_tokens", usageIn > 0 ? usageIn : estimateIn);
        usage.put("output_tokens", usageOut > 0 ? usageOut : countedOut);
        ev.put("usage", usage);
        send("message_delta", ev);
        JSONObject stop = new JSONObject();
        stop.put("type", "message_stop");
        send("message_stop", stop);
        closed = true;
    }

    public void fail(String message) {
        if (closed) return;
        try {
            JSONObject err = new JSONObject();
            err.put("type", "api_error");
            err.put("message", message);
            JSONObject ev = new JSONObject();
            ev.put("type", "error");
            ev.put("error", err);
            send("error", ev); // 先发事件再置位（node 版先置位会被自己的 send 拦掉，属上游潜在 bug，此处按正确顺序写）
        } catch (IOException e) {
            log.debug("[WBGATEWAY] 写错误事件失败（客户端可能已断开）：{}", e.getMessage());
        } finally {
            closed = true;
        }
    }

    public boolean isClosed() {
        return closed;
    }

    /** 输入 token（上游 usage，缺省 0） */
    public long getInputTokens() {
        return usageIn;
    }

    public long outputTokens() {
        return usageOut > 0 ? usageOut : countedOut;
    }
}
