package com.cyz.archive.model;

/**
 * 归档下载过程中的业务异常。
 * <p>
 * stage 用于告诉前端「卡在哪一步」，例如「发起请求」「提取 Base64」「Base64 解码」，
 * 便于页面上分步展示排查。httpStatus 决定接口返回的状态码。
 */
public class ArchiveException extends RuntimeException {

    private final String stage;

    private final int httpStatus;

    public ArchiveException(String stage, String message) {
        this(stage, message, 502);
    }

    public ArchiveException(String stage, String message, int httpStatus) {
        super(message);
        this.stage = stage;
        this.httpStatus = httpStatus;
    }

    public String getStage() {
        return stage;
    }

    public int getHttpStatus() {
        return httpStatus;
    }
}
