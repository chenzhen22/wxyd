package com.cyz.dubbo.model;

import lombok.Data;

/**
 * Dubbo 调用结果。
 */
@Data
public class CallResult {

    private boolean success;
    private long costMs;
    private Object response;
    private String error;      // 失败时的异常信息
    private String configId;
    private String configName;
}
