package com.cyz.archive.model;

import lombok.Data;

/**
 * 归档抓取结果。data 为解码后的 7z 字节，其余字段用于回填响应头与页面展示。
 */
@Data
public class ArchiveBundle {

    /** 最终下载文件名 */
    private String fileName;

    /** 解码后的文件内容 */
    private byte[] data;

    /** 数据来源地址 */
    private String sourceUrl;

    /** 使用的元素 id */
    private String elementId;

    /** 该元素实际标签名，如 span */
    private String elementTag;

    /** 拉取到的 HTML 字节数 */
    private int htmlBytes;

    /** 提取出的 Base64 字符数（已去掉空白） */
    private int base64Length;

    /** 解码后的字节数 */
    private int decodedSize;

    /** 是否通过 7z 魔数校验 */
    private boolean sevenZ;

    /** 7z 格式版本，如 0.4 */
    private String sevenZVersion;

    /** 7z 头部长度字段是否自洽（32 + nextHeaderOffset + nextHeaderSize == 文件长度） */
    private boolean sevenZConsistent;

    /** 内容 SHA-256 */
    private String sha256;

    /** 总耗时毫秒 */
    private long elapsedMs;
}
