package com.cyz.bohe.model;

import lombok.Data;

import java.util.Date;

/**
 * 薄荷账好友关系（有向记录：userId 发起 → friendId 接收）。
 * status: 0=待通过 1=已通过 2=已拒绝/删除。任一方向 status=1 即互为好友。
 * hideMyBills: 发起方对好友隐藏自己的薄荷账（好友粒度，与 users.bohe_hidden 全局开关并存）。
 */
@Data
public class BoheFriend {
    private Long id;
    private Long userId;
    private Long friendId;
    private Integer status;
    private Integer hideMyBills;
    private Date createTime;
    private Date updateTime;
}
