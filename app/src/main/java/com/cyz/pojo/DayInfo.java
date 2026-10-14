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
    private String createTime;
    private String updateTime;
}
