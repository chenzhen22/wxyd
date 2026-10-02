package com.cyz.service;

import com.cyz.pojo.User;

public interface AuthService {

    User register(String username, String password, String displayName);

    /** 登录校验，返回 user（密码字段置空）；失败返回 errorCode 的 Result 由 controller 包 */
    User login(String username, String password);

    /**
     * GitHub 授权登录：按 githubLogin 查找，不存在则自动建号（role=1, status=0, 免审批）。
     * 返回的用户已置空密码字段。
     */
    User findOrCreateByGithub(String githubLogin, String name);

    /**
     * Gitee 授权登录：按 giteeLogin 查找，不存在则自动建号（role=1, status=0, 免审批）。
     * 返回的用户已置空密码字段。
     */
    User findOrCreateByGitee(String giteeLogin, String name);
}
