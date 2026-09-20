package com.cyz.archive.model;

import lombok.Data;

/**
 * 归档抓取失败时的 JSON 响应体。
 * <p>
 * 成功时接口直接返回二进制附件，只有失败才走这个结构，前端按 Content-Type 区分。
 */
@Data
public class ArchiveError {

    private boolean success = false;

    /** 失败发生在哪一步 */
    private String stage;

    private String errorMsg;

    /** 上游 HTTP 状态码（若已拿到响应） */
    private Integer httpStatus;

    private String sourceUrl;

    private String elementId;

    /** 已拉取到的 HTML 字节数（若已拉到） */
    private Integer htmlBytes;

    public static ArchiveError of(String stage, String errorMsg) {
        ArchiveError e = new ArchiveError();
        e.setStage(stage);
        e.setErrorMsg(errorMsg);
        return e;
    }
}
