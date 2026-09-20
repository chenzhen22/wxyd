package com.cyz.archive.model;

import lombok.Data;

/**
 * 归档抓取请求参数（POST /api/archive/fetch 的 JSON body）。
 */
@Data
public class ArchiveFetchRequest {

    /** 目标页面地址，必填，仅支持 http / https */
    private String url;

    /** HTML 中承载 Base64 的元素 id，留空则用默认值 xzz */
    private String elementId;

    /** 下载文件名（可空，默认「主机名_yyyyMMddHHmmss.7z」） */
    private String fileName;

    /** 超时毫秒，默认 60000，允许范围 1000 ~ 300000 */
    private Integer timeoutMs;

    /** 是否忽略 TLS 证书校验，默认 false（目标站证书有效时不需要） */
    private Boolean insecureTls;
}
