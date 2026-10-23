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
    /** 服务本请求的账号/来源（WorkBuddy 账号 label / Qoder 凭据来源） */
    private String account;
    /** 客户端请求数据 JSON（存 /apps/shareFile/gateway/{id}.json，DB 不再保存） */
    private String requestData;
    /** 应答数据 JSON（存 /apps/shareFile/gateway/{id}.json，DB 不再保存） */
    private String responseData;
}
