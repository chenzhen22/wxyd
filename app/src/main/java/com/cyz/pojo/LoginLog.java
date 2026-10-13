package com.cyz.pojo;

import lombok.Data;

/**
 * 登录日志（login_log 表）：记录密码登录成功/失败、OAuth 登录、登出等事件。
 */
@Data
public class LoginLog {

    public static final String EVENT_LOGIN_SUCCESS = "login_success";
    public static final String EVENT_LOGIN_FAIL = "login_fail";
    public static final String EVENT_LOGOUT = "logout";

    public static final String CHANNEL_PASSWORD = "password";
    public static final String CHANNEL_GITHUB = "github";
    public static final String CHANNEL_GITEE = "gitee";
    public static final String CHANNEL_EMAIL = "email";

    private Long id;
    /** 登录用户 id；失败尝试且用户不存在时为 null */
    private Long userId;
    /** 尝试登录的用户名/邮箱，OAuth 为登录名 */
    private String username;
    /** login_success | login_fail | logout */
    private String event;
    /** password | github | gitee | email */
    private String channel;
    private String ip;
    /** 失败原因等补充信息 */
    private String detail;
    /** yyyy-MM-dd HH:mm:ss */
    private String createTime;
}
