package com.cyz.bohe.model;

import lombok.Data;

import java.util.Date;
import java.time.LocalDate;

/**
 * 薄荷账账单。amount 为整数分；cloudId 为小程序云数据库 _id（导入去重键，手工删除账单时置空）。
 */
@Data
public class BoheBill {
    private Long id;
    private Long userId;
    private String cloudId;
    private Long amount;
    private String category;
    private String note;
    private LocalDate billDate;
    private Date createTime;
}
