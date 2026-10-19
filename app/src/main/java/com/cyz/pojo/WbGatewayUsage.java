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
}
