package com.cyz.pojo;

import lombok.Data;

/**
 * 金融日报（day_info 表）：外部 SM2 加密 + SM4 加签推送，每种类型每天一条。
 */
@Data
public class DayInfo {

    private Long id;
    /** 日报类型，finance=金融日报 */
    private String type;
    /** 日报所属日期，YYYY-MM-DD */
    private String infoDate;
    private String title;
    /** md 原文 */
    private String content;
    /** 简洁版内容（订阅推送用；空则后端自动截取正文） */
    private String brief;
    /** 订阅提醒发送标记：0=待推送 1=已发送/无需发送 */
    private Integer notified;
    private String createTime;
    private String updateTime;
}
