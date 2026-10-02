package com.cyz.controller;

import com.cyz.pojo.Result;
import com.cyz.pojo.User;
import com.cyz.service.AuthService;
import com.cyz.service.EmailRegisterService;
import com.cyz.service.PasswordResetService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpSession;
import java.util.Collections;
import java.util.Map;

@RestController
@Slf4j
public class AuthController implements CommController {

    @Autowired
    private AuthService authService;

    @Autowired
    private EmailRegisterService emailRegisterService;

    @Autowired
    private PasswordResetService passwordResetService;

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
            if (u.getStatus() != null && u.getStatus() == 3) {
                result.setErrorCode("000003");
                result.setErrorMsg("账号已被暂停，请联系管理员");
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

    // ===== 邮箱注册（滑块验证码 → 邮箱验证码 → 设置账号） =====

    /** 发送邮箱验证码：需先通过滑块验证携带 ticket；同一邮箱 60 秒内不可重发 */
    @ResponseBody
    @RequestMapping("register/email/send")
    public Result sendEmailCode(@RequestBody Result req) {
        Map<String, Object> map = (Map<String, Object>) req.getBody();
        String email = (String) map.get("email");
        String ticket = (String) map.get("ticket");
        Result result = Result.getInstance();
        try {
            emailRegisterService.sendCode(email, ticket);
        } catch (IllegalArgumentException | IllegalStateException e) {
            result.setErrorCode("000003");
            result.setErrorMsg(e.getMessage());
        }
        return result;
    }

    /** 校验 6 位邮箱验证码，通过返回一次性 registerToken */
    @ResponseBody
    @RequestMapping("register/email/verify")
    public Result verifyEmailCode(@RequestBody Result req) {
        Map<String, Object> map = (Map<String, Object>) req.getBody();
        String email = (String) map.get("email");
        String code = (String) map.get("code");
        Result result = Result.getInstance();
        try {
            String token = emailRegisterService.verifyCode(email, code);
            result.setBody(Collections.singletonMap("registerToken", token));
        } catch (IllegalArgumentException e) {
            result.setErrorCode("000003");
            result.setErrorMsg(e.getMessage());
        }
        return result;
    }

    /** 完成注册：设置密码与用户名（选填，自动生成），成功即登录 */
    @ResponseBody
    @RequestMapping("register/email/complete")
    public Result completeRegister(@RequestBody Result req, HttpSession session) {
        Map<String, Object> map = (Map<String, Object>) req.getBody();
        String token = (String) map.get("registerToken");
        String password = (String) map.get("password");
        String username = (String) map.get("username");
        Result result = Result.getInstance();
        try {
            User u = emailRegisterService.complete(token, password, username);
            session.setAttribute("userId", u.getId());
            session.setAttribute("username", u.getUsername());
            session.setAttribute("role", u.getRole());
            result.setBody(u);
        } catch (IllegalArgumentException | IllegalStateException e) {
            result.setErrorCode("000003");
            result.setErrorMsg(e.getMessage());
        }
        return result;
    }

    // ===== 忘记密码（滑块验证码 → 邮箱验证码 → 重置密码） =====

    /** 发送密码重置验证码：需先通过滑块验证携带 ticket；邮箱须已绑定账号，60 秒内不可重发 */
    @ResponseBody
    @RequestMapping("password/reset/send")
    public Result sendResetCode(@RequestBody Result req) {
        Map<String, Object> map = (Map<String, Object>) req.getBody();
        String email = (String) map.get("email");
        String ticket = (String) map.get("ticket");
        Result result = Result.getInstance();
        try {
            passwordResetService.sendCode(email, ticket);
        } catch (IllegalArgumentException | IllegalStateException e) {
            result.setErrorCode("000003");
            result.setErrorMsg(e.getMessage());
        }
        return result;
    }

    /** 校验 6 位邮箱验证码，通过返回一次性 resetToken */
    @ResponseBody
    @RequestMapping("password/reset/verify")
    public Result verifyResetCode(@RequestBody Result req) {
        Map<String, Object> map = (Map<String, Object>) req.getBody();
        String email = (String) map.get("email");
        String code = (String) map.get("code");
        Result result = Result.getInstance();
        try {
            String token = passwordResetService.verifyCode(email, code);
            result.setBody(Collections.singletonMap("resetToken", token));
        } catch (IllegalArgumentException e) {
            result.setErrorCode("000003");
            result.setErrorMsg(e.getMessage());
        }
        return result;
    }

    /** 重置密码：resetToken 有效则更新密码 */
    @ResponseBody
    @RequestMapping("password/reset/complete")
    public Result completeReset(@RequestBody Result req) {
        Map<String, Object> map = (Map<String, Object>) req.getBody();
        String token = (String) map.get("resetToken");
        String password = (String) map.get("password");
        Result result = Result.getInstance();
        try {
            passwordResetService.reset(token, password);
        } catch (IllegalArgumentException | IllegalStateException e) {
            result.setErrorCode("000003");
            result.setErrorMsg(e.getMessage());
        }
        return result;
    }
}
