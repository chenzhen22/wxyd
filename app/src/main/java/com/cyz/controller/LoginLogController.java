package com.cyz.controller;

import com.cyz.pojo.LoginLog;
import com.cyz.pojo.Result;
import com.cyz.service.LoginLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpSession;
import java.util.List;

/**
 * 登录日志查询：管理员看全部（可按用户名模糊过滤），普通用户只看自己的记录。
 */
@RestController
public class LoginLogController implements CommController {

    @Autowired
    private LoginLogService loginLogService;

    @ResponseBody
    @RequestMapping("loginLog/list")
    public Result list(@RequestParam(value = "username", required = false) String username,
                       HttpSession session) {
        Result result = Result.getInstance();
        Integer role = (Integer) session.getAttribute("role");
        Long uid = (Long) session.getAttribute("userId");
        boolean isAdmin = role != null && role == 0;
        // 普通用户强制只看自己；仅管理员允许 username 关键字过滤
        Long userId = isAdmin ? null : uid;
        String keyword = isAdmin ? username : null;
        List<LoginLog> logs = loginLogService.list(userId, keyword);
        result.setBody(logs);
        return result;
    }
}
