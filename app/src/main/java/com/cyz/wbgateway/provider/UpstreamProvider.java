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

    /**
     * 模型 id 前缀（无前缀返回空串）。带前缀的模型 id 只归该 provider；
     * 无前缀的 id 归无前缀的 provider——路由不依赖具体厂商名。
     */
    default String modelPrefix() {
        return "";
    }

    /** 该模型 id 是否由本 provider 提供 */
    default boolean owns(String modelId) {
        if (modelId == null) return true;
        String prefix = modelPrefix();
        if (!prefix.isEmpty()) return modelId.startsWith(prefix);
        return modelId.indexOf(':') < 0;
    }

    /** 当前可用模型目录（实时，失败时退回兜底） */
    ModelCatalog catalog();

    /** 默认模型 id（用于请求未带模型时） */
    String defaultModel();

    /** 该模型在本 provider 是否免费（默认看目录 freeIds） */
    default boolean isFree(String nativeId) {
        return nativeId != null && catalog().freeIds().contains(nativeId);
    }

    /** 可用账号数（0=不可用），用于跨 provider 兜底排序 */
    int usableAccountCount();

    /** 账号剩余积分（-1=未知），用于跨 provider 兜底排序 */
    long remainingCredits();

    /** 账号积分/套餐到期时间（毫秒；Long.MAX_VALUE=未知/不过期），路由优先消耗先到期的 */
    long creditsExpireAtMs();

    /** 账号池展示行（每行至少含 label/source/expiresAtMs/credits 等字段） */
    default java.util.List<java.util.Map<String, Object>> accountRows() {
        return new java.util.ArrayList<>();
    }

    /**
     * 按候选链发起流式对话；内部完成 provider 自己的凭据/账号轮换与链内重试。
     * body 为 OpenAI chat-completions 请求体（stream 由 provider 自行处理）。
     */
    /**
     * @param allowModelFallback true=静默映射模式（用户看不到实际模型），某个候选失败可继续尝试下一个候选
     */
    ProviderStream open(List<String> chain, String openAiBodyJson, boolean allowModelFallback) throws WbUpstreamException;

    /** 管理页诊断（登录态/账号池/额度等），可为空 map */
    Map<String, Object> status();

    /** 一次成功打开的上游流 */
    interface ProviderStream extends Closeable {

        /** 实际使用的上游模型（用于用量统计） */
        String usedModel();

        /** 服务本请求的账号/来源（用量记录展示用，如账号 label 或凭据来源；未知给空串） */
        default String account() {
            return "";
        }

        /** 逐块回调 OpenAI 风格 chunk（内部负责协议差异，如 SSE 解包） */
        void read(Consumer<JSONObject> onChunk) throws IOException;
    }
}
