package com.cyz.wbgateway;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import lombok.extern.slf4j.Slf4j;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * OpenAI 协议 SSE 输出器（server.js makeOpenAiSsePipe 的直连版）：
 * 把上游 OpenAI chunk 流实时转写为 chat.completion.chunk 流。
 * 推理模型的思考内容在 OpenAI 协议下略去，只输出正文。
 */
@Slf4j
public class WbOpenAiSseWriter {

    private final HttpServletResponse res;
    private final String model;
    private final String id;
    private final long created;

    private boolean started;
    private final Map<Integer, Integer> toolIdx = new LinkedHashMap<>();
    private int nextTool;
    private String finishReason;
    private long usageIn;
    private long usageOut;

    public WbOpenAiSseWriter(HttpServletResponse res, String model) {
        this.res = res;
        this.model = model;
        this.id = "chatcmpl-" + WbProtocol.rand(20);
        this.created = System.currentTimeMillis() / 1000L;
    }

    private void writeData(Object chunk) throws IOException {
        res.getWriter().write("data: " + chunk + "\n\n");
        res.flushBuffer();
    }

    private void sendDelta(JSONObject delta, String finish, JSONObject usage) throws IOException {
        JSONObject chunk = new JSONObject();
        chunk.put("id", id);
        chunk.put("object", "chat.completion.chunk");
        chunk.put("created", created);
        chunk.put("model", model);
        JSONObject choice = new JSONObject();
        choice.put("index", 0);
        choice.put("delta", delta);
        choice.put("finish_reason", finish);
        JSONArray choices = new JSONArray();
        choices.add(choice);
        chunk.put("choices", choices);
        if (usage != null) chunk.put("usage", usage);
        writeData(chunk.toJSONString());
    }

    private void ensureStarted() throws IOException {
        if (started) return;
        started = true;
        JSONObject delta = new JSONObject();
        delta.put("role", "assistant");
        delta.put("content", "");
        sendDelta(delta, null, null);
    }

    /** 喂一个上游 OpenAI chunk */
    public void handleChunk(JSONObject chunk) throws IOException {
        if (chunk == null) return;
        ensureStarted();
        JSONObject usage = chunk.getJSONObject("usage");
        if (usage != null) {
            Long p = usage.getLong("prompt_tokens");
            Long c = usage.getLong("completion_tokens");
            if (p != null && p > 0) usageIn = p;
            if (c != null && c > 0) usageOut = c;
        }
        JSONArray choices = chunk.getJSONArray("choices");
        if (choices == null) return;
        for (int i = 0; i < choices.size(); i++) {
            JSONObject choice = choices.getJSONObject(i);
            if (choice == null) continue;
            JSONObject d = choice.getJSONObject("delta");
            if (d != null) {
                // reasoning_content 在 OpenAI 协议下略去
                if (d.get("content") instanceof String) {
                    String text = d.getString("content");
                    if (!text.isEmpty()) {
                        JSONObject delta = new JSONObject();
                        delta.put("content", text);
                        sendDelta(delta, null, null);
                    }
                } else if (d.get("content") instanceof JSONArray) {
                    for (Object o : d.getJSONArray("content")) {
                        if (o instanceof JSONObject && "text".equals(((JSONObject) o).getString("type"))) {
                            String t = ((JSONObject) o).getString("text");
                            if (t != null && !t.isEmpty()) {
                                JSONObject delta = new JSONObject();
                                delta.put("content", t);
                                sendDelta(delta, null, null);
                            }
                        }
                    }
                }
                JSONArray toolCalls = d.getJSONArray("tool_calls");
                if (toolCalls != null && !toolCalls.isEmpty()) {
                    JSONArray outCalls = new JSONArray();
                    for (int k = 0; k < toolCalls.size(); k++) {
                        JSONObject tc = toolCalls.getJSONObject(k);
                        if (tc == null) continue;
                        Integer upIdx = tc.getInteger("index");
                        int up = upIdx == null ? 0 : upIdx;
                        Integer mapped = toolIdx.get(up);
                        if (mapped == null) {
                            mapped = nextTool++;
                            toolIdx.put(up, mapped);
                        }
                        JSONObject outCall = new JSONObject();
                        outCall.put("index", mapped);
                        if (tc.getString("id") != null) outCall.put("id", tc.getString("id"));
                        if (tc.getString("type") != null) outCall.put("type", tc.getString("type"));
                        JSONObject fn = tc.getJSONObject("function");
                        if (fn != null) {
                            JSONObject outFn = new JSONObject();
                            if (fn.getString("name") != null) outFn.put("name", fn.getString("name"));
                            if (fn.getString("arguments") != null) outFn.put("arguments", fn.getString("arguments"));
                            outCall.put("function", outFn);
                        }
                        outCalls.add(outCall);
                    }
                    if (!outCalls.isEmpty()) {
                        JSONObject delta = new JSONObject();
                        delta.put("tool_calls", outCalls);
                        sendDelta(delta, null, null);
                    }
                }
            }
            if (choice.getString("finish_reason") != null) finishReason = choice.getString("finish_reason");
        }
    }

    public long getUsageIn() {
        return usageIn;
    }

    public long getUsageOut() {
        return usageOut;
    }

    public void finish() throws IOException {
        if (!started) ensureStarted();
        String finish;
        if (finishReason == null) {
            finish = !toolIdx.isEmpty() ? "tool_calls" : "stop";
        } else {
            switch (finishReason) {
                case "tool_calls":
                case "function_call": finish = "tool_calls"; break;
                case "length": finish = "length"; break;
                case "content_filter": finish = "content_filter"; break;
                default: finish = "stop";
            }
        }
        JSONObject usage = new JSONObject();
        usage.put("prompt_tokens", usageIn);
        usage.put("completion_tokens", usageOut);
        sendDelta(new JSONObject(), finish, usage);
        res.getWriter().write("data: [DONE]\n\n");
        res.flushBuffer();
    }
}
