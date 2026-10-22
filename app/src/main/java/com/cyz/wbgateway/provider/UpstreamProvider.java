package com.cyz.wbgateway.provider;

import com.alibaba.fastjson2.JSONObject;
import com.cyz.wbgateway.WbUpstreamException;

import java.io.Closeable;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * 上游模型服务适配器：一个 provider 对应一种插件/协议（WorkBuddy、Qoder…）。
 * <p>
 * 网关只依赖本接口：目录、是否归属某个模型、发起对话（内部负责凭据/账号轮换与候选链重试）、
 * 以及给管理页用的诊断信息。新增插件只需实现本接口并注册为 Spring Bean。
 */
public interface UpstreamProvider {

    /** 唯一标识（如 workbuddy / qoder-cn） */
    String id();

    String displayName();

    /** 当前可用模型目录（实时，失败时退回兜底） */
    ModelCatalog catalog();

    /** 默认模型 id（用于请求未带模型时） */
    String defaultModel();

    /** 该模型 id 是否由本 provider 提供（路由依据） */
    boolean owns(String modelId);

    /**
     * 按候选链发起流式对话；内部完成 provider 自己的凭据/账号轮换与链内重试。
     * body 为 OpenAI chat-completions 请求体（stream 由 provider 自行处理）。
     */
    ProviderStream open(List<String> chain, String openAiBodyJson) throws WbUpstreamException;

    /** 管理页诊断（登录态/账号池/额度等），可为空 map */
    Map<String, Object> status();

    /** 一次成功打开的上游流 */
    interface ProviderStream extends Closeable {

        /** 实际使用的上游模型（用于用量统计） */
        String usedModel();

        /** 逐块回调 OpenAI 风格 chunk（内部负责协议差异，如 SSE 解包） */
        void read(Consumer<JSONObject> onChunk) throws IOException;
    }
}
