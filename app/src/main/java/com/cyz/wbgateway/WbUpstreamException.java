package com.cyz.wbgateway;

import lombok.Getter;

/**
 * WorkBuddy 上游失败（照插件 classifyUpstreamError 分类，kind 与网关 kindStatus 对应）。
 */
@Getter
public class WbUpstreamException extends RuntimeException {

    public static final String KIND_HARD_CREDIT = "hard_credit";
    public static final String KIND_SOFT_RATE = "soft_rate";
    public static final String KIND_SESSION_DEAD = "session_dead";
    public static final String KIND_NOT_FOUND = "not_found";
    public static final String KIND_SERVER = "server";
    public static final String KIND_CLIENT = "client";
    /** 上游风控限制账号（403「账号存在异常行为，已被限制使用」）：换号能救，换模型/刷新令牌都没用 */
    public static final String KIND_ACCOUNT_BLOCKED = "account_blocked";

    private final String kind;
    private final int httpStatus;

    public WbUpstreamException(String kind, int httpStatus, String detail) {
        super(detail);
        this.kind = kind;
        this.httpStatus = httpStatus;
    }

    /** kind -> 对外 HTTP 状态（与插件 shim 的 KIND_STATUS 一致） */
    public int outboundStatus() {
        switch (kind) {
            case KIND_HARD_CREDIT: return 402;
            case KIND_SOFT_RATE: return 429;
            case KIND_SESSION_DEAD: return 401;
            case KIND_NOT_FOUND: return 502;
            case KIND_SERVER: return 502;
            case KIND_ACCOUNT_BLOCKED: return 403;
            default: return 400;
        }
    }

    public String outboundErrorType() {
        switch (kind) {
            case KIND_SESSION_DEAD: return "authentication_error";
            case KIND_SOFT_RATE: return "rate_limit_error";
            case KIND_CLIENT: return "invalid_request_error";
            case KIND_ACCOUNT_BLOCKED: return "permission_error";
            default: return "api_error";
        }
    }
}
