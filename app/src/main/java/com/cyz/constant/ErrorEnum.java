package com.chenzhen.constant;

import lombok.Getter;
import lombok.Setter;

/**
 * 错误码枚举
 */
public enum ErrorEnum {

    ERROR000000("000000", "successful"),
    ERROR000001("000001", "操作员不存在"),
    ERROR000002("000002", "不能输入特殊字符"),
    ERROR000003("000002", "输入的证书不存在"),
    ERROR888888("888888", "您不在白名单，请联系管理员"),
    ERROR999999("999999", "未知错误");

    @Getter
    @Setter
    private String errorCode;

    @Getter
    @Setter
    private String errorMsg;

    ErrorEnum(String code, String msg) {
        this.errorCode = code;
        this.errorMsg = msg;
    }

}
