package com.cyz.controller;

import com.cyz.pojo.Result;
import com.cyz.pojo.User;
import com.cyz.service.AuthService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpSession;
import java.util.Map;

@RestController
@Slf4j
public class AuthController implements CommController {

    @Autowired
    private AuthService authService;

    @ResponseBody
    @RequestMapping("register")
    public Result register(@RequestBody Result result) {
        Map<String, Object> map = (Map<String, Object>) result.getBody();
        String username = (String) map.get("username");
        String password = (String) map.get("password");
        String displayName = (String) map.get("displayName");
        result = Result.getInstance();
        try {
            User u = authService.register(username, password, displayName);
            result.setBody(u);
        } catch (IllegalArgumentException e) {
            result.setErrorCode("000003");
            result.setErrorMsg(e.getMessage());
        }
        return result;
    }

    @ResponseBody
    @RequestMapping("login")
    public Result login(@RequestBody Result result, HttpSession session) {
        Map<String, Object> map = (Map<String, Object>) result.getBody();
        String username = (String) map.get("username");
        String password = (String) map.get("password");
        result = Result.getInstance();
        try {
            User u = authService.login(username, password);
            if (u.getStatus() != null && u.getStatus() == 1) {
                result.setErrorCode("000003");
                result.setErrorMsg("账号待审批，请联系管理员");
                return result;
            }
            if (u.getStatus() != null && u.getStatus() == 2) {
                result.setErrorCode("000003");
                result.setErrorMsg("账号已被拒绝");
                return result;
            }
            session.setAttribute("userId", u.getId());
            session.setAttribute("username", u.getUsername());
            session.setAttribute("role", u.getRole());
            result.setBody(u);
        } catch (IllegalArgumentException e) {
            result.setErrorCode("000003");
            result.setErrorMsg(e.getMessage());
        }
        return result;
    }

    @ResponseBody
    @RequestMapping("logout")
    public Result logout(HttpSession session) {
        session.invalidate();
        return Result.getInstance();
    }
}
