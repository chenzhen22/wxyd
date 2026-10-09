package com.cyz.radar.model;

import lombok.Data;

import java.util.List;

/**
 * 羊毛雷达订阅（radar_subscribe 表）。
 * <p>
 * 每条 = 用户 × 关键词 × 通道；RadarPushJob 每小时按 last_post_id 水位
 * 增量扫描 scan_post，命中即按通道推送。
 */
@Data
public class RadarSubscribe {

    private Long id;
    private Long userId;
    /** 命中关键词（标题或摘要 LIKE） */
    private String keyword;
    /** dingtalk | email */
    private String channel;
    /** channel=dingtalk 时必填，指向用户自己的 ding_robot.id */
    private Long robotId;
    /** channel=email 时必填 */
    private String email;
    /** 已推送到的 scan_post.id 水位 */
    private Long lastPostId;
    private Integer enabled;
    private String createTime;

    /** 推送时填充：本批命中的帖子 */
    private transient List<HitPost> hits;

    /** 命中的帖子（scan_post 投影，不含原站信息 —— 遵守福利区纯展示原则） */
    @Data
    public static class HitPost {
        private Long id;
        private String title;
        private String summary;
        private String publishedAt;
    }
}
