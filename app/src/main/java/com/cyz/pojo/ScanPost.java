package com.cyz.pojo;

import lombok.Data;

/**
 * 福利专区帖子（只读映射 dataScan 的 scan_post 表，7 天滚动保留）。
 * 列表接口只返回 title/summary/firstImage/publishedAt，
 * 详情接口返回 title/publishedAt/contentMd/images。
 */
@Data
public class ScanPost {
    private Long id;
    private String title;
    /** 列表摘要（SQL 截取 content_md 前 120 字） */
    private String summary;
    /** 列表首图（服务端由 images JSON 解析出的本地代理地址，可能为 null） */
    private String firstImage;
    private String publishedAt;
    private String contentMd;
    /** images JSON 原文（[{url,alt,width,height,local_path,size}]），service 解析后返回 */
    private String images;
}
