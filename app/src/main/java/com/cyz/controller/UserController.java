package com.cyz.controller;

import com.cyz.pojo.Result;
import com.cyz.pojo.User;
import com.cyz.service.UserService;
import com.cyz.util.CommUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpSession;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@Slf4j
public class UserController implements CommController {

    @Autowired
    private UserService userService;

    @ResponseBody
    @RequestMapping("user/list")
    public Result listUsers(HttpSession session) {
        Result result = Result.getInstance();
        Integer role = (Integer) session.getAttribute("role");
        Long uid = (Long) session.getAttribute("userId");
        List<User> all = userService.listUsers();
        if (role != null && role == 0) {
            result.setBody(all);
        } else {
            final Long self = uid;
            result.setBody(all.stream().filter(u -> self.equals(u.getId())).collect(Collectors.toList()));
        }
        return result;
    }

    @ResponseBody
    @RequestMapping("approveUser")
    public Result approveUser(@RequestBody Result result, HttpSession session) {
        Map<String, Object> map = (Map<String, Object>) result.getBody();
        Long id = ((Number) map.get("id")).longValue();
        Result r = Result.getInstance();
        if (!isAdmin(session)) {
            r.setErrorCode("000003");
            r.setErrorMsg("权限不足，仅超级管理员可审批");
            return r;
        }
        int idx = userService.approveUser(id);
        return CommUtils.handleDaoResult(idx);
    }

    @ResponseBody
    @RequestMapping("rejectUser")
    public Result rejectUser(@RequestBody Result result, HttpSession session) {
        Map<String, Object> map = (Map<String, Object>) result.getBody();
        Long id = ((Number) map.get("id")).longValue();
        Result r = Result.getInstance();
        if (!isAdmin(session)) {
            r.setErrorCode("000003");
            r.setErrorMsg("权限不足，仅超级管理员可操作");
            return r;
        }
        int idx = userService.rejectUser(id);
        return CommUtils.handleDaoResult(idx);
    }

    @ResponseBody
    @RequestMapping("deleteUser")
    public Result deleteUser(@RequestBody Result result, HttpSession session) {
        Map<String, Object> map = (Map<String, Object>) result.getBody();
        Long id = ((Number) map.get("id")).longValue();
        Result r = Result.getInstance();
        if (!isAdmin(session)) {
            r.setErrorCode("000003");
            r.setErrorMsg("权限不足，仅超级管理员可操作");
            return r;
        }
        int idx = userService.deleteUser(id);
        return CommUtils.handleDaoResult(idx);
    }

    private boolean isAdmin(HttpSession session) {
        Integer role = (Integer) session.getAttribute("role");
        return role != null && role == 0;
    }
}
