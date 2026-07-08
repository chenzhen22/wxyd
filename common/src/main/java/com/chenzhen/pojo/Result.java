package com.chenzhen.pojo;

import com.chenzhen.constant.ErrorEnum;
import lombok.Data;
import org.slf4j.MDC;

@Data
public class Result {

    private String errorCode;

    private String errorMsg;

    private String traceId;

    private Object body;


    public Result setErrorEnum(ErrorEnum errorEnum) {
        this.errorCode = errorEnum.getErrorCode();
        this.errorMsg = errorEnum.getErrorMsg();
        return this;
    }

    public static Result getInstance() {
        Result result = new Result();
        result.setErrorCode(ErrorEnum.ERROR000000.getErrorCode());
        result.setErrorMsg(ErrorEnum.ERROR000000.getErrorMsg());
        result.setTraceId(MDC.get("traceId"));
        result.setBody("");
        return result;
    }


}
