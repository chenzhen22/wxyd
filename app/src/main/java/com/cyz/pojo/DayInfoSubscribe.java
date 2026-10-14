package com.cyz.pojo;

import lombok.Data;

/**
 * 金融日报订阅提醒（day_info_subscribe 表）：一人多条，可同时订阅多个钉钉机器人与多个邮箱，
 * 新日报入库时对启用的每条订阅各推一次。
 */
@Data
public class DayInfoSubscribe {

    private Long id;
    private Long userId;
    /** dingtalk | email */
    private String channel;
    /** channel=dingtalk 时必填，指向用户自己的 ding_robot.id */
    private Long robotId;
    /** channel=email 时必填 */
    private String email;
    private Integer enabled;
    private String createTime;
    private String updateTime;
}
