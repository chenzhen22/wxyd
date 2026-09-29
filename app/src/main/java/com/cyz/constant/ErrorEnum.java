package com.cyz.constant;

import lombok.Getter;
import lombok.Setter;

/**
 * 错误码枚举
 */
public enum ErrorEnum {

    ERROR000000("000000", "successful"),
    ERROR000001("000001", "操作员不存在"),
    ERROR000002("000002", "不能输入特殊字符"),
    ERROR000003("000002", "输入的证书不存在"),
    ERROR888888("888888", "您不在白名单，请联系管理员"),
    ERROR999999("999999", "未知错误"),
    ERROR000010("000010", "群数量已达上限（5个）"),
    ERROR000011("000011", "群名非法"),
    ERROR000012("000012", "已是群成员"),
    ERROR000013("000013", "已提交过申请，等待管理员审批"),
    ERROR000014("000014", "无权操作"),
    ERROR000015("000015", "参数非法"),
    ERROR000016("000016", "消息内容非法"),
    ERROR000017("000017", "图片上传失败"),
    ERROR000018("000018", "笔记不存在");

    @Getter
    @Setter
    private String errorCode;

    @Getter
    @Setter
    private String errorMsg;

    ErrorEnum(String code, String msg) {
        this.errorCode = code;
        this.errorMsg = msg;
    }

}
