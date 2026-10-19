package com.cyz.pojo;

import lombok.Data;

/**
 * 模型网关令牌消耗记录（wb_gateway_usage）——每次对话请求一条。
 */
@Data
public class WbGatewayUsage {
    private Long id;
    private Long userId;
    private Long keyId;
    private String protocol;
    private String requestedModel;
    private String usedModel;
    private Integer stream;
    private Long inputTokens;
    private Long outputTokens;
    private Long durationMs;
    private String status;
    private String errorKind;
    private String createTime;
    /** 客户端请求数据 JSON（超长截断） */
    private String requestData;
    /** 应答数据 JSON（聚合后的内容/工具调用/usage，超长截断） */
    private String responseData;
}
