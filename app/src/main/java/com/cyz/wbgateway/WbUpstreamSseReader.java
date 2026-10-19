package com.cyz.wbgateway;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Response;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;

/** 上游 OpenAI 风格 SSE 解析（server.js forEachOpenAIChunk 的 Java 移植） */
@Slf4j
public final class WbUpstreamSseReader {

    private WbUpstreamSseReader() {
    }

    /** 逐块读取 SSE 流并回调解析后的 JSON chunk；结束后关闭响应 */
    public static void read(Response resp, Consumer<JSONObject> onChunk) throws IOException {
        try (Response r = resp) {
            if (r.body() == null) return;
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(r.body().byteStream(), StandardCharsets.UTF_8), 8192);
            StringBuilder data = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isEmpty()) {
                    emit(data, onChunk);
                    data.setLength(0);
                } else if (line.startsWith(":")) {
                    // 注释/心跳
                } else if (line.startsWith("data:")) {
                    String v = line.substring(5);
                    if (v.startsWith(" ")) v = v.substring(1);
                    data.append(v);
                }
            }
            emit(data, onChunk);
        }
    }

    private static void emit(StringBuilder data, Consumer<JSONObject> onChunk) {
        if (data.length() == 0) return;
        String payload = data.toString().trim();
        data.setLength(0);
        if (payload.isEmpty() || "[DONE]".equals(payload)) return;
        try {
            JSONObject chunk = JSON.parseObject(payload);
            if (chunk != null) onChunk.accept(chunk);
        } catch (Exception e) {
            // 忽略无法解析的心跳/分片
        }
    }
}
