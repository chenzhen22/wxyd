package com.cyz.service;

import com.cyz.pojo.User;

public interface AuthService {

    User register(String username, String password, String displayName);

    /** 登录校验，返回 user（密码字段置空）；失败返回 errorCode 的 Result 由 controller 包 */
    User login(String username, String password);
}
